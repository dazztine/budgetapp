package com.example.budgettracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Kwago Light Color Scheme
 * - Primary (Action): Amber Glow (#F4B942) — interactive only
 * - Secondary (Brand): Midnight Navy (#1A1B41) — headers, brand identity
 * - Secondary Container (Soft Tint): Beige (#F7D88F) — decorative backgrounds only
 * - Background: Zinc Light (#FAFAFA)
 * - Error: Muted Coral (#F87171)
 */
val KwagoLightColorScheme = lightColorScheme(
    primary = AmberGlow,
    onPrimary = MidnightNavy,
    primaryContainer = AmberGlow.copy(alpha = 0.15f),
    onPrimaryContainer = MidnightNavy,
    secondary = MidnightNavy,
    onSecondary = Color.White,
    secondaryContainer = Zinc100,
    onSecondaryContainer = MidnightNavy,
    tertiary = MutedSage,
    onTertiary = MidnightNavy,
    tertiaryContainer = MutedSage.copy(alpha = 0.2f),
    onTertiaryContainer = MidnightNavy,
    background = ZincLight,
    onBackground = MidnightNavy,
    surface = Color.White,
    onSurface = MidnightNavy,
    surfaceVariant = Zinc100,
    onSurfaceVariant = Zinc600,
    outline = Zinc300,
    outlineVariant = Zinc200,
    error = MutedCoral,
    onError = Color.White,
    errorContainer = MutedCoral.copy(alpha = 0.15f),
    onErrorContainer = Zinc900
)

/**
 * Kwago Dark Color Scheme
 * - Neutral charcoal/gray tones (shadcn/ui Zinc dark palette)
 * - Primary (Action): Amber Glow (#F4B942) — interactive only
 * - Secondary: Zinc400 (#A1A1AA)
 * - Secondary Container: Zinc800 (#27272A)
 * - Background: Zinc Dark (#09090B)
 * - Surface: Zinc900 (#18181B) — flat neutral charcoal cards
 * - Error: Muted Coral (#F87171)
 */
val KwagoDarkColorScheme = darkColorScheme(
    primary = AmberGlow,
    onPrimary = Zinc950,
    primaryContainer = AmberGlow.copy(alpha = 0.2f),
    onPrimaryContainer = AmberGlow,
    secondary = Zinc400,
    onSecondary = Zinc950,
    secondaryContainer = Zinc800,
    onSecondaryContainer = Zinc200,
    tertiary = MutedSage,
    onTertiary = ZincDark,
    tertiaryContainer = MutedSage.copy(alpha = 0.2f),
    onTertiaryContainer = MutedSage,
    background = ZincDark,
    onBackground = Zinc50,
    surface = Zinc900,
    onSurface = Zinc50,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc400,
    outline = Zinc700,
    outlineVariant = Zinc800,
    error = MutedCoral,
    onError = ZincDark,
    errorContainer = MutedCoral.copy(alpha = 0.2f),
    onErrorContainer = MutedCoral
)

enum class ThemeSetting {
    FOLLOW_DEVICE,
    LIGHT,
    DARK
}

/**
 * Kwago Design System Theme wrapper
 */
@Composable
fun KwagoTheme(
    themeSetting: ThemeSetting = ThemeSetting.LIGHT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        ThemeSetting.FOLLOW_DEVICE -> isSystemInDarkTheme()
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> KwagoDarkColorScheme
        else -> KwagoLightColorScheme
    }

    val kwagoCustomColors = KwagoCustomColors(
        primaryBrand = MidnightNavy,
        action = AmberGlow,
        softTint = Beige,
        backgroundLight = ZincLight,
        backgroundDark = ZincDark,
        income = MutedSage,
        expense = MutedCoral
    )

    CompositionLocalProvider(LocalKwagoColors provides kwagoCustomColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = Shapes,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Backward-compatible alias for KwagoTheme
 */
@Composable
fun BudgetTrackerTheme(
    themeSetting: ThemeSetting = ThemeSetting.LIGHT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    KwagoTheme(
        themeSetting = themeSetting,
        dynamicColor = dynamicColor,
        content = content
    )
}