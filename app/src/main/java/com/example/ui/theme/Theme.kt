package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OPXColorScheme = darkColorScheme(
    primary = CyanBlue,
    onPrimary = Color.Black,
    secondary = SafetyOrange,
    onSecondary = Color.Black,
    tertiary = MustardOchre,
    onTertiary = Color.Black,
    background = ChassisBackground,
    onBackground = TextPrimary,
    surface = ChassisSurface,
    onSurface = TextPrimary,
    surfaceVariant = ChassisCard,
    onSurfaceVariant = TextSecondary,
    outline = ChassisBorder
)

@Composable
fun OPXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OPXColorScheme,
        typography = Typography,
        content = content
    )
}
