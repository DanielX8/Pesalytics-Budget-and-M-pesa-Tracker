package com.pesalytics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import kotlin.math.sqrt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.pesalytics.model.ThemeMode
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pesalytics.ui.navigation.*
import com.pesalytics.ui.screens.*
import com.pesalytics.ui.theme.MyApplicationTheme

// Shared transition spec used for all nav transitions
private val NavEnterTransition = slideInHorizontally(
    initialOffsetX = { it / 3 },
    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))

private val NavExitTransition = slideOutHorizontally(
    targetOffsetX = { -it / 3 },
    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
) + fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))

private val NavPopEnterTransition = slideInHorizontally(
    initialOffsetX = { -it / 3 },
    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))

private val NavPopExitTransition = slideOutHorizontally(
    targetOffsetX = { it / 3 },
    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
) + fadeOut(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))

private val TabEnterTransition = fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
private val TabExitTransition = fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))

// Payee History: the "View all transactions" item in the transaction bottom sheet should feel
// like the sheet itself rising up to cover the screen in one continuous motion. Pure translateY,
// no accompanying fade — a fade on top of the slide finishes on a different timeline than the
// position animation and reads as a laggy/hazy start instead of an instant, crisp rise. The
// outgoing screen underneath doesn't animate at all; it just gets covered as the new screen
// slides over it, so there's no separate "close, then open" step.
private val PayeeHistoryEnterTransition = slideInVertically(
    initialOffsetY = { fullHeight -> fullHeight },
    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
)

private val PayeeHistoryExitTransition = ExitTransition.None

private val PayeeHistoryPopEnterTransition = EnterTransition.None

private val PayeeHistoryPopExitTransition = slideOutVertically(
    targetOffsetY = { fullHeight -> fullHeight },
    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val notifDeepLink = intent.getStringExtra("navigate_to")
        setContent {
            val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as PesalyticsApplication
            val notificationHelper = remember { com.pesalytics.notifications.NotificationHelper(app) }
            val factory = remember { PesaViewModelFactory(app.repository, notificationHelper, app.subscriptionManager) }
            val viewModel: PesaViewModel = viewModel(factory = factory)
            LaunchedEffect(notifDeepLink) {
                if (notifDeepLink != null) viewModel.pendingDeepLink.value = notifDeepLink
            }
            
            val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            
            val targetDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            // Hoisted so it survives circular-reveal recompositions
            val navController = rememberNavController()

            // ── Circular reveal on theme change ─────────────────────────────
            val revealOrigin by viewModel.revealOrigin.collectAsState()
            var previousDark by remember { mutableStateOf(targetDarkTheme) }
            var currentRenderDark by remember { mutableStateOf(targetDarkTheme) }
            var overlayColor by remember { mutableStateOf(Color.Transparent) }
            var animOrigin by remember { mutableStateOf(Offset.Zero) }
            val revealAnim = remember { Animatable(0f) }

            LaunchedEffect(targetDarkTheme) {
                if (previousDark != targetDarkTheme) {
                    // Capture old-theme background and tap origin before switching
                    overlayColor = if (previousDark) Color(0xFF000000) else Color(0xFFF8F9FA)
                    animOrigin = revealOrigin
                    revealAnim.snapTo(0f)               // start with no hole (fully covered)
                    currentRenderDark = targetDarkTheme // switch theme under the overlay
                    revealAnim.animateTo(               // expand hole to reveal new theme
                        targetValue = 1f,
                        animationSpec = tween(800, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
                    )
                    previousDark = targetDarkTheme
                } else {
                    currentRenderDark = targetDarkTheme
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                MyApplicationTheme(darkTheme = currentRenderDark) {
                    PesalyticsApp(viewModel = viewModel, navController = navController)
                }

                // Expanding reveal overlay — old theme shrinks away as hole grows from tap point
                if (revealAnim.value < 1f) {
                    val capturedColor = overlayColor
                    val capturedOrigin = animOrigin
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    ) {
                        drawRect(color = capturedColor)
                        val maxRadius = sqrt(size.width * size.width + size.height * size.height)
                        drawCircle(
                            color = Color.Black,
                            radius = maxRadius * revealAnim.value,
                            center = capturedOrigin,
                            blendMode = BlendMode.Clear
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val app = application as com.pesalytics.PesalyticsApplication
        app.subscriptionManager.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        val app = application as com.pesalytics.PesalyticsApplication
        app.subscriptionManager.disconnect()
    }
}



@Composable
fun PesalyticsApp(viewModel: PesaViewModel, navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val pendingDeepLink by viewModel.pendingDeepLink.collectAsState()
    LaunchedEffect(pendingDeepLink) {
        val target = pendingDeepLink ?: return@LaunchedEffect
        viewModel.pendingDeepLink.value = null
        when (target) {
            "budget_planner"    -> navController.navigate(BudgetPlanner)
            "bills"             -> navController.navigate(Bills)
            "goals"             -> navController.navigate(FinancialGoals)
            "all_transactions"  -> navController.navigate(AllTransactions())
            "settings"          -> navController.navigate(Settings)
        }
    }


    val isTopLevelDestination = currentDestination?.hierarchy?.any {
        it.hasRoute<Home>() ||
        it.hasRoute<Analytics>() ||
        it.hasRoute<Bills>() ||
        it.hasRoute<Settings>()
    } ?: false

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isTopLevelDestination,
                enter = slideInVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) { it } +
                        fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)),
                exit = slideOutVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) { it } +
                        fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        icon = {
                            val isSelected = currentDestination?.hierarchy?.any { it.hasRoute<Home>() } == true
                            AnimatedContent(
                                targetState = isSelected,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                                },
                                label = "NavIcon"
                            ) { selected ->
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = if (selected) R.drawable.ic_nav_home_filled else R.drawable.ic_nav_home),
                                    contentDescription = "Home",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = { Text("Home") },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<Home>() } == true,
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            indicatorColor = com.pesalytics.ui.theme.AccentGreenDark,
                            selectedIconColor = Color.White,
                            selectedTextColor = com.pesalytics.ui.theme.AccentGreenDark,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        onClick = {
                            navController.navigate(Home) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            val isSelected = currentDestination?.hierarchy?.any { it.hasRoute<Analytics>() } == true
                            AnimatedContent(
                                targetState = isSelected,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                                },
                                label = "NavIcon"
                            ) { selected ->
                                Icon(
                                    imageVector = if (selected) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                                    contentDescription = "Analytics",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = { Text("Analytics") },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<Analytics>() } == true,
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            indicatorColor = com.pesalytics.ui.theme.AccentGreenDark,
                            selectedIconColor = Color.White,
                            selectedTextColor = com.pesalytics.ui.theme.AccentGreenDark,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        onClick = {
                            navController.navigate(Analytics) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            val isSelected = currentDestination?.hierarchy?.any { it.hasRoute<Bills>() } == true
                            AnimatedContent(
                                targetState = isSelected,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                                },
                                label = "NavIcon"
                            ) { selected ->
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = if (selected) R.drawable.ic_nav_bills_filled else R.drawable.ic_nav_bills),
                                    contentDescription = "Bills",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = { Text("Bills") },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<Bills>() } == true,
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            indicatorColor = com.pesalytics.ui.theme.AccentGreenDark,
                            selectedIconColor = Color.White,
                            selectedTextColor = com.pesalytics.ui.theme.AccentGreenDark,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        onClick = {
                            navController.navigate(Bills) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            val isSelected = currentDestination?.hierarchy?.any { it.hasRoute<Settings>() } == true
                            AnimatedContent(
                                targetState = isSelected,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                                },
                                label = "NavIcon"
                            ) { selected ->
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = if (selected) R.drawable.ic_nav_settings_filled else R.drawable.ic_nav_settings),
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = { Text("Settings") },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<Settings>() } == true,
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            indicatorColor = com.pesalytics.ui.theme.AccentGreenDark,
                            selectedIconColor = Color.White,
                            selectedTextColor = com.pesalytics.ui.theme.AccentGreenDark,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        onClick = {
                            navController.navigate(Settings) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Splash,
                enterTransition = { NavEnterTransition },
                exitTransition = { NavExitTransition },
                popEnterTransition = { NavPopEnterTransition },
                popExitTransition = { NavPopExitTransition }
            ) {
                composable<Splash> {
                    SplashScreen(
                        viewModel = viewModel,
                        onSplashComplete = { isFirstLaunch ->
                            if (isFirstLaunch) {
                                navController.navigate(Onboarding) {
                                    popUpTo(Splash) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Home) {
                                    popUpTo(Splash) { inclusive = true }
                                }
                            }
                        }
                    )
                }
                composable<Onboarding> {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onNavigateNext = {
                            navController.navigate(Home) {
                                popUpTo(Onboarding) { inclusive = true }
                            }
                        }
                    )
                }
                composable<Home>(
                    enterTransition = { TabEnterTransition },
                    exitTransition = { TabExitTransition },
                    popEnterTransition = { TabEnterTransition },
                    popExitTransition = { TabExitTransition }
                ) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAllTransactions = { navController.navigate(AllTransactions()) },
                        onNavigateToAnalytics = { navController.navigate(Analytics) },
                        onNavigateToBills = { navController.navigate(Bills) },
                        onNavigateToBudgetPlanner = { navController.navigate(BudgetPlanner) },
                        onNavigateToGoals = { navController.navigate(FinancialGoals) }
                    )
                }
                composable<Analytics>(
                    enterTransition = { TabEnterTransition },
                    exitTransition = { TabExitTransition },
                    popEnterTransition = { TabEnterTransition },
                    popExitTransition = { TabExitTransition }
                ) {
                    AnalyticsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSubscription = { navController.navigate(Subscription) },
                        onNavigateToNeedsWants = { navController.navigate(NeedsWants) },
                        onNavigateToAllTransactions = { filter -> navController.navigate(AllTransactions(filter)) }
                    )
                }
                composable<Bills>(
                    enterTransition = { TabEnterTransition },
                    exitTransition = { TabExitTransition },
                    popEnterTransition = { TabEnterTransition },
                    popExitTransition = { TabExitTransition }
                ) {
                    BillsScreen(
                        viewModel = viewModel,
                        navController = navController,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<Settings>(
                    enterTransition = { TabEnterTransition },
                    exitTransition = { TabExitTransition },
                    popEnterTransition = { TabEnterTransition },
                    popExitTransition = { TabExitTransition }
                ) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSubscription = { navController.navigate(Subscription) },
                        onNavigateToBudgetPlanner = { navController.navigate(BudgetPlanner) },
                        onNavigateToFinancialGoals = { navController.navigate(FinancialGoals) },
                        onNavigateToFaq = { navController.navigate(Faq) },
                        onNavigateToNeedsWants = { navController.navigate(NeedsWants) }
                    )
                }
                composable<NeedsWants> {
                    NeedsWantsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<BudgetPlanner> {
                    BudgetPlannerScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<Subscription> {
                    SubscriptionScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<AllTransactions> { backStackEntry ->
                    val dest: AllTransactions = backStackEntry.toRoute()
                    AllTransactionsScreen(
                        viewModel = viewModel,
                        initialFilter = dest.filter,
                        navController = navController,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<PayeeHistory>(
                    enterTransition = { PayeeHistoryEnterTransition },
                    exitTransition = { PayeeHistoryExitTransition },
                    popEnterTransition = { PayeeHistoryPopEnterTransition },
                    popExitTransition = { PayeeHistoryPopExitTransition }
                ) { backStackEntry ->
                    val dest: PayeeHistory = backStackEntry.toRoute()
                    PayeeHistoryScreen(
                        payee = dest.payee,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<FinancialGoals> {
                    FinancialGoalsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSubscription = { navController.navigate(Subscription) }
                    )
                }
                composable<Faq> {
                    com.pesalytics.ui.screens.FaqScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
