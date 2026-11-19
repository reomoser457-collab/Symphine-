package com.symphine.gamespace.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.symphine.gamespace.core.model.ThemePreset
import com.symphine.gamespace.core.model.UiSettingsState

object SymphineDefaults {
    val Background = Color(0xFF08090D)
    val Surface = Color(0xFF111319)
    val SurfaceElevated = Color(0xFF191B23)
    val LavenderGray = Color(0xFFB8B4C9)
    val SoftSilver = Color(0xFFD5D5DF)
    val PurpleAccent = Color(0xFFA78BFA)
    val SecondaryAccent = Color(0xFFC4B5FD)
    val MutedText = Color(0xFF8D909C)
    val Border = Color(0xFF292B35)

    val StatusEmerald = Color(0xFF34D399)
    val StatusAmber = Color(0xFFFBBF24)
    val StatusRose = Color(0xFFF87171)
    val StatusSky = Color(0xFF60A5FA)
}

val SpaceGroteskFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.SemiBold))
val PlusJakartaSansFamily = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val JetBrainsMonoFamily = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

data class SymphineExtendedColors(
    val background: Color = SymphineDefaults.Background,
    val surface: Color = SymphineDefaults.Surface,
    val surfaceElevated: Color = SymphineDefaults.SurfaceElevated,
    val lavenderGray: Color = SymphineDefaults.LavenderGray,
    val softSilver: Color = SymphineDefaults.SoftSilver,
    val purpleAccent: Color = SymphineDefaults.PurpleAccent,
    val secondaryAccent: Color = SymphineDefaults.SecondaryAccent,
    val mutedText: Color = SymphineDefaults.MutedText,
    val border: Color = SymphineDefaults.Border,
    val cornerRadius: Dp = 20.dp,
    val glassOpacity: Float = 0.82f,
    val glassGlow: Boolean = true,
    val reducedMotion: Boolean = false
)

val LocalSymphineColors = staticCompositionLocalOf { SymphineExtendedColors() }
val LocalUiSettings = staticCompositionLocalOf { UiSettingsState() }

fun parseHexColorOrNull(hex: String): Color? {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return null
    val parsed = clean.toLongOrNull(16) ?: return null
    val argb = if (clean.length == 6) (0xFF000000L or parsed) else parsed
    return Color(argb)
}

private data class ThemePalette(
    val bg: Color,
    val surf: Color,
    val surfElev: Color,
    val primary: Color,
    val secondary: Color,
    val border: Color,
    val text: Color
)

@Composable
fun SymphineTheme(
    uiSettings: UiSettingsState = UiSettingsState(),
    content: @Composable () -> Unit
) {
    val palette = when (uiSettings.themePreset) {
        ThemePreset.SYMPHINE_AMOLED -> ThemePalette(
            SymphineDefaults.Background,
            SymphineDefaults.Surface,
            SymphineDefaults.SurfaceElevated,
            SymphineDefaults.PurpleAccent,
            SymphineDefaults.SecondaryAccent,
            SymphineDefaults.Border,
            SymphineDefaults.SoftSilver
        )
        ThemePreset.MIDNIGHT_SILVER -> ThemePalette(
            Color(0xFF050608),
            Color(0xFF0E1117),
            Color(0xFF171B24),
            Color(0xFFE2E8F0),
            Color(0xFF94A3B8),
            Color(0xFF28303D),
            Color(0xFFF1F5F9)
        )
        ThemePreset.OBSIDIAN_PURPLE -> ThemePalette(
            Color(0xFF09060E),
            Color(0xFF140F1F),
            Color(0xFF1E1630),
            Color(0xFFC084FC),
            Color(0xFFA855F7),
            Color(0xFF34274E),
            Color(0xFFEDE9FE)
        )
        ThemePreset.CUSTOM -> ThemePalette(
            parseHexColorOrNull(uiSettings.backgroundHex) ?: SymphineDefaults.Background,
            parseHexColorOrNull(uiSettings.surfaceHex) ?: SymphineDefaults.Surface,
            parseHexColorOrNull(uiSettings.surfaceElevatedHex) ?: SymphineDefaults.SurfaceElevated,
            parseHexColorOrNull(uiSettings.primaryHex) ?: SymphineDefaults.PurpleAccent,
            parseHexColorOrNull(uiSettings.secondaryHex) ?: SymphineDefaults.SecondaryAccent,
            parseHexColorOrNull(uiSettings.borderHex) ?: SymphineDefaults.Border,
            parseHexColorOrNull(uiSettings.textHex) ?: SymphineDefaults.SoftSilver
        )
    }
    val (bg, surf, surfElev, primary, secondary, border, text) = palette

    val radius = uiSettings.cornerRadiusDp.coerceIn(0, 32).dp
    val scale = uiSettings.textScale.multiplier

    val colorScheme = darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF08090D),
        primaryContainer = surfElev,
        onPrimaryContainer = secondary,
        secondary = secondary,
        onSecondary = Color(0xFF08090D),
        secondaryContainer = surf,
        onSecondaryContainer = text,
        tertiary = SymphineDefaults.LavenderGray,
        background = bg,
        onBackground = text,
        surface = surf,
        onSurface = text,
        surfaceVariant = surfElev,
        onSurfaceVariant = SymphineDefaults.MutedText,
        outline = border,
        error = SymphineDefaults.StatusRose
    )

    val typography = Typography(
        displaySmall = TextStyle(
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28f * scale).sp,
            lineHeight = (34f * scale).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (22f * scale).sp,
            lineHeight = (28f * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18f * scale).sp,
            lineHeight = (24f * scale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (15f * scale).sp,
            lineHeight = (20f * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontSize = (15f * scale).sp,
            lineHeight = (22f * scale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontSize = (13f * scale).sp,
            lineHeight = (19f * scale).sp
        ),
        bodySmall = TextStyle(
            fontFamily = PlusJakartaSansFamily,
            fontSize = (11.5f * scale).sp,
            lineHeight = (16f * scale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * scale).sp,
            lineHeight = (15f * scale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontSize = (10f * scale).sp,
            lineHeight = (13f * scale).sp
        )
    )

    val shapes = Shapes(
        small = RoundedCornerShape((radius.value * 0.6f).dp),
        medium = RoundedCornerShape(radius),
        large = RoundedCornerShape((radius.value * 1.2f).coerceAtMost(32f).dp)
    )

    val extended = SymphineExtendedColors(
        background = bg,
        surface = surf,
        surfaceElevated = surfElev,
        lavenderGray = SymphineDefaults.LavenderGray,
        softSilver = text,
        purpleAccent = primary,
        secondaryAccent = secondary,
        mutedText = SymphineDefaults.MutedText,
        border = border,
        cornerRadius = radius,
        glassOpacity = uiSettings.glassSurfaceOpacity,
        glassGlow = uiSettings.glassGlowEnabled,
        reducedMotion = uiSettings.reducedMotion
    )

    CompositionLocalProvider(
        LocalSymphineColors provides extended,
        LocalUiSettings provides uiSettings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
