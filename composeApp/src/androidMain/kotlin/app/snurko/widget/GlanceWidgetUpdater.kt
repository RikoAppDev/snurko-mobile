package app.snurko.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

class GlanceWidgetUpdater(
    private val context: Context,
) : WidgetUpdater {

    override suspend fun updateAll() {
        SnurkoWidget().updateAll(context)
    }
}
