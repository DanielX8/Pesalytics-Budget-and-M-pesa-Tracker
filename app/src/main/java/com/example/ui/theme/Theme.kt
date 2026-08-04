package com.pesalytics.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreenLight,
    secondary = AccentGreenDark,
    tertiary = TransferBlue,
    background = AmoledBlack,
    surface = DarkSurface1,
    surfaceVariant = DarkSurface2,
    onPrimary = AmoledBlack,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ExpenseRed,
    primaryContainer = AccentGreenDark,
    onPrimaryContainer = Color.White,
    secondaryContainer = AccentGreenDark,
    onSecondaryContainer = Color.White,
    surfaceTint = AccentGreenLight,
    surfaceContainer = DarkSurface1,
    surfaceContainerHigh = DarkSurface2,
    surfaceContainerLow = AmoledBlack,
    surfaceContainerLowest = AmoledBlack
)

private val LightColorScheme = lightColorScheme(
    primary = AccentGreenDark,
    secondary = AccentGreenLight,
    tertiary = TransferBlue,
    background = Color(0xFFF4FBF7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F2F5),
    onPrimary = Color.White,
    onSecondary = AmoledBlack,
    onTertiary = AmoledBlack,
    onBackground = AmoledBlack,
    onSurface = AmoledBlack,
    onSurfaceVariant = AmoledBlack,
    error = ExpenseRed,
    primaryContainer = AccentGreenLight,
    onPrimaryContainer = AmoledBlack,
    secondaryContainer = AccentGreenLight,
    onSecondaryContainer = AmoledBlack,
    surfaceTint = AccentGreenDark,
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0F2F5),
    surfaceContainerLow = Color(0xFFF4FBF7),
    surfaceContainerLowest = Color(0xFFFFFFFF)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamicColor to enforce Pesalytics branding
  dynamicColor: Boolean = false,
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

  val view = androidx.compose.ui.platform.LocalView.current
  if (!view.isInEditMode) {
    androidx.compose.runtime.SideEffect {
      var context = view.context
      while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) break
        context = context.baseContext
      }
      val window = (context as? android.app.Activity)?.window
      if (window != null) {
        androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
