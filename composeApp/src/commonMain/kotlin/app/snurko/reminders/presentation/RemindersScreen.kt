package app.snurko.reminders.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snurko.core.designsystem.SnurkoColors
import app.snurko.core.designsystem.SnurkoTheme
import app.snurko.core.presentation.ObserveAsEvents
import app.snurko.core.presentation.OnResume
import app.snurko.core.presentation.PlatformTimePicker
import app.snurko.core.presentation.formattedTime
import app.snurko.resources.Res
import app.snurko.resources.reminders_back
import app.snurko.resources.reminders_back_mark
import app.snurko.resources.reminders_blocked_body
import app.snurko.resources.reminders_blocked_settings
import app.snurko.resources.reminders_blocked_title
import app.snurko.resources.reminders_change
import app.snurko.resources.reminders_intro
import app.snurko.resources.reminders_off
import app.snurko.resources.reminders_set
import app.snurko.resources.reminders_switch_off
import app.snurko.resources.reminders_switch_off_mark
import app.snurko.resources.reminders_title
import app.snurko.resources.reminders_update_failed
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val PAGE_PADDING = 20.dp
private val NOTE_CORNER = RoundedCornerShape(6.dp)

@Composable
fun RemindersRoot(
    onNavigateBack: () -> Unit,
    viewModel: RemindersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val updateFailedMessage = stringResource(Res.string.reminders_update_failed)

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            RemindersEvent.UpdateFailed -> scope.launch {
                snackbarHostState.showSnackbar(updateFailedMessage)
            }
        }
    }

    OnResume { viewModel.onAction(RemindersAction.ScreenResumed) }

    Scaffold(
        containerColor = SnurkoColors.paper,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        RemindersScreen(
            state = state,
            onAction = viewModel::onAction,
            onNavigateBack = onNavigateBack,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun RemindersScreen(
    state: RemindersState,
    onAction: (RemindersAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SnurkoColors.paper)
            .verticalScroll(rememberScrollState()),
    ) {
        RemindersHeader(onNavigateBack = onNavigateBack)

        Column(modifier = Modifier.padding(horizontal = PAGE_PADDING)) {
            Text(
                text = stringResource(Res.string.reminders_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = SnurkoColors.textSecondary,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            if (state.isNotificationPermissionDenied) {
                BlockedNotice(onOpenSettings = { onAction(RemindersAction.SettingsClicked) })
                Spacer(Modifier.height(20.dp))
            }

            state.rows.forEach { row ->
                HorizontalDivider(color = SnurkoColors.line)
                ReminderRow(
                    row = row,
                    onClick = { onAction(RemindersAction.RowClicked(row.habitId)) },
                    onSwitchOff = { onAction(RemindersAction.ReminderSwitchedOff(row.habitId)) },
                )
            }
            if (state.rows.isNotEmpty()) {
                HorizontalDivider(color = SnurkoColors.line)
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    state.timePicker?.let { picker ->
        PlatformTimePicker(
            startFrom = picker.startFrom,
            onDismiss = { onAction(RemindersAction.TimePickerDismissed) },
            onConfirm = { hour, minute -> onAction(RemindersAction.TimePicked(hour, minute)) },
        )
    }
}

@Composable
private fun RemindersHeader(onNavigateBack: () -> Unit) {
    val backDescription = stringResource(Res.string.reminders_back)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_PADDING - 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onNavigateBack)
                .semantics { contentDescription = backDescription },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.reminders_back_mark),
                style = MaterialTheme.typography.titleLarge,
                color = SnurkoColors.text,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        Text(
            text = stringResource(Res.string.reminders_title),
            style = MaterialTheme.typography.displaySmall,
            color = SnurkoColors.text,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun BlockedNotice(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NOTE_CORNER)
            .background(SnurkoColors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(Res.string.reminders_blocked_title),
            style = MaterialTheme.typography.titleSmall,
            color = SnurkoColors.text,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.reminders_blocked_body),
            style = MaterialTheme.typography.bodyMedium,
            color = SnurkoColors.textSecondary,
        )
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onOpenSettings),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(Res.string.reminders_blocked_settings),
                style = MaterialTheme.typography.bodyLarge,
                color = SnurkoColors.accentInk,
            )
        }
    }
}

@Composable
private fun ReminderRow(
    row: ReminderRowUi,
    onClick: () -> Unit,
    onSwitchOff: () -> Unit,
) {
    val rowDescription = if (row.time == null) {
        stringResource(Res.string.reminders_set, row.name)
    } else {
        stringResource(Res.string.reminders_change, row.name)
    }
    val switchOffDescription = stringResource(Res.string.reminders_switch_off, row.name)

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = rowDescription }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyLarge,
                color = SnurkoColors.text,
            )
            Text(
                text = row.time?.let { formattedTime(it) } ?: stringResource(Res.string.reminders_off),
                style = MaterialTheme.typography.bodyLarge,
                color = if (row.time == null) SnurkoColors.textSecondary else SnurkoColors.accentInk,
            )
        }
        if (row.time != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(role = Role.Button, onClick = onSwitchOff)
                    .semantics { contentDescription = switchOffDescription },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.reminders_switch_off_mark),
                    style = MaterialTheme.typography.titleLarge,
                    color = SnurkoColors.textSecondary,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Preview
@Composable
private fun RemindersScreenPreview() {
    SnurkoTheme {
        RemindersScreen(state = previewState, onAction = {}, onNavigateBack = {})
    }
}

@Preview
@Composable
private fun RemindersBlockedPreview() {
    SnurkoTheme {
        RemindersScreen(
            state = previewState.copy(isNotificationPermissionDenied = true),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

private val previewState = RemindersState(
    rows = listOf(
        ReminderRowUi(habitId = 1, name = "Workout", time = LocalTime(7, 30)),
        ReminderRowUi(habitId = 2, name = "Read", time = null),
        ReminderRowUi(habitId = 3, name = "No alcohol", time = null),
    ),
    isLoading = false,
)
