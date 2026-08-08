package app.snurko.core.presentation

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.datetime.LocalTime
import java.util.Calendar

@Composable
actual fun PlatformTimePicker(
    startFrom: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val context = LocalContext.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    DisposableEffect(startFrom) {
        val dialog = TimePickerDialog(
            context,
            android.R.style.Theme_DeviceDefault_Light_Dialog_Alert,
            { _, hour, minute -> currentOnConfirm(hour, minute) },
            startFrom.hour,
            startFrom.minute,
            DateFormat.is24HourFormat(context),
        )
        dialog.setOnCancelListener { currentOnDismiss() }
        dialog.show()
        onDispose { dialog.dismiss() }
    }
}

@Composable
actual fun formattedTime(time: LocalTime): String {
    val context = LocalContext.current
    val format = remember(context) { DateFormat.getTimeFormat(context) }
    val calendar = remember(time) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
        }
    }
    return format.format(calendar.time)
}
