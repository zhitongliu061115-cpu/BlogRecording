package com.example.blogrecording.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ColorTokens.CoralLight,
    onPrimary = ColorTokens.CoralOnLight,
    primaryContainer = CoralSoftDark,
    onPrimaryContainer = ColorTokens.CoralContainerOnDark,
    secondary = ColorTokens.TealLight,
    onSecondary = ColorTokens.TealOnLight,
    secondaryContainer = TealSoftDark,
    onSecondaryContainer = ColorTokens.TealContainerOnDark,
    tertiary = ColorTokens.AmberLight,
    onTertiary = ColorTokens.AmberOnLight,
    background = PaperDark,
    onBackground = InkOnDark,
    surface = SurfaceDark,
    onSurface = InkOnDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = InkMutedDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = Coral,
    onPrimary = SurfaceLight,
    primaryContainer = CoralSoft,
    onPrimaryContainer = CoralDark,
    secondary = Teal,
    onSecondary = SurfaceLight,
    secondaryContainer = TealSoft,
    onSecondaryContainer = TealDark,
    tertiary = Amber,
    onTertiary = SurfaceLight,
    tertiaryContainer = AmberSoft,
    onTertiaryContainer = ColorTokens.AmberOnContainer,
    background = Paper,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkMuted,
    outline = OutlineLight
)

private object ColorTokens {
    val CoralLight = androidx.compose.ui.graphics.Color(0xFFFFB4A8)
    val CoralOnLight = androidx.compose.ui.graphics.Color(0xFF5F160F)
    val CoralContainerOnDark = androidx.compose.ui.graphics.Color(0xFFFFDAD3)
    val TealLight = androidx.compose.ui.graphics.Color(0xFF83D5CC)
    val TealOnLight = androidx.compose.ui.graphics.Color(0xFF003733)
    val TealContainerOnDark = androidx.compose.ui.graphics.Color(0xFFBCEDE6)
    val AmberLight = androidx.compose.ui.graphics.Color(0xFFFFC96F)
    val AmberOnLight = androidx.compose.ui.graphics.Color(0xFF442C00)
    val AmberOnContainer = androidx.compose.ui.graphics.Color(0xFF372400)
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp).copy(bottomEnd = ZeroCornerSize)
)

@Composable
fun BlogRecordingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
