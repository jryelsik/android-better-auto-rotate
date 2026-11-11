package com.example.reverseportrait.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OledDarkColorScheme = darkColorScheme(
  primary = primaryDark,
  primaryContainer = primaryContainerDark,
  secondary = secondaryDark,
  tertiary = tertiaryDark,
  background = Color.Black,
  surface = Color.Black,
  onBackground = Color.White,
  onSurface = Color.White
)

@Composable
fun ReversePortraitTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = OledDarkColorScheme,
    typography = Typography,
    content = content
  )
}
