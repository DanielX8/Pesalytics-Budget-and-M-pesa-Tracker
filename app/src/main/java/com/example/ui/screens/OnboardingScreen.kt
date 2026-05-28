package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentGreenLight
import kotlinx.coroutines.launch

sealed class OnboardingContent {
    data class Standard(
        val title: String,
        val description: String,
        val icon: ImageVector? = null,
        val bullets: List<String> = emptyList()
    ) : OnboardingContent()
    
    object ProfileSetup : OnboardingContent()
}

val onboardingSlides = listOf(
    OnboardingContent.Standard(
        title = "Welcome to PesaSense",
        description = "Your automated, privacy-first financial companion that transforms offline M-PESA alerts into powerful insights.",
        bullets = listOf("Secure data access setup", "Local SMS log synchronization", "Budget target configuration")
    ),
    OnboardingContent.ProfileSetup,
    OnboardingContent.Standard(
        title = "Your Privacy Matters",
        description = "PesaSense operates entirely offline. Your transaction logs are computed directly on your phone hardware—zero cloud tracking, zero data sharing, absolute financial anonymity.",
        icon = Icons.Rounded.Security,
        bullets = listOf(
            "Only transaction messages are processed",
            "All data stays on your device",
            "No personal messages are read",
            "You can revoke access anytime in Settings"
        )
    ),
    OnboardingContent.Standard(
        title = "Intelligent Budget Mapping",
        description = "Automatically isolates hidden transaction costs, maps out tricky Fuliza overdraft lines, and routes your spending habits instantly into clean visual categories.",
        icon = Icons.Rounded.PieChart
    ),
    OnboardingContent.Standard(
        title = "Synchronize Your Ledger",
        description = "To automatically capture incoming alerts and map your historical budget trends in real-time, PesaSense requires local device permission to look up message patterns.",
        icon = Icons.Rounded.Sync
    )
)

val avatarIcons = listOf(
    Icons.Rounded.Person,
    Icons.Rounded.Face,
    Icons.Rounded.SentimentSatisfied,
    Icons.Rounded.CrueltyFree,
    Icons.Rounded.Pets
)

val funNicknames = listOf("Budget Boss", "Finance Ninja", "Coin Master", "Ledger Lord", "Saver Supreme")

@Composable
fun AnimatedLogo() {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    
    val offsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 60.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logo_y"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "logo_alpha"
    )
    
    Image(
        painter = painterResource(id = R.drawable.header_logo),
        contentDescription = "PesaSense Logo",
        modifier = Modifier
            .size(80.dp)
            .offset(y = offsetY)
            .alpha(alpha)
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: PesaViewModel,
    onNavigateNext: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { onboardingSlides.size })
    
    var name by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val finalName = if (name.isNotBlank()) name.trim() else "User"
        viewModel.completeOnboarding(finalName, selectedAvatar, context)
        onNavigateNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (val slide = onboardingSlides[page]) {
                is OnboardingContent.Standard -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp)
                            .padding(bottom = 100.dp), // space for bottom controls
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (page == 0) {
                            AnimatedLogo()
                            Spacer(modifier = Modifier.height(24.dp))
                        } else if (slide.icon != null) {
                            Icon(
                                imageVector = slide.icon,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = AccentGreenLight
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = slide.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )

                        if (slide.bullets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(32.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    slide.bullets.forEach { bullet ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = AccentGreenLight,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = bullet,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is OnboardingContent.ProfileSetup -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp)
                            .padding(bottom = 100.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "What should we call you?",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Your name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGreenLight,
                                cursorColor = AccentGreenLight
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        TextButton(
                            onClick = { name = funNicknames.random() }
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Nickname", color = AccentGreenLight, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = "Choose an avatar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            avatarIcons.forEachIndexed { index, icon ->
                                val isSelected = selectedAvatar == index
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) AccentGreenLight.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) AccentGreenLight else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedAvatar = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Avatar $index",
                                        tint = if (isSelected) AccentGreenLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicator
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(onboardingSlides.size) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    val width by animateDpAsState(targetValue = if (isSelected) 24.dp else 8.dp, label = "indicator_width")
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(if (isSelected) AccentGreenLight else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }

            // Action Button
            if (pagerState.currentPage == onboardingSlides.lastIndex) {
                Button(
                    onClick = {
                        val permissions = mutableListOf(Manifest.permission.READ_SMS)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight)
                ) {
                    Text(
                        text = "Grant Access & Finish",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            } else {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = "Next",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
