package app.snurko.challenge.data

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import app.snurko.challenge.domain.NotebookDocumentGateway
import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafNotebookDocumentGateway(
    private val contentResolver: ContentResolver,
) : NotebookDocumentGateway {

    data class Launchers(
        val createDocument: ActivityResultLauncher<String>,
        val openDocument: ActivityResultLauncher<Array<String>>,
    )

    private var launchers: Launchers? = null
    private var pickedDocument: CompletableDeferred<Uri?>? = null

    fun attach(launchers: Launchers) {
        this.launchers = launchers
    }

    fun detach() {
        launchers = null
    }

    fun onDocumentPicked(uri: Uri?) {
        pickedDocument?.complete(uri)
        pickedDocument = null
    }

    override suspend fun writeDocument(
        suggestedName: String,
        content: String,
    ): EmptyResult<DataError.Backup> {
        val uri = when (val picked = pick { it.createDocument.launch(suggestedName) }) {
            is Result.Error -> return picked
            is Result.Success -> picked.data
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                val stream = contentResolver.openOutputStream(uri, TRUNCATING_WRITE)
                    ?: return@runCatching null
                stream.use { it.write(content.encodeToByteArray()) }
                Unit
            }.fold(
                onSuccess = { written ->
                    if (written == null) {
                        Result.Error(DataError.Backup.DOCUMENT_UNAVAILABLE)
                    } else {
                        Result.Success(Unit)
                    }
                },
                onFailure = { Result.Error(DataError.Backup.DOCUMENT_UNAVAILABLE) },
            )
        }
    }

    override suspend fun readDocument(): Result<String, DataError.Backup> {
        val uri = when (val picked = pick { it.openDocument.launch(READABLE_MIME_TYPES) }) {
            is Result.Error -> return picked
            is Result.Success -> picked.data
        }
        return withContext(Dispatchers.IO) {
            runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }
                .fold(
                    onSuccess = { content ->
                        content?.let { Result.Success(it) }
                            ?: Result.Error(DataError.Backup.DOCUMENT_UNAVAILABLE)
                    },
                    onFailure = { Result.Error(DataError.Backup.DOCUMENT_UNAVAILABLE) },
                )
        }
    }

    private suspend fun pick(launch: (Launchers) -> Unit): Result<Uri, DataError.Backup> {
        val current = launchers ?: return Result.Error(DataError.Backup.DOCUMENT_UNAVAILABLE)
        val awaited = CompletableDeferred<Uri?>()
        pickedDocument?.complete(null)
        pickedDocument = awaited
        launch(current)
        val uri = awaited.await() ?: return Result.Error(DataError.Backup.CANCELLED)
        return Result.Success(uri)
    }

    private companion object {
        const val TRUNCATING_WRITE = "wt"
        val READABLE_MIME_TYPES = arrayOf("application/json", "text/plain", "*/*")
    }
}
