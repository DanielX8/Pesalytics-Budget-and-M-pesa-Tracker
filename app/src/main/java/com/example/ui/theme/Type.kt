package com.pesalytics.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.pesalytics.R

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, weight = FontWeight.Normal),
    Font(R.font.poppins_medium, weight = FontWeight.Medium),
    Font(R.font.poppins_semibold, weight = FontWeight.SemiBold),
    Font(R.font.poppins_bold, weight = FontWeight.Bold),
)

val defaultTypography = Typography()

val Typography = Typography(
    displayLarge   = defaultTypography.displayLarge.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold),
    displayMedium  = defaultTypography.displayMedium.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold),
    displaySmall   = defaultTypography.displaySmall.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold),
    headlineLarge  = defaultTypography.headlineLarge.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Bold),
    headlineSmall  = defaultTypography.headlineSmall.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold),
    titleLarge     = defaultTypography.titleLarge.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium    = defaultTypography.titleMedium.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall     = defaultTypography.titleSmall.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge      = defaultTypography.bodyLarge.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Normal),
    bodyMedium     = defaultTypography.bodyMedium.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Normal),
    bodySmall      = defaultTypography.bodySmall.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Normal),
    labelLarge     = defaultTypography.labelLarge.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold),
    labelMedium    = defaultTypography.labelMedium.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium),
    labelSmall     = defaultTypography.labelSmall.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Normal),
)
