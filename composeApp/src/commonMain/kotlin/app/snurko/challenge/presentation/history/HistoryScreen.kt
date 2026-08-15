package app.snurko.challenge.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snurko.billing.domain.isTipJarAvailable
import app.snurko.challenge.domain.NotebookStats
import app.snurko.core.designsystem.BackChevronButton
import app.snurko.core.designsystem.QuietTapShape
import app.snurko.core.designsystem.SnurkoColors
import app.snurko.core.designsystem.SnurkoTheme
import app.snurko.core.designsystem.navigationBarInset
import app.snurko.core.designsystem.snurkoContentInsets
import app.snurko.core.domain.DataError
import app.snurko.core.presentation.ObserveAsEvents
import app.snurko.resources.Res
import app.snurko.resources.backup_error_document_unavailable
import app.snurko.resources.backup_error_malformed
import app.snurko.resources.backup_error_nothing_to_export
import app.snurko.resources.backup_error_unknown
import app.snurko.resources.backup_error_unsupported_version
import app.snurko.resources.history_attempt
import app.snurko.resources.history_back
import app.snurko.resources.history_completed
import app.snurko.resources.history_completion
import app.snurko.resources.history_date
import app.snurko.resources.history_date_range
import app.snurko.resources.history_date_since
import app.snurko.resources.history_export
import app.snurko.resources.history_exported
import app.snurko.resources.history_failed
import app.snurko.resources.history_failed_missed
import app.snurko.resources.history_import
import app.snurko.resources.history_import_confirm_body
import app.snurko.resources.history_import_confirm_cancel
import app.snurko.resources.history_import_confirm_replace
import app.snurko.resources.history_import_confirm_title
import app.snurko.resources.history_imported
import app.snurko.resources.history_in_progress
import app.snurko.resources.history_missed_separator
import app.snurko.resources.history_past_empty
import app.snurko.resources.history_stats_best
import app.snurko.resources.history_stats_count
import app.snurko.resources.history_stats_marks
import app.snurko.resources.history_stats_percent
import app.snurko.resources.history_stats_streak
import app.snurko.resources.history_stats_title
import app.snurko.resources.history_support
import app.snurko.resources.history_title
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val MINI_GRID_COLUMNS = 30
private val MINI_CELL_SIZE = 4.dp
private val MINI_CELL_GAP = 2.dp
private val MINI_CELL_CORNER = RoundedCornerShape(1.dp)
private val PANEL_CORNER = RoundedCornerShape(12.dp)
private val PAGE_PADDING = 20.dp

@Composable
fun HistoryRoot(
    onNavigateBack: () -> Unit,
    onOpenTipJar: () -> Unit,
    viewModel: HistoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val exportedMessage = stringResource(Res.string.history_exported)
    val importedMessage = stringResource(Res.string.history_imported)
    val unknownMessage = stringResource(Res.string.backup_error_unknown)
    val errorMessages = backupErrorMessages()

    ObserveAsEvents(viewModel.events) { event ->
        val message = when (event) {
            HistoryEvent.NotebookExported -> exportedMessage
            HistoryEvent.NotebookImported -> importedMessage
            is HistoryEvent.BackupFailed -> errorMessages[event.error] ?: unknownMessage
        }
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        containerColor = SnurkoColors.paper,
        contentWindowInsets = snurkoContentInsets,
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        },
    ) { innerPadding ->
        HistoryScreen(
            state = state,
            onAction = viewModel::onAction,
            onNavigateBack = onNavigateBack,
            onOpenTipJar = onOpenTipJar,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun HistoryScreen(
    state: HistoryState,
    onAction: (HistoryAction) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenTipJar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SnurkoColors.paper),
    ) {
        HistoryHeader(onNavigateBack = onNavigateBack)

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = PAGE_PADDING,
                end = PAGE_PADDING,
                top = 8.dp,
                bottom = 8.dp + navigationBarInset(),
            ),
        ) {
            state.current?.let { current ->
                item(key = "current-${current.number}") {
                    AttemptEntry(row = current, isCompletionVisible = false)
                }
            }
            state.stats?.let { stats ->
                item(key = "stats") {
                    StatsPanel(stats = stats)
                }
            }
            items(state.past, key = { it.number }) { row ->
                AttemptEntry(row = row, isCompletionVisible = true)
            }
            if (!state.isLoading && state.past.isEmpty()) {
                item(key = "past-empty") {
                    Text(
                        text = stringResource(Res.string.history_past_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnurkoColors.textSecondary,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            }
            item(key = "backup") {
                BackupRows(
                    isEnabled = !state.isBusy,
                    onExport = { onAction(HistoryAction.ExportClicked) },
                    onImport = { onAction(HistoryAction.ImportClicked) },
                )
            }
            if (isTipJarAvailable) {
                item(key = "support") {
                    SupportRow(onClick = onOpenTipJar)
                }
            }
        }
    }

    if (state.isImportConfirmationVisible) {
        ImportConfirmationDialog(
            onConfirm = { onAction(HistoryAction.ImportConfirmed) },
            onDismiss = { onAction(HistoryAction.ImportCancelled) },
        )
    }
}

@Composable
private fun HistoryHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_PADDING - 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackChevronButton(
            contentDescription = stringResource(Res.string.history_back),
            onClick = onNavigateBack,
        )
        Text(
            text = stringResource(Res.string.history_title),
            style = MaterialTheme.typography.displaySmall,
            color = SnurkoColors.text,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun AttemptEntry(row: AttemptRowUi, isCompletionVisible: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Text(
            text = stringResource(Res.string.history_attempt, row.number),
            style = MaterialTheme.typography.titleMedium,
            color = SnurkoColors.text,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = outcomeText(row.outcome),
            style = MaterialTheme.typography.bodyLarge,
            color = SnurkoColors.text,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = datesText(row),
            style = MaterialTheme.typography.bodySmall,
            color = SnurkoColors.textSecondary,
        )
        if (isCompletionVisible) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(Res.string.history_completion, row.completionPercent),
                style = MaterialTheme.typography.bodySmall,
                color = SnurkoColors.textSecondary,
            )
        }
        Spacer(Modifier.height(10.dp))
        MiniGrid(markedDays = row.markedDays)
    }
    HorizontalDivider(color = SnurkoColors.line)
}

@Composable
private fun StatsPanel(stats: NotebookStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp)
            .border(1.dp, SnurkoColors.line, PANEL_CORNER)
            .background(SnurkoColors.surface, PANEL_CORNER)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.history_stats_title),
            style = MaterialTheme.typography.titleMedium,
            color = SnurkoColors.text,
        )
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatFigure(
                value = stringResource(Res.string.history_stats_percent, stats.bestCompletionPercent),
                label = stringResource(Res.string.history_stats_best),
                modifier = Modifier.weight(1f),
            )
            StatFigure(
                value = stringResource(Res.string.history_stats_count, stats.longestStreak),
                label = stringResource(Res.string.history_stats_streak),
                modifier = Modifier.weight(1f),
            )
            StatFigure(
                value = stringResource(Res.string.history_stats_count, stats.totalMarks),
                label = stringResource(Res.string.history_stats_marks),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatFigure(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = SnurkoColors.text,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SnurkoColors.textSecondary,
        )
    }
}

@Composable
private fun outcomeText(outcome: AttemptOutcomeUi): String = when (outcome) {
    is AttemptOutcomeUi.InProgress -> stringResource(Res.string.history_in_progress, outcome.day)
    is AttemptOutcomeUi.Failed -> if (outcome.missedHabits.isEmpty()) {
        stringResource(Res.string.history_failed, outcome.failedOnDay)
    } else {
        stringResource(
            Res.string.history_failed_missed,
            outcome.failedOnDay,
            outcome.missedHabits.joinToString(stringResource(Res.string.history_missed_separator)),
        )
    }

    AttemptOutcomeUi.Completed -> stringResource(Res.string.history_completed)
}

@Composable
private fun datesText(row: AttemptRowUi): String {
    val start = formattedDate(row.startDate)
    return row.endDate
        ?.let { end -> stringResource(Res.string.history_date_range, start, formattedDate(end)) }
        ?: stringResource(Res.string.history_date_since, start)
}

@Composable
private fun formattedDate(date: LocalDate): String =
    stringResource(Res.string.history_date, date.day, date.month.ordinal + 1, date.year)

/**
 * A decorative echo of the home grid: fills only, no marks, and no semantics — the
 * outcome line above already says everything this attempt amounts to.
 */
@Composable
private fun MiniGrid(markedDays: List<Boolean>) {
    Column(
        modifier = Modifier.clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(MINI_CELL_GAP),
    ) {
        markedDays.chunked(MINI_GRID_COLUMNS).forEach { rowDays ->
            Row(horizontalArrangement = Arrangement.spacedBy(MINI_CELL_GAP)) {
                rowDays.forEach { isMarked ->
                    Box(
                        modifier = Modifier
                            .size(MINI_CELL_SIZE)
                            .clip(MINI_CELL_CORNER)
                            .background(if (isMarked) SnurkoColors.text else SnurkoColors.lineSoft),
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupRows(isEnabled: Boolean, onExport: () -> Unit, onImport: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) {
        QuietRow(
            label = stringResource(Res.string.history_export),
            isEnabled = isEnabled,
            onClick = onExport,
        )
        HorizontalDivider(color = SnurkoColors.line)
        QuietRow(
            label = stringResource(Res.string.history_import),
            isEnabled = isEnabled,
            onClick = onImport,
        )
        HorizontalDivider(color = SnurkoColors.line)
    }
}

@Composable
private fun SupportRow(onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 28.dp)) {
        QuietRow(label = stringResource(Res.string.history_support), onClick = onClick)
        HorizontalDivider(color = SnurkoColors.line)
    }
}

@Composable
private fun QuietRow(label: String, onClick: () -> Unit, isEnabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(QuietTapShape)
            .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SnurkoColors.textSecondary,
        )
    }
}

@Composable
private fun ImportConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SnurkoColors.surface,
        titleContentColor = SnurkoColors.text,
        textContentColor = SnurkoColors.text,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = stringResource(Res.string.history_import_confirm_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.history_import_confirm_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = SnurkoColors.accentInk),
            ) {
                Text(stringResource(Res.string.history_import_confirm_replace))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = SnurkoColors.textMuted),
            ) {
                Text(stringResource(Res.string.history_import_confirm_cancel))
            }
        },
    )
}

@Composable
private fun backupErrorMessages(): Map<DataError.Backup, String> = mapOf(
    DataError.Backup.NOTHING_TO_EXPORT to stringResource(Res.string.backup_error_nothing_to_export),
    DataError.Backup.DOCUMENT_UNAVAILABLE to stringResource(Res.string.backup_error_document_unavailable),
    DataError.Backup.MALFORMED_NOTEBOOK to stringResource(Res.string.backup_error_malformed),
    DataError.Backup.UNSUPPORTED_SCHEMA_VERSION to stringResource(Res.string.backup_error_unsupported_version),
    DataError.Backup.UNKNOWN to stringResource(Res.string.backup_error_unknown),
)

@Preview
@Composable
private fun HistoryScreenPreview() {
    SnurkoTheme {
        HistoryScreen(
            state = previewState,
            onAction = {},
            onNavigateBack = {},
            onOpenTipJar = {},
        )
    }
}

@Preview
@Composable
private fun HistoryImportConfirmationPreview() {
    SnurkoTheme {
        HistoryScreen(
            state = previewState.copy(isImportConfirmationVisible = true),
            onAction = {},
            onNavigateBack = {},
            onOpenTipJar = {},
        )
    }
}

@Preview
@Composable
private fun HistoryFirstAttemptPreview() {
    SnurkoTheme {
        HistoryScreen(
            state = HistoryState(
                rows = listOf(previewState.rows.first()),
                stats = previewStats,
                isLoading = false,
            ),
            onAction = {},
            onNavigateBack = {},
            onOpenTipJar = {},
        )
    }
}

private fun previewMarkedDays(completeDays: Int): List<Boolean> = List(90) { it < completeDays }

private val previewStats = NotebookStats(
    bestCompletionPercent = 100,
    longestStreak = 90,
    totalMarks = 123,
)

private val previewState = HistoryState(
    rows = listOf(
        AttemptRowUi(
            number = 3,
            startDate = LocalDate(2026, 4, 1),
            endDate = null,
            outcome = AttemptOutcomeUi.InProgress(day = 12),
            markedDays = previewMarkedDays(11),
            completionPercent = 12,
        ),
        AttemptRowUi(
            number = 2,
            startDate = LocalDate(2026, 3, 9),
            endDate = LocalDate(2026, 3, 31),
            outcome = AttemptOutcomeUi.Failed(
                failedOnDay = 23,
                missedHabits = listOf("Reading", "No alcohol"),
            ),
            markedDays = previewMarkedDays(22),
            completionPercent = 24,
        ),
        AttemptRowUi(
            number = 1,
            startDate = LocalDate(2025, 12, 10),
            endDate = LocalDate(2026, 3, 9),
            outcome = AttemptOutcomeUi.Completed,
            markedDays = previewMarkedDays(90),
            completionPercent = 100,
        ),
    ),
    stats = previewStats,
    isLoading = false,
)
