package com.mmy.g700remote.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mmy.g700remote.data.AppColorMode
import com.mmy.g700remote.data.AppTheme

private val TransparentTint = Color.Transparent

private val HorizonLightColors = lightColorScheme(
    primary = Color(0xFF9A4A18),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD8BD),
    onPrimaryContainer = Color(0xFF321300),
    secondary = Color(0xFF42606F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE6F4),
    onSecondaryContainer = Color(0xFF001F2A),
    tertiary = Color(0xFF6E5F32),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF9E3A9),
    onTertiaryContainer = Color(0xFF241A00),
    surface = Color(0xFFFFFBF6),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFEFE0D1),
    onSurfaceVariant = Color(0xFF504539),
    background = Color(0xFFFAF3EA),
    outline = Color(0xFF817568),
    outlineVariant = Color(0xFFD3C4B5),
    surfaceTint = TransparentTint,
    error = Color(0xFFB3261E),
)

private val HorizonDarkColors = darkColorScheme(
    primary = Color(0xFFFFB582),
    onPrimary = Color(0xFF542100),
    primaryContainer = Color(0xFF773300),
    onPrimaryContainer = Color(0xFFFFD8BD),
    secondary = Color(0xFFA6D0E3),
    onSecondary = Color(0xFF073542),
    secondaryContainer = Color(0xFF264D5C),
    onSecondaryContainer = Color(0xFFCDE6F4),
    tertiary = Color(0xFFDCC788),
    onTertiary = Color(0xFF3D2F00),
    tertiaryContainer = Color(0xFF554614),
    onTertiaryContainer = Color(0xFFF9E3A9),
    surface = Color(0xFF15120F),
    onSurface = Color(0xFFECE0D6),
    surfaceVariant = Color(0xFF504539),
    onSurfaceVariant = Color(0xFFD3C4B5),
    background = Color(0xFF0F0D0B),
    outline = Color(0xFF9D8F80),
    outlineVariant = Color(0xFF504539),
    surfaceTint = TransparentTint,
    error = Color(0xFFFFB4AB),
)

private val HimalayaLightColors = lightColorScheme(
    primary = Color(0xFF285B7A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE6FF),
    onPrimaryContainer = Color(0xFF001D31),
    secondary = Color(0xFF59616A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE5EF),
    onSecondaryContainer = Color(0xFF161D24),
    tertiary = Color(0xFF6D5978),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4D9FF),
    onTertiaryContainer = Color(0xFF271032),
    surface = Color(0xFFFBFCFF),
    onSurface = Color(0xFF181C20),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41484F),
    background = Color(0xFFF3F7FB),
    outline = Color(0xFF71787F),
    outlineVariant = Color(0xFFC1C7CF),
    surfaceTint = TransparentTint,
    error = Color(0xFFB3261E),
)

private val HimalayaDarkColors = darkColorScheme(
    primary = Color(0xFF96CDF7),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF0A4A68),
    onPrimaryContainer = Color(0xFFCDE6FF),
    secondary = Color(0xFFC1C9D3),
    onSecondary = Color(0xFF2B3239),
    secondaryContainer = Color(0xFF414850),
    onSecondaryContainer = Color(0xFFDDE5EF),
    tertiary = Color(0xFFD8BDE5),
    onTertiary = Color(0xFF3D2848),
    tertiaryContainer = Color(0xFF543F5F),
    onTertiaryContainer = Color(0xFFF4D9FF),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE1E2E7),
    surfaceVariant = Color(0xFF41484F),
    onSurfaceVariant = Color(0xFFC1C7CF),
    background = Color(0xFF0B0F13),
    outline = Color(0xFF8B9299),
    outlineVariant = Color(0xFF41484F),
    surfaceTint = TransparentTint,
    error = Color(0xFFFFB4AB),
)

private val NomadLightColors = lightColorScheme(
    primary = Color(0xFF7F4A1D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF2C1600),
    secondary = Color(0xFF6C5D4E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6DFC8),
    onSecondaryContainer = Color(0xFF25190D),
    tertiary = Color(0xFF536340),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6E8BF),
    onTertiaryContainer = Color(0xFF121F04),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF201A15),
    surfaceVariant = Color(0xFFECDDD0),
    onSurfaceVariant = Color(0xFF4E453C),
    background = Color(0xFFFBF2E8),
    outline = Color(0xFF817568),
    outlineVariant = Color(0xFFD3C4B6),
    surfaceTint = TransparentTint,
    error = Color(0xFFB3261E),
)

private val NomadDarkColors = darkColorScheme(
    primary = Color(0xFFFFB77D),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF633A12),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFD9C3AD),
    onSecondary = Color(0xFF3C2D20),
    secondaryContainer = Color(0xFF544334),
    onSecondaryContainer = Color(0xFFF6DFC8),
    tertiary = Color(0xFFBBCBA5),
    onTertiary = Color(0xFF263516),
    tertiaryContainer = Color(0xFF3C4B2B),
    onTertiaryContainer = Color(0xFFD6E8BF),
    surface = Color(0xFF15120F),
    onSurface = Color(0xFFECE0D7),
    surfaceVariant = Color(0xFF4E453C),
    onSurfaceVariant = Color(0xFFD3C4B6),
    background = Color(0xFF0F0D0A),
    outline = Color(0xFF9C8F81),
    outlineVariant = Color(0xFF4E453C),
    surfaceTint = TransparentTint,
    error = Color(0xFFFFB4AB),
)

private val PastelLightColors = lightColorScheme(
    primary = Color(0xFF4768A8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001A43),
    secondary = Color(0xFF48645F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCBE9DF),
    onSecondaryContainer = Color(0xFF06201B),
    tertiary = Color(0xFF85515D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9DF),
    onTertiaryContainer = Color(0xFF371018),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE1E3EC),
    onSurfaceVariant = Color(0xFF44474F),
    background = Color(0xFFF8FAFF),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    surfaceTint = TransparentTint,
    error = Color(0xFFB3261E),
)

private val PastelDarkColors = darkColorScheme(
    primary = Color(0xFFB1C5FF),
    onPrimary = Color(0xFF15315F),
    primaryContainer = Color(0xFF2E4E8E),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFAFCFC5),
    onSecondary = Color(0xFF1B3530),
    secondaryContainer = Color(0xFF324B46),
    onSecondaryContainer = Color(0xFFCBE9DF),
    tertiary = Color(0xFFF6B8C4),
    onTertiary = Color(0xFF4F252F),
    tertiaryContainer = Color(0xFF6A3B45),
    onTertiaryContainer = Color(0xFFFFD9DF),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC5C6D0),
    background = Color(0xFF0D1117),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F),
    surfaceTint = TransparentTint,
    error = Color(0xFFFFB4AB),
)

private val MinimalLightColors = lightColorScheme(
    primary = Color(0xFF245E4B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1EFE2),
    onPrimaryContainer = Color(0xFF072117),
    secondary = Color(0xFF56605A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9E7DE),
    onSecondaryContainer = Color(0xFF141D18),
    tertiary = Color(0xFF3E6374),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC2E8FC),
    onTertiaryContainer = Color(0xFF001F2A),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFE0E4DF),
    onSurfaceVariant = Color(0xFF424A45),
    background = Color(0xFFF7F8F6),
    outline = Color(0xFF727970),
    outlineVariant = Color(0xFFC0C8BF),
    surfaceTint = TransparentTint,
    error = Color(0xFFB3261E),
)

private val MinimalDarkColors = darkColorScheme(
    primary = Color(0xFF9AD8BF),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF0C503D),
    onPrimaryContainer = Color(0xFFD1EFE2),
    secondary = Color(0xFFBDCBBF),
    onSecondary = Color(0xFF27332C),
    secondaryContainer = Color(0xFF3D4A41),
    onSecondaryContainer = Color(0xFFD9E7DE),
    tertiary = Color(0xFFA7CCDE),
    onTertiary = Color(0xFF0B3545),
    tertiaryContainer = Color(0xFF284C5C),
    onTertiaryContainer = Color(0xFFC2E8FC),
    surface = Color(0xFF111315),
    onSurface = Color(0xFFE4E3DF),
    surfaceVariant = Color(0xFF424A45),
    onSurfaceVariant = Color(0xFFC0C8BF),
    background = Color(0xFF0B0D10),
    outline = Color(0xFF8A938A),
    outlineVariant = Color(0xFF424A45),
    surfaceTint = TransparentTint,
    error = Color(0xFFFFB4AB),
)

@Composable
fun G700RemoteTheme(
    appTheme: AppTheme = AppTheme.G700Horizon,
    colorMode: AppColorMode = AppColorMode.Dark,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = appTheme.colorScheme(colorMode == AppColorMode.Dark),
        content = content,
    )
}

private fun AppTheme.colorScheme(darkTheme: Boolean): ColorScheme =
    when (this) {
        AppTheme.G700Horizon -> if (darkTheme) HorizonDarkColors else HorizonLightColors
        AppTheme.HimalayaSlate -> if (darkTheme) HimalayaDarkColors else HimalayaLightColors
        AppTheme.NomadStone -> if (darkTheme) NomadDarkColors else NomadLightColors
        AppTheme.ModernPastel -> if (darkTheme) PastelDarkColors else PastelLightColors
        AppTheme.Minimal -> if (darkTheme) MinimalDarkColors else MinimalLightColors
    }
