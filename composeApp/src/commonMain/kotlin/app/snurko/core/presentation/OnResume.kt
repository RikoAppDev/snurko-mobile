package app.snurko.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
fun OnResume(onResume: () -> Unit) {
    val currentOnResume by rememberUpdatedState(onResume)

    LifecycleResumeEffect(Unit) {
        currentOnResume()
        onPauseOrDispose { }
    }
}
