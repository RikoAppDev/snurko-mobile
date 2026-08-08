package app.snurko.challenge.presentation.sticker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult
import app.snurko.core.domain.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SystemDayStickerSharer(private val context: Context) : DayStickerSharer {

    override suspend fun share(image: ImageBitmap, fileName: String): EmptyResult<DataError.Share> =
        runCatching {
            val uri = withContext(Dispatchers.IO) { writeSticker(image, fileName) }
            withContext(Dispatchers.Main) { context.startActivity(chooserFor(uri)) }
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(DataError.Share.UNAVAILABLE) },
        )

    private fun writeSticker(image: ImageBitmap, fileName: String): Uri {
        val directory = File(context.cacheDir, STICKER_DIRECTORY)
        directory.mkdirs()
        directory.listFiles()?.forEach { it.delete() }

        val file = File(directory, fileName)
        file.outputStream().use { output ->
            image.toShareableBitmap().compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)
        }
        return FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", file)
    }

    private fun chooserFor(uri: Uri): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = STICKER_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun ImageBitmap.toShareableBitmap(): Bitmap {
        val bitmap = asAndroidBitmap()
        val isHardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            bitmap.config == Bitmap.Config.HARDWARE
        return if (isHardware) bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap else bitmap
    }

    private companion object {
        const val STICKER_DIRECTORY = "stickers"
        const val AUTHORITY_SUFFIX = ".stickers"
        const val STICKER_MIME_TYPE = "image/png"
        const val PNG_QUALITY = 100
    }
}
