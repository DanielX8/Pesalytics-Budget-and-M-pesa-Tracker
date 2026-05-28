package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.model.ThemeMode
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as PesaSenseApplication
            val notificationHelper = remember { com.example.notifications.NotificationHelper(app) }
            val factory = remember { PesaViewModelFactory(app.repository, notificationHelper) }
            val viewModel: PesaViewModel = viewModel(factory = factory)
            
            val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }


            MyApplicationTheme(darkTheme = darkTheme) {
                androidx.compose.material3.ProvideTextStyle(
                    value = androidx.compose.ui.text.TextStyle(fontFamily = com.example.ui.theme.SpaceGroteskFontFamily)
                ) {
                    PesaSenseApp(viewModel = viewModel)
                }
            }
        }
    }
}



@Composable
fun PesaSenseApp(viewModel: PesaViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination


    val isTopLevelDestination = currentDestination?.hierarchy?.any {
        it.hasRoute<Home>() ||
        it.hasRoute<Analytics>() ||
        it.hasRoute<Bills>() ||
        it.hasRoute<Settings>()
    } ?: false

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        icon = { 
                            val isSelected = currentDestination?.hierarchy?.any { it.hasRoute<Home>() } == true
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = if (isSelected) R.drawable.ic_nav_home_filled else R.drawable.ic_nav_home), 
                                contentDescription = "Home",
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        label = { Text("Home") },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<Home>() } == true,
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            indicatorColor = com.example.ui.theme.AccentGreenLight.copy(alpha = 0.2f),
                            selectedIconColor = com.example.ui.theme.AccentGreenDark,
                            selectedTextColor = com.example.ui.theme.AccentGreenDark
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
                            val isSelected = currentDestination?.hierarchy?.any { it.route == Analytics::class.qualifiedName } == true
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.Analytics else Icons.Outlined.Analytics, 
                                contentDescription = "Analytics",
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        label = { Text("Analytics") },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<Analytics>() } == true,
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            indicatorColor = com.example.ui.theme.AccentGreenLight.copy(alpha = 0.2f),
                            selectedIconColor = com.example.ui.theme.AccentGreenDark,
                            selectedTextColor = com.example.ui.theme.AccentGreenDark
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
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = if (isSelected) R.drawable.ic_nav_bills_filled else R.drawable.ic_nav_bills), 
                                contentDescription = "Bills",
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        label = { Text("Bills") },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<Bills>() } == true,
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            indicatorColor = com.example.ui.theme.AccentGreenLight.copy(alpha = 0.2f),
                            selectedIconColor = com.example.ui.theme.AccentGreenDark,
                            selectedTextColor = com.example.ui.theme.AccentGreenDark
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
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = if (isSelected) R.drawable.ic_nav_settings_filled else R.drawable.ic_nav_settings), 
                                contentDescription = "Settings",
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        label = { Text("Settings") },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute<Settings>() } == true,
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            indicatorColor = com.example.ui.theme.AccentGreenLight.copy(alpha = 0.2f),
                            selectedIconColor = com.example.ui.theme.AccentGreenDark,
                            selectedTextColor = com.example.ui.theme.AccentGreenDark
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
                composable<Home> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToAllTransactions = { navController.navigate(AllTransactions) },
                        onNavigateToAnalytics = { navController.navigate(Analytics) },
                        onNavigateToBills = { navController.navigate(Bills) },
                        onNavigateToSettings = { navController.navigate(BudgetPlanner) },
                        onNavigateToGoals = { navController.navigate(FinancialGoals) }
                    )
                }
                composable<Analytics> {
                    AnalyticsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<Bills> {
                    BillsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<Settings> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSubscription = { navController.navigate(Subscription) },
                        onNavigateToBudgetPlanner = { navController.navigate(BudgetPlanner) },
                        onNavigateToFinancialGoals = { navController.navigate(FinancialGoals) },
                        onNavigateToFaq = { navController.navigate(Faq) }
                    )
                }
                composable<BudgetPlanner> {
                    BudgetPlannerScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<Subscription> {
                    SubscriptionScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable<AllTransactions> {
                    AllTransactionsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<FinancialGoals> {
                    FinancialGoalsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable<Faq> {
                    com.example.ui.screens.FaqScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
