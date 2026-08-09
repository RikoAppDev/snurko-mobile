package app.snurko.billing.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snurko.core.designsystem.SnurkoColors
import app.snurko.core.designsystem.SnurkoTheme
import app.snurko.core.domain.DataError
import app.snurko.core.presentation.ObserveAsEvents
import app.snurko.resources.Res
import app.snurko.resources.unlock_back
import app.snurko.resources.unlock_back_mark
import app.snurko.resources.unlock_buy
import app.snurko.resources.unlock_error_cancelled
import app.snurko.resources.unlock_error_disconnected
import app.snurko.resources.unlock_error_nothing_to_restore
import app.snurko.resources.unlock_error_product_unavailable
import app.snurko.resources.unlock_error_unavailable
import app.snurko.resources.unlock_error_unknown
import app.snurko.resources.unlock_forever
import app.snurko.resources.unlock_free_backup
import app.snurko.resources.unlock_free_challenge
import app.snurko.resources.unlock_free_reminders
import app.snurko.resources.unlock_free_sticker
import app.snurko.resources.unlock_free_title
import app.snurko.resources.unlock_included_mark
import app.snurko.resources.unlock_included_stats
import app.snurko.resources.unlock_included_themes
import app.snurko.resources.unlock_included_title
import app.snurko.resources.unlock_included_widget
import app.snurko.resources.unlock_owned
import app.snurko.resources.unlock_owned_note
import app.snurko.resources.unlock_pending
import app.snurko.resources.unlock_price_loading
import app.snurko.resources.unlock_price_retry
import app.snurko.resources.unlock_price_unavailable
import app.snurko.resources.unlock_restore
import app.snurko.resources.unlock_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val PAGE_PADDING = 20.dp
private val CARD_CORNER = RoundedCornerShape(16.dp)

@Composable
fun UnlockRoot(
    onNavigateBack: () -> Unit,
    viewModel: UnlockViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val unlockedMessage = stringResource(Res.string.unlock_owned)

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            UnlockEvent.Unlocked -> scope.launch { snackbarHostState.showSnackbar(unlockedMessage) }
        }
    }

    Scaffold(
        containerColor = SnurkoColors.paper,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        UnlockScreen(
            state = state,
            onAction = viewModel::onAction,
            onNavigateBack = onNavigateBack,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun UnlockScreen(
    state: UnlockState,
    onAction: (UnlockAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SnurkoColors.paper)
            .verticalScroll(rememberScrollState()),
    ) {
        UnlockHeader(onNavigateBack = onNavigateBack)

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = PAGE_PADDING)) {
            UnlockCard(state = state, onAction = onAction)
            Spacer(Modifier.height(28.dp))
            FreeForever()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun UnlockHeader(onNavigateBack: () -> Unit) {
    val backDescription = stringResource(Res.string.unlock_back)

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
                text = stringResource(Res.string.unlock_back_mark),
                style = MaterialTheme.typography.titleLarge,
                color = SnurkoColors.text,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        Text(
            text = stringResource(Res.string.unlock_title),
            style = MaterialTheme.typography.displaySmall,
            color = SnurkoColors.text,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun UnlockCard(state: UnlockState, onAction: (UnlockAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SnurkoColors.line, CARD_CORNER)
            .background(SnurkoColors.surface, CARD_CORNER)
            .padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        if (state.isUnlocked) {
            Text(
                text = stringResource(Res.string.unlock_owned),
                style = MaterialTheme.typography.titleMedium,
                color = SnurkoColors.accentInk,
            )
            Spacer(Modifier.height(14.dp))
        }

        Text(
            text = stringResource(Res.string.unlock_included_title),
            style = MaterialTheme.typography.titleMedium,
            color = SnurkoColors.text,
        )
        Spacer(Modifier.height(10.dp))
        IncludedLine(stringResource(Res.string.unlock_included_widget))
        IncludedLine(stringResource(Res.string.unlock_included_stats))
        IncludedLine(stringResource(Res.string.unlock_included_themes))

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = SnurkoColors.lineSoft)
        Spacer(Modifier.height(18.dp))

        Text(
            text = stringResource(Res.string.unlock_forever),
            style = MaterialTheme.typography.bodyMedium,
            color = SnurkoColors.textSecondary,
        )

        if (state.isUnlocked) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.unlock_owned_note),
                style = MaterialTheme.typography.bodySmall,
                color = SnurkoColors.textSecondary,
            )
        } else {
            Spacer(Modifier.height(20.dp))
            BuyRow(state = state, onAction = onAction)
        }

        if (state.isPurchasePending) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(Res.string.unlock_pending),
                style = MaterialTheme.typography.bodyMedium,
                color = SnurkoColors.textSecondary,
            )
        }

        state.error?.let { error ->
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(billingErrorResource(error)),
                style = MaterialTheme.typography.bodyMedium,
                color = SnurkoColors.text,
            )
        }
    }
}

@Composable
private fun BuyRow(state: UnlockState, onAction: (UnlockAction) -> Unit) {
    if (state.isPriceUnavailable) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.unlock_price_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = SnurkoColors.text,
            )
            Spacer(Modifier.width(8.dp))
            QuietLink(
                text = stringResource(Res.string.unlock_price_retry),
                onClick = { onAction(UnlockAction.PriceRetryClicked) },
            )
        }
    } else {
        Button(
            onClick = { onAction(UnlockAction.BuyClicked) },
            enabled = state.canBuy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SnurkoColors.accentInk,
                contentColor = SnurkoColors.surface,
                disabledContainerColor = SnurkoColors.lineSoft,
                disabledContentColor = SnurkoColors.textSecondary,
            ),
        ) {
            if (state.isPurchasing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = SnurkoColors.surface,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = state.price
                        ?.let { stringResource(Res.string.unlock_buy, it) }
                        ?: stringResource(Res.string.unlock_price_loading),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    QuietLink(
        text = stringResource(Res.string.unlock_restore),
        onClick = { onAction(UnlockAction.RestoreClicked) },
        isEnabled = !state.isBusy,
    )
}

@Composable
private fun QuietLink(text: String, onClick: () -> Unit, isEnabled: Boolean = true) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = SnurkoColors.accentInk,
        )
    }
}

@Composable
private fun IncludedLine(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(
            text = stringResource(Res.string.unlock_included_mark),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
            color = SnurkoColors.accent,
            modifier = Modifier.width(22.dp).alignByBaseline().clearAndSetSemantics { },
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = SnurkoColors.text,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
private fun FreeForever() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.unlock_free_title),
            style = MaterialTheme.typography.titleMedium,
            color = SnurkoColors.text,
        )
        Spacer(Modifier.height(8.dp))
        listOf(
            Res.string.unlock_free_challenge,
            Res.string.unlock_free_reminders,
            Res.string.unlock_free_backup,
            Res.string.unlock_free_sticker,
        ).forEach { line ->
            Text(
                text = stringResource(line),
                style = MaterialTheme.typography.bodyMedium,
                color = SnurkoColors.textSecondary,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

private fun billingErrorResource(error: DataError.Billing): StringResource = when (error) {
    DataError.Billing.UNAVAILABLE -> Res.string.unlock_error_unavailable
    DataError.Billing.DISCONNECTED -> Res.string.unlock_error_disconnected
    DataError.Billing.CANCELLED -> Res.string.unlock_error_cancelled
    DataError.Billing.PRODUCT_UNAVAILABLE -> Res.string.unlock_error_product_unavailable
    DataError.Billing.NOTHING_TO_RESTORE -> Res.string.unlock_error_nothing_to_restore
    DataError.Billing.UNKNOWN -> Res.string.unlock_error_unknown
}

@Preview
@Composable
private fun UnlockPricedPreview() {
    SnurkoTheme {
        UnlockScreen(
            state = UnlockState(price = "€4.99", isPriceLoading = false),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun UnlockPriceLoadingPreview() {
    SnurkoTheme {
        UnlockScreen(state = UnlockState(), onAction = {}, onNavigateBack = {})
    }
}

@Preview
@Composable
private fun UnlockPriceUnavailablePreview() {
    SnurkoTheme {
        UnlockScreen(
            state = UnlockState(isPriceLoading = false),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun UnlockPurchasingPreview() {
    SnurkoTheme {
        UnlockScreen(
            state = UnlockState(price = "€4.99", isPriceLoading = false, isPurchasing = true),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun UnlockPendingPreview() {
    SnurkoTheme {
        UnlockScreen(
            state = UnlockState(price = "€4.99", isPriceLoading = false, isPurchasePending = true),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun UnlockErrorPreview() {
    SnurkoTheme {
        UnlockScreen(
            state = UnlockState(
                price = "€4.99",
                isPriceLoading = false,
                error = DataError.Billing.NOTHING_TO_RESTORE,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun UnlockOwnedPreview() {
    SnurkoTheme {
        UnlockScreen(
            state = UnlockState(isUnlocked = true, price = "€4.99", isPriceLoading = false),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
