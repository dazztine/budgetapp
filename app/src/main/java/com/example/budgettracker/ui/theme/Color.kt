package com.example.budgettracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

// ============================================================================
// Kwago Locked Brand Color Palette
// ============================================================================

/**
 * Primary Brand: Midnight Navy (#1A1B41)
 * Usage: Headers, dark-mode surfaces, brand identity elements.
 */
val MidnightNavy = Color(0xFF1A1B41)

/**
 * Action (Interactive only): Amber Glow (#F4B942)
 * Usage: Floating "+" button, primary CTA buttons, active nav/tab indicator.
 * STRICT RULE: Reserved exclusively for interactive/tappable elements.
 * Never use for non-interactive decorative backgrounds.
 */
val AmberGlow = Color(0xFFF4B942)

/**
 * Soft Tint: Beige (#F7D88F)
 * Usage: Decorative backgrounds only (e.g. light-mode icon bg, badges).
 * STRICT RULE: Reserved exclusively for non-interactive backgrounds.
 * Never use on tappable buttons or interactive elements.
 */
val Beige = Color(0xFFF7D88F)
val KwagoBeige = Beige

/**
 * Background (Light): Zinc Light (#FAFAFA)
 * Usage: App background and card surfaces, light mode.
 */
val ZincLight = Color(0xFFFAFAFA)

/**
 * Background (Dark): Zinc Dark (#09090B)
 * Usage: App background and card surfaces, dark mode.
 */
val ZincDark = Color(0xFF09090B)

/**
 * Positive/Success: Muted Sage (#7FB88F)
 * Usage: Income entries, positive balances, success states.
 */
val MutedSage = Color(0xFF7FB88F)

/**
 * Negative/Alert: Muted Coral (#F87171)
 * Usage: Expense entries, negative balances, error states.
 */
val MutedCoral = Color(0xFFF87171)


// ============================================================================
// Backward-Compatibility Grayscale & Accent Palette (Zinc / Red / Green / Orange)
// ============================================================================
val Zinc950 = Color(0xFF09090B)
val Zinc900 = Color(0xFF18181B)
val Zinc800 = Color(0xFF27272A)
val Zinc700 = Color(0xFF3F3F46)
val Zinc600 = Color(0xFF52525B)
val Zinc500 = Color(0xFF71717A)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc100 = Color(0xFFF4F4F5)
val Zinc50 = Color(0xFFFAFAFA)

val Red500 = Color(0xFFEF4444)
val Red400 = Color(0xFFF87171)
val Green500 = Color(0xFF22C55E)
val Green400 = Color(0xFF4ADE80)
val Orange500 = Color(0xFFF97316)
val Orange400 = Color(0xFFFB923C)


// ============================================================================
// Kwago Semantic Custom Colors & Design Token Extensions
// ============================================================================

@Immutable
data class KwagoCustomColors(
    val primaryBrand: Color = MidnightNavy,
    val action: Color = AmberGlow,
    val softTint: Color = Beige,
    val backgroundLight: Color = ZincLight,
    val backgroundDark: Color = ZincDark,
    val income: Color = MutedSage,
    val expense: Color = MutedCoral,
)

val LocalKwagoColors = staticCompositionLocalOf { KwagoCustomColors() }

/**
 * Direct access object for Kwago design tokens:
 * e.g. KwagoColors.income, KwagoColors.expense, KwagoColors.amberGlow
 */
object KwagoColors {
    val midnightNavy: Color = MidnightNavy
    val amberGlow: Color = AmberGlow
    val beige: Color = Beige
    val zincLight: Color = ZincLight
    val zincDark: Color = ZincDark
    val mutedSage: Color = MutedSage
    val mutedCoral: Color = MutedCoral

    // Semantic roles
    val income: Color get() = MutedSage
    val expense: Color get() = MutedCoral
    val brand: Color get() = MidnightNavy
    val action: Color get() = AmberGlow
    val softTint: Color get() = Beige
}

/**
 * Access custom Kwago colors via Compose MaterialTheme:
 * MaterialTheme.kwagoColors.income
 */
val MaterialTheme.kwagoColors: KwagoCustomColors
    @Composable
    @ReadOnlyComposable
    get() = LocalKwagoColors.current

/**
 * Access custom Kwago colors via ColorScheme:
 * MaterialTheme.colorScheme.income
 */
val ColorScheme.income: Color get() = MutedSage
val ColorScheme.expense: Color get() = MutedCoral
val ColorScheme.softTint: Color get() = Beige
val ColorScheme.action: Color get() = AmberGlow
val ColorScheme.brand: Color get() = MidnightNavy