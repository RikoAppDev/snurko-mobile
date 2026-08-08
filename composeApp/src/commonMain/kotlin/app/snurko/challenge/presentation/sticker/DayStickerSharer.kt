package app.snurko.challenge.presentation.sticker

import androidx.compose.ui.graphics.ImageBitmap
import app.snurko.core.domain.DataError
import app.snurko.core.domain.EmptyResult

fun interface DayStickerSharer {
    suspend fun share(image: ImageBitmap, fileName: String): EmptyResult<DataError.Share>
}
