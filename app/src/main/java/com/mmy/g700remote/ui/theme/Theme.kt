package com.mmy.g700remote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mmy.g700remote.data.AppTheme

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F5E4A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EFE5),
    onPrimaryContainer = Color(0xFF0E2D22),
    secondary = Color(0xFF56605A),
    onSecondary = Color.White,
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFE0E4DF),
    onSurfaceVariant = Color(0xFF424A45),
    background = Color(0xFFF7F8F6),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB782),
    onPrimary = Color(0xFF4D2500),
    primaryContainer = Color(0xFF6E3700),
    onPrimaryContainer = Color(0xFFFFDCC6),
    secondary = Color(0xFF8FD7FF),
    onSecondary = Color(0xFF00344B),
    secondaryContainer = Color(0xFF0A4B67),
    onSecondaryContainer = Color(0xFFC8EAFF),
    tertiary = Color(0xFFB6CCFF),
    onTertiary = Color(0xFF18315F),
    tertiaryContainer = Color(0xFF2F4777),
    onTertiaryContainer = Color(0xFFDCE5FF),
    surface = Color(0xFF111315),
    onSurface = Color(0xFFE6E2DD),
    surfaceVariant = Color(0xFF4C463F),
    onSurfaceVariant = Color(0xFFD2C5B8),
    background = Color(0xFF0B0D10),
    error = Color(0xFFFFB4AB),
)

private val HorizonLightColors = lightColorScheme(
    primary = Color(0xFF9A4D1F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9C2),
    onPrimaryContainer = Color(0xFF321300),
    secondary = Color(0xFF58646F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE4EF),
    onSecondaryContainer = Color(0xFF111D28),
    tertiary = Color(0xFF256274),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC9EAF5),
    onTertiaryContainer = Color(0xFF001F29),
    surface = Color(0xFFFFFBF6),
    onSurface = Color(0xFF1E1B17),
    surfaceVariant = Color(0xFFECE0D2),
    onSurfaceVariant = Color(0xFF50453A),
    background = Color(0xFFF8F2EA),
    error = Color(0xFFB3261E),
)

private val HorizonDarkColors = darkColorScheme(
    primary = Color(0xFFFFB688),
    onPrimary = Color(0xFF552100),
    primaryContainer = Color(0xFF773400),
    onPrimaryContainer = Color(0xFFFFD9C2),
    secondary = Color(0xFFC0CAD5),
    onSecondary = Color(0xFF29333D),
    secondaryContainer = Color(0xFF3F4954),
    onSecondaryContainer = Color(0xFFDCE4EF),
    tertiary = Color(0xFF9ED2E1),
    onTertiary = Color(0xFF003641),
    tertiaryContainer = Color(0xFF004E5F),
    onTertiaryContainer = Color(0xFFC9EAF5),
    surface = Color(0xFF171411),
    onSurface = Color(0xFFECE0D7),
    surfaceVariant = Color(0xFF50453A),
    onSurfaceVariant = Color(0xFFD5C4B3),
    background = Color(0xFF11100E),
    error = Color(0xFFFFB4AB),
)

private val PastelLightColors = lightColorScheme(
    primary = Color(0xFF4768A8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001A43),
    secondary = Color(0xFF526263),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E6E7),
    onSecondaryContainer = Color(0xFF0F1F20),
    tertiary = Color(0xFF8A4F73),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8EB),
    onTertiaryContainer = Color(0xFF38102C),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE1E3EC),
    onSurfaceVariant = Color(0xFF44474F),
    background = Color(0xFFF8FAFF),
    error = Color(0xFFB3261E),
)

private val PastelDarkColors = darkColorScheme(
    primary = Color(0xFFB1C5FF),
    onPrimary = Color(0xFF15315F),
    primaryContainer = Color(0xFF2E4E8E),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFB9CACB),
    onSecondary = Color(0xFF243334),
    secondaryContainer = Color(0xFF3A4A4B),
    onSecondaryContainer = Color(0xFFD5E6E7),
    tertiary = Color(0xFFF9B8D9),
    onTertiary = Color(0xFF522743),
    tertiaryContainer = Color(0xFF6E385A),
    onTertiaryContainer = Color(0xFFFFD8EB),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC5C6D0),
    background = Color(0xFF0D1117),
    error = Color(0xFFFFB4AB),
)

@Composable
fun G700RemoteTheme(
    appTheme: AppTheme = AppTheme.Minimal,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = appTheme.colorScheme(darkTheme),
        content = content,
    )
}

private fun AppTheme.colorScheme(darkTheme: Boolean): ColorScheme =
    when (this) {
        AppTheme.Minimal -> if (darkTheme) DarkColors else LightColors
        AppTheme.G700Horizon -> if (darkTheme) HorizonDarkColors else HorizonLightColors
        AppTheme.ModernPastel -> if (darkTheme) PastelDarkColors else PastelLightColors
    }
