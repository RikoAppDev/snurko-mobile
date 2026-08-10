package app.snurko.challenge.data

import app.snurko.challenge.domain.NotebookDocumentGateway
import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import app.snurko.core.ios.RootViewControllerProvider
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.darwin.NSObject

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
class DocumentPickerNotebookDocumentGateway(
    private val rootViewControllers: RootViewControllerProvider,
) : NotebookDocumentGateway {

    private var pendingPick: PickedDocumentDelegate? = null

    override suspend fun writeDocument(
        suggestedName: String,
        content: String,
    ): EmptyResult<DataError.Backup> {
        val source = writeTemporaryDocument(suggestedName, content)
            ?: return Result.Error(DataError.Backup.DOCUMENT_UNAVAILABLE)

        val exported = pick {
            UIDocumentPickerViewController(forExportingURLs = listOf(source), asCopy = true)
        }
        NSFileManager.defaultManager.removeItemAtURL(source, null)

        return when (exported) {
            is Result.Error -> exported
            is Result.Success -> Result.Success(Unit)
        }
    }

    override suspend fun readDocument(): Result<String, DataError.Backup> {
        val url = when (val picked = pick { UIDocumentPickerViewController(forOpeningContentTypes = READABLE_TYPES) }) {
            is Result.Error -> return picked
            is Result.Success -> picked.data
        }
        val isScoped = url.startAccessingSecurityScopedResource()
        val content = NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null)
        if (isScoped) url.stopAccessingSecurityScopedResource()

        return content?.let { Result.Success(it) }
            ?: Result.Error(DataError.Backup.DOCUMENT_UNAVAILABLE)
    }

    private suspend fun pick(
        create: () -> UIDocumentPickerViewController,
    ): Result<NSURL, DataError.Backup> {
        val presenter = rootViewControllers.current()
            ?: return Result.Error(DataError.Backup.DOCUMENT_UNAVAILABLE)

        val awaited = CompletableDeferred<NSURL?>()
        pendingPick?.cancel()
        val delegate = PickedDocumentDelegate(awaited)
        pendingPick = delegate

        val controller = create()
        controller.delegate = delegate
        withContext(Dispatchers.Main) {
            presenter.presentViewController(controller, animated = true, completion = null)
        }

        val url = awaited.await()
        if (pendingPick === delegate) pendingPick = null

        return url?.let { Result.Success(it) } ?: Result.Error(DataError.Backup.CANCELLED)
    }

    private fun writeTemporaryDocument(name: String, content: String): NSURL? {
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + name)
        val isWritten = NSString.create(string = content)
            .writeToURL(url, true, NSUTF8StringEncoding, null)
        return url.takeIf { isWritten }
    }

    private companion object {
        val READABLE_TYPES = listOf(UTTypeJSON, UTTypePlainText)
    }
}

private class PickedDocumentDelegate(
    private val awaited: CompletableDeferred<NSURL?>,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    fun cancel() {
        awaited.complete(null)
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        awaited.complete(didPickDocumentsAtURLs.filterIsInstance<NSURL>().firstOrNull())
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        awaited.complete(null)
    }
}
