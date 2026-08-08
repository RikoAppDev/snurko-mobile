package app.snurko.core.presentation

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalTime

@Composable
expect fun PlatformTimePicker(
    startFrom: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
)

@Composable
expect fun formattedTime(time: LocalTime): String
