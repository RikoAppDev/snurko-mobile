package app.snurko.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.snurko.challenge.presentation.home.MAX_MARK_ROTATION

private const val STROKE_RATIO = 0.09f
private const val MIN_STROKE_PX = 2f

/**
 * Glance has no draw scope, so the pencil check reaches a widget cell as a bitmap. One bitmap per
 * rotation step is enough: the whole grid draws from the same handful, and RemoteViews sends each
 * distinct bitmap over only once.
 */
fun pencilCheckBitmaps(sizePx: Int, color: Color): Map<Float, Bitmap> {
    val steps = MAX_MARK_ROTATION.toInt()
    return (-steps..steps).associate { degrees ->
        degrees.toFloat() to pencilCheckBitmap(sizePx, degrees.toFloat(), color)
    }
}

private fun pencilCheckBitmap(sizePx: Int, rotation: Float, color: Color): Bitmap {
    val side = sizePx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = maxOf(side * STROKE_RATIO, MIN_STROKE_PX)
    }
    val mark = Path().apply {
        moveTo(side * 0.20f, side * 0.55f)
        lineTo(side * 0.42f, side * 0.78f)
        lineTo(side * 0.82f, side * 0.24f)
    }

    canvas.rotate(rotation, side / 2f, side / 2f)
    canvas.drawPath(mark, paint)
    return bitmap
}
