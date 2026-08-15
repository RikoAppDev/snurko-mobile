package app.snurko.core.designsystem

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * What a page hands its Scaffold: only the status bar. The navigation bar stays unclaimed so a
 * page runs to the bottom edge and its content scrolls underneath it.
 */
val snurkoContentInsets: WindowInsets
    @Composable get() = WindowInsets.statusBars

/** How far the navigation bar reaches up, for the bottom content padding of a scrolling page. */
@Composable
fun navigationBarInset(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
