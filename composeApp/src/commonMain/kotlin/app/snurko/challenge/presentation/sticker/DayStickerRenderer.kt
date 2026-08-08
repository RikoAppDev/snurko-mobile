package app.snurko.challenge.presentation.sticker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CompletableDeferred

const val STICKER_WIDTH_PX = 1080
const val STICKER_HEIGHT_PX = 1350
const val STICKER_DENSITY = 3f

/**
 * Draws the card at its own fixed density into a graphics layer instead of onto the screen,
 * so the shared image is identical on every device and never flashes in front of the user.
 */
@Composable
fun DayStickerRenderer(sticker: DayStickerState, onRendered: (ImageBitmap) -> Unit) {
    val layer = rememberGraphicsLayer()
    val recorded = remember(sticker) { CompletableDeferred<Unit>() }

    Layout(
        content = {
            CompositionLocalProvider(
                LocalDensity provides Density(density = STICKER_DENSITY, fontScale = 1f),
            ) {
                DayStickerCard(state = sticker)
            }
        },
        modifier = Modifier.drawWithContent {
            layer.record(size = IntSize(STICKER_WIDTH_PX, STICKER_HEIGHT_PX)) {
                this@drawWithContent.drawContent()
            }
            recorded.complete(Unit)
        },
    ) { measurables, _ ->
        val card = measurables.first().measure(
            Constraints.fixed(width = STICKER_WIDTH_PX, height = STICKER_HEIGHT_PX),
        )
        layout(width = 1, height = 1) { card.place(0, 0) }
    }

    LaunchedEffect(sticker) {
        recorded.await()
        onRendered(layer.toImageBitmap())
    }
}
