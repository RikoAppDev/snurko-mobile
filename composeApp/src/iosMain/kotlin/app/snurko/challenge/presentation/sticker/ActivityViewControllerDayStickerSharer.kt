package app.snurko.challenge.presentation.sticker

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import app.snurko.core.ios.RootViewControllerProvider
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

class ActivityViewControllerDayStickerSharer(
    private val rootViewControllers: RootViewControllerProvider,
) : DayStickerSharer {

    override suspend fun share(image: ImageBitmap, fileName: String): EmptyResult<DataError.Share> {
        val presenter = rootViewControllers.current()
            ?: return Result.Error(DataError.Share.UNAVAILABLE)
        val sticker = image.toPng()?.let { writeSticker(it, fileName) }
            ?: return Result.Error(DataError.Share.UNAVAILABLE)

        withContext(Dispatchers.Main) {
            val sheet = UIActivityViewController(
                activityItems = listOf(sticker),
                applicationActivities = null,
            )
            sheet.popoverPresentationController?.sourceView = presenter.view
            presenter.presentViewController(sheet, animated = true, completion = null)
        }
        return Result.Success(Unit)
    }

    private fun writeSticker(png: NSData, fileName: String): NSURL? {
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
        return url.takeIf { png.writeToURL(it, true) }
    }
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private fun ImageBitmap.toPng(): NSData? {
    val encoded = Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)
        ?: return null
    val bytes = encoded.bytes
    if (bytes.isEmpty()) return null
    return bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }
}
