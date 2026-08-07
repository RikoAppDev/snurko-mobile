package app.snurko.challenge.presentation.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snurko.core.designsystem.SnurkoColors
import app.snurko.core.designsystem.SnurkoTheme
import app.snurko.core.presentation.ObserveAsEvents
import app.snurko.resources.Res
import app.snurko.resources.challenge_habit_count
import app.snurko.resources.challenge_setup_add_habit
import app.snurko.resources.challenge_setup_cancel_edit
import app.snurko.resources.challenge_setup_confirm_cancel
import app.snurko.resources.challenge_setup_confirm_locked
import app.snurko.resources.challenge_setup_confirm_restart
import app.snurko.resources.challenge_setup_confirm_start
import app.snurko.resources.challenge_setup_confirm_title
import app.snurko.resources.challenge_setup_edit_habit
import app.snurko.resources.challenge_setup_fail_condition_label
import app.snurko.resources.challenge_setup_fail_condition_placeholder
import app.snurko.resources.challenge_setup_fail_condition_summary
import app.snurko.resources.challenge_setup_habit_name_label
import app.snurko.resources.challenge_setup_habit_name_placeholder
import app.snurko.resources.challenge_setup_hint_duplicate_name
import app.snurko.resources.challenge_setup_hint_name_required
import app.snurko.resources.challenge_setup_invitation
import app.snurko.resources.challenge_setup_remove_habit
import app.snurko.resources.challenge_setup_remove_mark
import app.snurko.resources.challenge_setup_save_habit
import app.snurko.resources.challenge_setup_start
import app.snurko.resources.challenge_setup_start_failed
import app.snurko.resources.challenge_setup_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChallengeSetupRoot(
    onChallengeStarted: () -> Unit = {},
    viewModel: ChallengeSetupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val startFailedMessage = stringResource(Res.string.challenge_setup_start_failed)

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ChallengeSetupEvent.ChallengeStarted -> onChallengeStarted()
            ChallengeSetupEvent.StartFailed -> scope.launch {
                snackbarHostState.showSnackbar(startFailedMessage)
            }
        }
    }

    Scaffold(
        containerColor = SnurkoColors.paper,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        ChallengeSetupScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun ChallengeSetupScreen(
    state: ChallengeSetupState,
    onAction: (ChallengeSetupAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SnurkoColors.paper)
            .imePadding(),
    ) {
        SetupHeader(showInvitation = state.habits.isEmpty())

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            items(state.habits, key = { it.id }) { habit ->
                HabitLine(
                    habit = habit,
                    isEdited = habit.id == state.editedHabitId,
                    onEdit = { onAction(ChallengeSetupAction.HabitEditRequested(habit.id)) },
                    onRemove = { onAction(ChallengeSetupAction.HabitRemoved(habit.id)) },
                )
            }
        }

        HabitInput(state = state, onAction = onAction)
        StartBar(state = state, onAction = onAction)
    }

    if (state.isConfirmingStart) {
        StartConfirmationDialog(
            onConfirm = { onAction(ChallengeSetupAction.StartConfirmed) },
            onDismiss = { onAction(ChallengeSetupAction.StartCancelled) },
        )
    }
}

@Composable
private fun SetupHeader(showInvitation: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 20.dp),
    ) {
        Text(
            text = stringResource(Res.string.challenge_setup_title),
            style = MaterialTheme.typography.displayLarge,
            color = SnurkoColors.text,
        )
        if (showInvitation) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.challenge_setup_invitation),
                style = MaterialTheme.typography.bodyMedium,
                color = SnurkoColors.textMuted,
            )
        }
    }
}

@Composable
private fun HabitLine(
    habit: HabitDraftUi,
    isEdited: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    val editLabel = stringResource(Res.string.challenge_setup_edit_habit, habit.name)
    val removeLabel = stringResource(Res.string.challenge_setup_remove_habit, habit.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (isEdited) SnurkoColors.surface else Color.Transparent)
            .clickable(onClickLabel = editLabel, role = Role.Button, onClick = onEdit)
            .padding(start = 24.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.bodyLarge,
                color = SnurkoColors.text,
            )
            habit.failCondition?.let { failCondition ->
                Text(
                    text = stringResource(
                        Res.string.challenge_setup_fail_condition_summary,
                        failCondition,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = SnurkoColors.textMuted,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onRemove)
                .semantics { contentDescription = removeLabel },
        ) {
            Text(
                text = stringResource(Res.string.challenge_setup_remove_mark),
                style = MaterialTheme.typography.headlineSmall,
                color = SnurkoColors.textMuted,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clearAndSetSemantics { },
            )
        }
    }

    HorizontalDivider(color = SnurkoColors.line)
}

@Composable
private fun HabitInput(
    state: ChallengeSetupState,
    onAction: (ChallengeSetupAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PaperTextField(
            value = state.habitNameInput,
            onValueChange = { onAction(ChallengeSetupAction.HabitNameChanged(it)) },
            label = stringResource(Res.string.challenge_setup_habit_name_label),
            placeholder = stringResource(Res.string.challenge_setup_habit_name_placeholder),
            imeAction = ImeAction.Next,
        )
        PaperTextField(
            value = state.failConditionInput,
            onValueChange = { onAction(ChallengeSetupAction.FailConditionChanged(it)) },
            label = stringResource(Res.string.challenge_setup_fail_condition_label),
            placeholder = stringResource(Res.string.challenge_setup_fail_condition_placeholder),
            imeAction = ImeAction.Done,
            onImeAction = { onAction(ChallengeSetupAction.HabitSubmitted) },
        )

        val hint = when (state.habitInputHint) {
            HabitInputHint.NONE -> null
            HabitInputHint.NAME_REQUIRED -> stringResource(Res.string.challenge_setup_hint_name_required)
            HabitInputHint.DUPLICATE_NAME -> stringResource(Res.string.challenge_setup_hint_duplicate_name)
        }
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = SnurkoColors.textMuted,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.isEditing) {
                TextButton(
                    onClick = { onAction(ChallengeSetupAction.HabitEditCancelled) },
                    colors = ButtonDefaults.textButtonColors(contentColor = SnurkoColors.textMuted),
                ) {
                    Text(stringResource(Res.string.challenge_setup_cancel_edit))
                }
            }
            TextButton(
                onClick = { onAction(ChallengeSetupAction.HabitSubmitted) },
                enabled = state.canSubmitHabit,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = SnurkoColors.accentInk,
                    disabledContentColor = SnurkoColors.textMuted,
                ),
            ) {
                Text(
                    text = if (state.isEditing) {
                        stringResource(Res.string.challenge_setup_save_habit)
                    } else {
                        stringResource(Res.string.challenge_setup_add_habit)
                    },
                )
            }
        }
    }
}

@Composable
private fun PaperTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    imeAction: ImeAction,
    onImeAction: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onImeAction() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SnurkoColors.text,
            unfocusedTextColor = SnurkoColors.text,
            focusedContainerColor = SnurkoColors.surface,
            unfocusedContainerColor = SnurkoColors.surface,
            cursorColor = SnurkoColors.accentInk,
            focusedBorderColor = SnurkoColors.accent,
            unfocusedBorderColor = SnurkoColors.line,
            focusedLabelColor = SnurkoColors.accentInk,
            unfocusedLabelColor = SnurkoColors.textMuted,
            focusedPlaceholderColor = SnurkoColors.textMuted,
            unfocusedPlaceholderColor = SnurkoColors.textMuted,
        ),
    )
}

@Composable
private fun StartBar(
    state: ChallengeSetupState,
    onAction: (ChallengeSetupAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = pluralStringResource(
                Res.plurals.challenge_habit_count,
                state.habitCount,
                state.habitCount,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = SnurkoColors.textMuted,
        )
        Button(
            onClick = { onAction(ChallengeSetupAction.StartRequested) },
            enabled = state.canStart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnurkoColors.accentInk,
                contentColor = SnurkoColors.surface,
                disabledContainerColor = SnurkoColors.lineSoft,
                disabledContentColor = SnurkoColors.textMuted,
            ),
        ) {
            Text(
                text = stringResource(Res.string.challenge_setup_start),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun StartConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SnurkoColors.surface,
        titleContentColor = SnurkoColors.text,
        textContentColor = SnurkoColors.text,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = stringResource(Res.string.challenge_setup_confirm_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.challenge_setup_confirm_locked),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(Res.string.challenge_setup_confirm_restart),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = SnurkoColors.accentInk),
            ) {
                Text(stringResource(Res.string.challenge_setup_confirm_start))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = SnurkoColors.textMuted),
            ) {
                Text(stringResource(Res.string.challenge_setup_confirm_cancel))
            }
        },
    )
}

@Preview
@Composable
private fun ChallengeSetupScreenEmptyPreview() {
    SnurkoTheme {
        ChallengeSetupScreen(state = ChallengeSetupState(), onAction = {})
    }
}

@Preview
@Composable
private fun ChallengeSetupScreenWithHabitsPreview() {
    SnurkoTheme {
        ChallengeSetupScreen(state = previewState, onAction = {})
    }
}

@Preview
@Composable
private fun ChallengeSetupScreenConfirmingPreview() {
    SnurkoTheme {
        ChallengeSetupScreen(state = previewState.copy(isConfirmingStart = true), onAction = {})
    }
}

private val previewState = ChallengeSetupState(
    habits = listOf(
        HabitDraftUi(id = 1, name = "Workout", failCondition = "less than 30 minutes"),
        HabitDraftUi(id = 2, name = "Read", failCondition = "fewer than 10 pages"),
        HabitDraftUi(id = 3, name = "No alcohol", failCondition = null),
    ),
)
