package com.pesalytics.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import com.pesalytics.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val SpaceGroteskFont = GoogleFont("Space Grotesk")

val SpaceGroteskFontFamily = FontFamily(
    Font(googleFont = SpaceGroteskFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = SpaceGroteskFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = SpaceGroteskFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = SpaceGroteskFont, fontProvider = provider, weight = FontWeight.Bold)
)

val defaultTypography = Typography()

// Set of Material typography styles to start with
val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = SpaceGroteskFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = SpaceGroteskFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = SpaceGroteskFontFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = SpaceGroteskFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = SpaceGroteskFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = SpaceGroteskFontFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = SpaceGroteskFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = SpaceGroteskFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = SpaceGroteskFontFamily),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = SpaceGroteskFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = SpaceGroteskFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = SpaceGroteskFontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = SpaceGroteskFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = SpaceGroteskFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = SpaceGroteskFontFamily)
)
