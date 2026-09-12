package com.example.budgettracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Zinc800,
    onPrimary = Color.White,
    primaryContainer = Zinc100,
    onPrimaryContainer = Zinc900,
    secondary = Zinc500,
    onSecondary = Color.White,
    secondaryContainer = Zinc100,
    onSecondaryContainer = Zinc800,
    background = Color.White,
    onBackground = Zinc900,
    surface = Color.White,
    onSurface = Zinc900,
    surfaceVariant = Zinc100,
    onSurfaceVariant = Zinc600,
    outline = Zinc200,
    outlineVariant = Zinc300,
    error = Red500,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Zinc200,
    onPrimary = Zinc900,
    primaryContainer = Zinc800,
    onPrimaryContainer = Color.White,
    secondary = Zinc400,
    onSecondary = Zinc900,
    secondaryContainer = Zinc800,
    onSecondaryContainer = Zinc200,
    background = Zinc900,
    onBackground = Color.White,
    surface = Zinc800,
    onSurface = Color.White,
    surfaceVariant = Zinc700,
    onSurfaceVariant = Zinc400,
    outline = Zinc700,
    outlineVariant = Zinc600,
    error = Red400,
    onError = Zinc900
)

enum class ThemeSetting {
    FOLLOW_DEVICE,
    LIGHT,
    DARK
}

@Composable
fun BudgetTrackerTheme(
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
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        typography = Typography,
        content = content
    )
}