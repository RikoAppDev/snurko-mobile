package app.snurko.challenge.presentation.started

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.snurko.core.designsystem.SnurkoColors
import app.snurko.core.designsystem.SnurkoTheme
import app.snurko.resources.Res
import app.snurko.resources.challenge_habit_count
import app.snurko.resources.challenge_started_day
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChallengeStartedScreen(
    day: Int,
    habitCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SnurkoColors.paper)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.challenge_started_day, day),
            style = MaterialTheme.typography.displayLarge,
            color = SnurkoColors.text,
        )
        Text(
            text = pluralStringResource(Res.plurals.challenge_habit_count, habitCount, habitCount),
            style = MaterialTheme.typography.bodyMedium,
            color = SnurkoColors.textMuted,
        )
    }
}

@Preview
@Composable
private fun ChallengeStartedScreenPreview() {
    SnurkoTheme {
        ChallengeStartedScreen(day = 1, habitCount = 3)
    }
}
