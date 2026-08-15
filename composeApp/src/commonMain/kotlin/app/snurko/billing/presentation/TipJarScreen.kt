package app.snurko.billing.presentation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snurko.billing.domain.TipProduct
import app.snurko.core.designsystem.BackChevronButton
import app.snurko.core.designsystem.QuietTapShape
import app.snurko.core.designsystem.SnurkoColors
import app.snurko.core.designsystem.SnurkoTheme
import app.snurko.core.designsystem.navigationBarInset
import app.snurko.core.designsystem.snurkoContentInsets
import app.snurko.core.domain.DataError
import app.snurko.resources.Res
import app.snurko.resources.tip_jar_amount
import app.snurko.resources.tip_jar_amounts_title
import app.snurko.resources.tip_jar_back
import app.snurko.resources.tip_jar_error_cancelled
import app.snurko.resources.tip_jar_error_disconnected
import app.snurko.resources.tip_jar_error_product_unavailable
import app.snurko.resources.tip_jar_error_unavailable
import app.snurko.resources.tip_jar_error_unknown
import app.snurko.resources.tip_jar_fees
import app.snurko.resources.tip_jar_free
import app.snurko.resources.tip_jar_pending
import app.snurko.resources.tip_jar_price_loading
import app.snurko.resources.tip_jar_price_retry
import app.snurko.resources.tip_jar_price_unavailable
import app.snurko.resources.tip_jar_thanks
import app.snurko.resources.tip_jar_thanks_note
import app.snurko.resources.tip_jar_title
import app.snurko.resources.tip_jar_unlocks_nothing
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val PAGE_PADDING = 20.dp
private val CARD_CORNER = RoundedCornerShape(16.dp)
private val AMOUNT_CORNER = RoundedCornerShape(12.dp)

@Composable
fun TipJarRoot(
    onNavigateBack: () -> Unit,
    viewModel: TipJarViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = SnurkoColors.paper,
        contentWindowInsets = snurkoContentInsets,
    ) { innerPadding ->
        TipJarScreen(
            state = state,
            onAction = viewModel::onAction,
            onNavigateBack = onNavigateBack,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun TipJarScreen(
    state: TipJarState,
    onAction: (TipJarAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SnurkoColors.paper)
            .verticalScroll(rememberScrollState()),
    ) {
        TipJarHeader(onNavigateBack = onNavigateBack)

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = PAGE_PADDING)) {
            TipJarCard(state = state, onAction = onAction)
            Spacer(Modifier.height(32.dp + navigationBarInset()))
        }
    }
}

@Composable
private fun TipJarHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_PADDING - 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackChevronButton(
            contentDescription = stringResource(Res.string.tip_jar_back),
            onClick = onNavigateBack,
        )
        Text(
            text = stringResource(Res.string.tip_jar_title),
            style = MaterialTheme.typography.displaySmall,
            color = SnurkoColors.text,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun TipJarCard(state: TipJarState, onAction: (TipJarAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SnurkoColors.line, CARD_CORNER)
            .background(SnurkoColors.surface, CARD_CORNER)
            .padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Text(
            text = stringResource(Res.string.tip_jar_free),
            style = MaterialTheme.typography.bodyLarge,
            color = SnurkoColors.text,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.tip_jar_unlocks_nothing),
            style = MaterialTheme.typography.bodyMedium,
            color = SnurkoColors.textSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.tip_jar_fees),
            style = MaterialTheme.typography.bodyMedium,
            color = SnurkoColors.textSecondary,
        )

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = SnurkoColors.lineSoft)
        Spacer(Modifier.height(18.dp))

        if (state.isThanksVisible) {
            ThanksNote()
        } else {
            Text(
                text = stringResource(Res.string.tip_jar_amounts_title),
                style = MaterialTheme.typography.titleMedium,
                color = SnurkoColors.text,
            )
        }
        Spacer(Modifier.height(14.dp))
        Amounts(state = state, onAction = onAction)

        if (state.isTipPending) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.tip_jar_pending),
                style = MaterialTheme.typography.bodyMedium,
                color = SnurkoColors.textSecondary,
            )
        }

        state.error?.let { error ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(billingErrorResource(error)),
                style = MaterialTheme.typography.bodyMedium,
                color = SnurkoColors.text,
            )
        }
    }
}

@Composable
private fun ThanksNote() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.tip_jar_thanks),
            style = MaterialTheme.typography.headlineMedium,
            color = SnurkoColors.accentInk,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.tip_jar_thanks_note),
            style = MaterialTheme.typography.bodyMedium,
            color = SnurkoColors.textSecondary,
        )
    }
}

@Composable
private fun Amounts(state: TipJarState, onAction: (TipJarAction) -> Unit) {
    when {
        state.isPriceLoading -> Text(
            text = stringResource(Res.string.tip_jar_price_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = SnurkoColors.textSecondary,
        )

        state.isPriceUnavailable -> Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.tip_jar_price_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = SnurkoColors.text,
            )
            QuietLink(
                text = stringResource(Res.string.tip_jar_price_retry),
                onClick = { onAction(TipJarAction.PriceRetryClicked) },
            )
        }

        else -> Column(modifier = Modifier.fillMaxWidth()) {
            state.amounts.forEach { amount ->
                AmountButton(
                    amount = amount,
                    isTipping = state.tippingProduct == amount.product,
                    isEnabled = !state.isTipping,
                    onClick = { onAction(TipJarAction.TipClicked(amount.product)) },
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AmountButton(
    amount: TipAmountUi,
    isTipping: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(Res.string.tip_jar_amount, amount.price)

    OutlinedButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = description },
        shape = AMOUNT_CORNER,
        border = BorderStroke(1.dp, if (isEnabled) SnurkoColors.accent else SnurkoColors.lineSoft),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SnurkoColors.accentInk,
            disabledContentColor = SnurkoColors.textSecondary,
        ),
    ) {
        if (isTipping) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = SnurkoColors.accentInk,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = amount.price, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun QuietLink(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(QuietTapShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = SnurkoColors.accentInk,
        )
    }
}

private fun billingErrorResource(error: DataError.Billing): StringResource = when (error) {
    DataError.Billing.UNAVAILABLE -> Res.string.tip_jar_error_unavailable
    DataError.Billing.DISCONNECTED -> Res.string.tip_jar_error_disconnected
    DataError.Billing.CANCELLED -> Res.string.tip_jar_error_cancelled
    DataError.Billing.PRODUCT_UNAVAILABLE -> Res.string.tip_jar_error_product_unavailable
    DataError.Billing.UNKNOWN -> Res.string.tip_jar_error_unknown
}

private val previewAmounts = listOf(
    TipAmountUi(product = TipProduct.SMALL, price = "€1.99"),
    TipAmountUi(product = TipProduct.MEDIUM, price = "€4.99"),
    TipAmountUi(product = TipProduct.LARGE, price = "€9.99"),
)

@Preview
@Composable
private fun TipJarPricedPreview() {
    SnurkoTheme {
        TipJarScreen(
            state = TipJarState(amounts = previewAmounts, isPriceLoading = false),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun TipJarPriceLoadingPreview() {
    SnurkoTheme {
        TipJarScreen(state = TipJarState(), onAction = {}, onNavigateBack = {})
    }
}

@Preview
@Composable
private fun TipJarPriceUnavailablePreview() {
    SnurkoTheme {
        TipJarScreen(
            state = TipJarState(isPriceLoading = false),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun TipJarTippingPreview() {
    SnurkoTheme {
        TipJarScreen(
            state = TipJarState(
                amounts = previewAmounts,
                isPriceLoading = false,
                tippingProduct = TipProduct.MEDIUM,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun TipJarThanksPreview() {
    SnurkoTheme {
        TipJarScreen(
            state = TipJarState(
                amounts = previewAmounts,
                isPriceLoading = false,
                isThanksVisible = true,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun TipJarPendingPreview() {
    SnurkoTheme {
        TipJarScreen(
            state = TipJarState(
                amounts = previewAmounts,
                isPriceLoading = false,
                isTipPending = true,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun TipJarErrorPreview() {
    SnurkoTheme {
        TipJarScreen(
            state = TipJarState(
                amounts = previewAmounts,
                isPriceLoading = false,
                error = DataError.Billing.DISCONNECTED,
            ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
