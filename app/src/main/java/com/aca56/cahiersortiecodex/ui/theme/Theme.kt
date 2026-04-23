package com.aca56.cahiersortiecodex.ui.theme

import android.graphics.Color.parseColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.aca56.cahiersortiecodex.data.settings.ThemeMode

@Composable
fun CahierSortieCodexTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    primaryColorHex: String = "#00684E",
    secondaryColorHex: String = "#1F7A5E",
    tertiaryColorHex: String = "#2F8B68",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = appColorScheme(
        darkTheme = darkTheme,
        primary = parseThemeColor(primaryColorHex, ForestGreenLight),
        secondary = parseThemeColor(secondaryColorHex, SecondaryGreenLight),
        tertiary = parseThemeColor(tertiaryColorHex, TertiaryGreenLight),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun appColorScheme(
    darkTheme: Boolean,
    primary: Color,
    secondary: Color,
    tertiary: Color,
): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = blendTowards(primary, Color.White, 0.35f),
            onPrimary = colorOn(blendTowards(primary, Color.White, 0.35f)),
            primaryContainer = blendTowards(primary, Color.Black, 0.25f),
            onPrimaryContainer = colorOn(blendTowards(primary, Color.Black, 0.25f)),
            secondary = blendTowards(secondary, Color.White, 0.30f),
            onSecondary = colorOn(blendTowards(secondary, Color.White, 0.30f)),
            secondaryContainer = blendTowards(secondary, Color.Black, 0.20f),
            onSecondaryContainer = colorOn(blendTowards(secondary, Color.Black, 0.20f)),
            tertiary = blendTowards(tertiary, Color.White, 0.28f),
            onTertiary = colorOn(blendTowards(tertiary, Color.White, 0.28f)),
            tertiaryContainer = blendTowards(tertiary, Color.Black, 0.22f),
            onTertiaryContainer = colorOn(blendTowards(tertiary, Color.Black, 0.22f)),
            background = AppBackgroundDark,
            onBackground = AppOnBackgroundDark,
            surface = AppSurfaceDark,
            onSurface = AppOnSurfaceDark,
            surfaceVariant = AppSurfaceVariantDark,
            onSurfaceVariant = AppOnSurfaceVariantDark,
            surfaceTint = blendTowards(primary, Color.White, 0.35f),
            surfaceDim = AppSurfaceDimDark,
            surfaceBright = AppSurfaceBrightDark,
            surfaceContainerLowest = tintSurface(AppSurfaceContainerLowestDark, primary, darkTheme),
            surfaceContainerLow = tintSurface(AppSurfaceContainerLowDark, primary, darkTheme),
            surfaceContainer = tintSurface(AppSurfaceContainerDark, primary, darkTheme),
            surfaceContainerHigh = tintSurface(AppSurfaceContainerHighDark, primary, darkTheme),
            surfaceContainerHighest = tintSurface(AppSurfaceContainerHighestDark, primary, darkTheme),
            outline = AppOutlineDark,
            outlineVariant = AppOutlineVariantDark,
            error = AppErrorDark,
            onError = AppOnErrorDark,
            errorContainer = AppErrorContainerDark,
            onErrorContainer = AppOnErrorContainerDark,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = colorOn(primary),
            primaryContainer = blendTowards(primary, Color.White, 0.70f),
            onPrimaryContainer = colorOn(blendTowards(primary, Color.White, 0.70f)),
            secondary = secondary,
            onSecondary = colorOn(secondary),
            secondaryContainer = blendTowards(secondary, Color.White, 0.68f),
            onSecondaryContainer = colorOn(blendTowards(secondary, Color.White, 0.68f)),
            tertiary = tertiary,
            onTertiary = colorOn(tertiary),
            tertiaryContainer = blendTowards(tertiary, Color.White, 0.66f),
            onTertiaryContainer = colorOn(blendTowards(tertiary, Color.White, 0.66f)),
            background = AppBackgroundLight,
            onBackground = AppOnBackgroundLight,
            surface = AppSurfaceLight,
            onSurface = AppOnSurfaceLight,
            surfaceVariant = AppSurfaceVariantLight,
            onSurfaceVariant = AppOnSurfaceVariantLight,
            surfaceTint = primary,
            surfaceDim = AppSurfaceDimLight,
            surfaceBright = AppSurfaceBrightLight,
            surfaceContainerLowest = tintSurface(AppSurfaceContainerLowestLight, primary, darkTheme),
            surfaceContainerLow = tintSurface(AppSurfaceContainerLowLight, primary, darkTheme),
            surfaceContainer = tintSurface(AppSurfaceContainerLight, primary, darkTheme),
            surfaceContainerHigh = tintSurface(AppSurfaceContainerHighLight, primary, darkTheme),
            surfaceContainerHighest = tintSurface(AppSurfaceContainerHighestLight, primary, darkTheme),
            outline = AppOutlineLight,
            outlineVariant = AppOutlineVariantLight,
            error = AppErrorLight,
            onError = AppOnErrorLight,
            errorContainer = AppErrorContainerLight,
            onErrorContainer = AppOnErrorContainerLight,
        )
    }
}

private fun parseThemeColor(hex: String, fallback: Color): Color {
    return runCatching { Color(parseColor(hex)) }.getOrDefault(fallback)
}

private fun blendTowards(from: Color, to: Color, amount: Float): Color {
    return lerp(from, to, amount.coerceIn(0f, 1f))
}

private fun tintSurface(base: Color, tint: Color, darkTheme: Boolean): Color {
    val amount = if (darkTheme) 0.12f else 0.08f
    return lerp(base, tint, amount)
}

private fun colorOn(background: Color): Color {
    return if (background.luminance() > 0.45f) Color(0xFF10201A) else Color.White
}
