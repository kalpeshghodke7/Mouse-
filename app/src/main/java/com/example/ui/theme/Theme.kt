package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
  primary = ElectricBlue,
  secondary = NeonCyan,
  tertiary = NeonRed,
  background = Black,
  surface = DarkSurface,
  surfaceVariant = DarkSurfaceVariant,
  onPrimary = Black,
  onSecondary = Black,
  onTertiary = TextPrimary,
  onBackground = TextPrimary,
  onSurface = TextPrimary,
  outline = DarkBorder
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}

