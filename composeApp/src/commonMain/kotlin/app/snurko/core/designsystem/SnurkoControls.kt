package app.snurko.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Keeps a tapped margin note or quiet row from flashing a sharp rectangle. */
val QuietTapShape = RoundedCornerShape(10.dp)

private val BACK_TARGET = 48.dp
private val CHEVRON_SIZE = 22.dp
private val CHEVRON_STROKE = 2.dp

@Composable
fun BackChevronButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = contentDescription

    Box(
        modifier = modifier
            .size(BACK_TARGET)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        BackChevron()
    }
}

@Composable
private fun BackChevron() {
    Canvas(
        modifier = Modifier
            .size(CHEVRON_SIZE)
            .clearAndSetSemantics { },
    ) {
        val side = size.minDimension
        val chevron = Path().apply {
            moveTo(side * 0.64f, side * 0.16f)
            lineTo(side * 0.32f, side * 0.50f)
            lineTo(side * 0.64f, side * 0.84f)
        }
        drawPath(
            path = chevron,
            color = SnurkoColors.text,
            style = Stroke(
                width = CHEVRON_STROKE.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
