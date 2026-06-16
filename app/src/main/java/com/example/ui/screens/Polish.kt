package com.pesalytics.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pesalytics.ui.theme.AccentGreenDark
import com.pesalytics.ui.theme.AccentGreenLight

/**
 * The hero-card mid-green (present in the Dashboard balance gradient in both light and dark
 * modes). Readable on both AMOLED black (~5:1) and the light surface (~3.8:1), so it works as
 * an accent on the Settings/Subscription screens — unlike the deep forest [AccentGreenDark],
 * which is reserved for the Goals/Budget screens and is near-invisible on dark backgrounds.
 */
val HeroGreen = Color(0xFF348C55)

/**
 * Shared visual-polish + motion helpers so secondary screens (Settings, Subscription,
 * Budget) match the premium language established on the Dashboard / Analytics screens:
 * colored glow shadows, the brand hero gradient, and spring press feedback.
 */

/** Tappable with a spring scale-down on press — the tactile feel used across the app. */
@Composable
fun Modifier.clickableScale(pressedScale: Float = 0.97f, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "press-scale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
}

/** The same hero gradient used on the Dashboard balance card (light vs dark aware). */
@Composable
fun rememberBrandGradient(): Brush {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val start = if (isLight) Color(0xFF55D687) else Color(0xFF348C55)
    val end = if (isLight) Color(0xFF348C55) else Color(0xFF1A4D2E)
    return Brush.linearGradient(listOf(start, end))
}

/** Colored glow shadow — "the single biggest visual upgrade" from the Dashboard hero/quick-nav. */
fun Modifier.brandGlow(elevation: Dp = 20.dp, radius: Dp = 24.dp): Modifier = this.shadow(
    elevation = elevation,
    shape = RoundedCornerShape(radius),
    spotColor = AccentGreenLight.copy(alpha = 0.40f),
    ambientColor = AccentGreenDark.copy(alpha = 0.25f)
)

/** Soft neutral elevation matching the Dashboard recent-activity cards. */
fun Modifier.softCard(radius: Dp = 16.dp): Modifier = this.shadow(
    elevation = 2.dp,
    shape = RoundedCornerShape(radius)
)
