package app.snurko.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.snurko.resources.Res
import app.snurko.resources.caveat_bold
import app.snurko.resources.caveat_regular
import org.jetbrains.compose.resources.Font

private val snurkoColorScheme = lightColorScheme(
    primary = SnurkoColors.accentInk,
    onPrimary = SnurkoColors.surface,
    primaryContainer = SnurkoColors.accent,
    onPrimaryContainer = SnurkoColors.surface,
    secondary = SnurkoColors.accent,
    onSecondary = SnurkoColors.surface,
    background = SnurkoColors.paper,
    onBackground = SnurkoColors.text,
    surface = SnurkoColors.surface,
    onSurface = SnurkoColors.text,
    surfaceVariant = SnurkoColors.lineSoft,
    onSurfaceVariant = SnurkoColors.textMuted,
    outline = SnurkoColors.line,
    outlineVariant = SnurkoColors.lineSoft,
)

@Composable
private fun handwrittenFamily(): FontFamily = FontFamily(
    Font(Res.font.caveat_regular, FontWeight.Normal),
    Font(Res.font.caveat_bold, FontWeight.Bold),
)

@Composable
private fun snurkoTypography(): Typography {
    val hand = handwrittenFamily()
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(
            fontFamily = hand,
            fontWeight = FontWeight.Bold,
            fontSize = 56.sp,
            lineHeight = 60.sp,
        ),
        displayMedium = base.displayMedium.copy(
            fontFamily = hand,
            fontWeight = FontWeight.Bold,
            fontSize = 46.sp,
            lineHeight = 52.sp,
        ),
        displaySmall = base.displaySmall.copy(
            fontFamily = hand,
            fontWeight = FontWeight.Normal,
            fontSize = 38.sp,
            lineHeight = 44.sp,
        ),
        headlineLarge = base.headlineLarge.copy(
            fontFamily = hand,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 40.sp,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = hand,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 36.sp,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = hand,
            fontWeight = FontWeight.Normal,
            fontSize = 26.sp,
            lineHeight = 32.sp,
        ),
    )
}

@Composable
fun SnurkoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = snurkoColorScheme,
        typography = snurkoTypography(),
        content = content,
    )
}
