package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = DarkTextPrimary,
    primaryContainer = PrimaryIndigoVariant,
    onPrimaryContainer = DarkTextPrimary,
    secondary = SecondaryCyan,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceCard,
    onSecondaryContainer = SecondaryCyanGlow,
    tertiary = AccentEmerald,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = LightSurface,
    primaryContainer = LightSurfaceCardHover,
    onPrimaryContainer = PrimaryIndigoVariant,
    secondary = SecondaryCyan,
    onSecondary = LightSurface,
    secondaryContainer = LightSurfaceCard,
    onSecondaryContainer = PrimaryIndigoVariant,
    tertiary = AccentEmerald,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our handcrafted modern palette for consistent premium aesthetic
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
