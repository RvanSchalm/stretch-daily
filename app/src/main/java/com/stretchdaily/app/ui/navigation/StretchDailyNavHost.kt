package com.stretchdaily.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stretchdaily.app.ui.benchmarks.BenchmarkHistoryScreen
import com.stretchdaily.app.ui.benchmarks.BenchmarksScreen
import com.stretchdaily.app.ui.benchmarks.BenchmarksViewModel
import com.stretchdaily.app.ui.home.HomeScreen
import com.stretchdaily.app.ui.home.HomeViewModel
import com.stretchdaily.app.ui.session.SessionCompleteScreen
import com.stretchdaily.app.ui.session.SessionFollowAlongScreen
import com.stretchdaily.app.ui.session.SessionHistoryScreen
import com.stretchdaily.app.ui.session.SessionPreviewScreen
import com.stretchdaily.app.ui.session.SessionUiState
import com.stretchdaily.app.ui.session.SessionViewModel
import com.stretchdaily.app.ui.settings.SettingsScreen

/**
 * Top-level routes. Each session leg lives inside the [Routes.SESSION_GRAPH]
 * nested graph so they can share a single [SessionViewModel] instance via the
 * graph's NavBackStackEntry. Same trick is used for the benchmarks graph.
 */
object Routes {
    const val HOME = "home"
    const val SESSION_GRAPH = "session_graph"
    const val SESSION_PREVIEW = "session/preview"
    const val SESSION_FOLLOW = "session/follow"
    const val SESSION_COMPLETE = "session/complete"
    const val BENCHMARKS_GRAPH = "benchmarks_graph"
    const val BENCHMARKS_LIST = "benchmarks/list"
    const val BENCHMARK_HISTORY = "benchmarks/history/{benchmarkId}"
    const val SETTINGS = "settings"
    const val SESSION_HISTORY = "settings/sessions"
    fun benchmarkHistory(benchmarkId: String) = "benchmarks/history/$benchmarkId"
    const val ARG_BENCHMARK_ID = "benchmarkId"
}

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val BOTTOM_NAV_TABS = listOf(
    TabItem(Routes.HOME, "Home", Icons.Filled.Home),
    TabItem(Routes.BENCHMARKS_GRAPH, "Benchmarks", Icons.Filled.Star),
    TabItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

/**
 * Routes that own the bottom navigation bar. The session flow and the
 * session history detail are immersive and hide the bar.
 */
private val BOTTOM_NAV_ROUTES = setOf(
    Routes.HOME,
    Routes.BENCHMARKS_LIST,
    Routes.BENCHMARK_HISTORY,
    Routes.SETTINGS,
)

@Composable
fun StretchDailyNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in BOTTOM_NAV_ROUTES

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Inner screens (HomeScreen, BenchmarksScreen, SettingsScreen) own
        // their own Scaffolds and consume status-bar insets themselves —
        // we only want this outer Scaffold to contribute the bottom-nav
        // height. Setting contentWindowInsets to zero prevents double
        // status-bar padding.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
        ) {
            composable(Routes.HOME) {
                val viewModel: HomeViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()
                LaunchedEffect(Unit) { viewModel.refresh() }
                HomeScreen(
                    state = state,
                    onStartSession = { navController.navigate(Routes.SESSION_GRAPH) },
                    onOpenBenchmarks = {
                        navController.navigateToTab(Routes.BENCHMARKS_GRAPH)
                    },
                    contentPadding = padding,
                )
            }
            sessionGraph(navController)
            benchmarksGraph(navController, padding)
            settingsGraph(navController, padding)
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        BOTTOM_NAV_TABS.forEach { tab ->
            val selected = currentRoute.belongsToTab(tab.route)
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                ),
            )
        }
    }
}

/**
 * Maps a destination route back to one of the three top-level tab roots.
 * `BENCHMARKS_LIST` and `BENCHMARK_HISTORY` both belong to the Benchmarks
 * tab; `SESSION_HISTORY` belongs to the Settings tab.
 */
private fun String?.belongsToTab(tabRoute: String): Boolean = when (tabRoute) {
    Routes.HOME -> this == Routes.HOME
    Routes.BENCHMARKS_GRAPH -> this == Routes.BENCHMARKS_LIST || this == Routes.BENCHMARK_HISTORY
    Routes.SETTINGS -> this == Routes.SETTINGS || this == Routes.SESSION_HISTORY
    else -> false
}

/**
 * Standard tab-style navigation: pop back to the graph's start destination,
 * single-top to avoid stacking, and restore prior state of the tab if any.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * The session leg is a nested graph so all three screens can share one
 * [SessionViewModel] — scoped to the graph entry, not to each composable.
 * That's how the timer state survives navigation from preview to follow-along.
 */
private fun androidx.navigation.NavGraphBuilder.sessionGraph(
    navController: NavHostController,
) {
    navigation(startDestination = Routes.SESSION_PREVIEW, route = Routes.SESSION_GRAPH) {
        composable(Routes.SESSION_PREVIEW) {
            val viewModel = sessionViewModel(navController)
            val state by viewModel.state.collectAsState()
            SessionPreviewScreen(
                state = state,
                onBack = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onSwap = viewModel::swap,
                onStart = {
                    viewModel.start()
                    navController.navigate(Routes.SESSION_FOLLOW) {
                        popUpTo(Routes.SESSION_PREVIEW) { inclusive = true }
                    }
                },
                onRetry = viewModel::generate,
            )
        }
        composable(Routes.SESSION_FOLLOW) {
            val viewModel = sessionViewModel(navController)
            val state by viewModel.state.collectAsState()
            // The ViewModel transitions to Complete on its own when the
            // last exercise wraps up — react and route forward.
            LaunchedEffect(state) {
                if (state is SessionUiState.Complete) {
                    navController.navigate(Routes.SESSION_COMPLETE) {
                        popUpTo(Routes.SESSION_FOLLOW) { inclusive = true }
                    }
                }
            }
            (state as? SessionUiState.FollowAlong)?.let { followState ->
                SessionFollowAlongScreen(
                    state = followState,
                    onTogglePause = viewModel::togglePause,
                    onSkip = viewModel::skip,
                )
            }
        }
        composable(Routes.SESSION_COMPLETE) {
            val viewModel = sessionViewModel(navController)
            val state by viewModel.state.collectAsState()
            (state as? SessionUiState.Complete)?.let { completeState ->
                SessionCompleteScreen(
                    state = completeState,
                    onDone = {
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    },
                )
            }
        }
    }
}

/**
 * Resolves [SessionViewModel] scoped to the nested session graph back stack
 * entry, so all three session screens share one ViewModel (and one running
 * timer) instead of getting their own per-destination instances.
 */
@Composable
private fun sessionViewModel(navController: NavHostController): SessionViewModel {
    val parentEntry = remember(navController) {
        navController.getBackStackEntry(Routes.SESSION_GRAPH)
    }
    return hiltViewModel(parentEntry)
}

/**
 * Benchmarks tab — list + per-benchmark history — sharing one
 * [BenchmarksViewModel] so the log dialog state stays consistent across
 * the two destinations.
 */
private fun androidx.navigation.NavGraphBuilder.benchmarksGraph(
    navController: NavHostController,
    contentPadding: PaddingValues,
) {
    navigation(startDestination = Routes.BENCHMARKS_LIST, route = Routes.BENCHMARKS_GRAPH) {
        composable(Routes.BENCHMARKS_LIST) {
            val viewModel = benchmarksViewModel(navController)
            BenchmarksScreen(
                viewModel = viewModel,
                onBack = { navController.navigateToTab(Routes.HOME) },
                onOpenHistory = { benchmark ->
                    navController.navigate(Routes.benchmarkHistory(benchmark.id))
                },
                contentPadding = contentPadding,
            )
        }
        composable(
            route = Routes.BENCHMARK_HISTORY,
            arguments = listOf(
                navArgument(Routes.ARG_BENCHMARK_ID) { type = NavType.StringType }
            ),
        ) { entry ->
            val benchmarkId = entry.arguments?.getString(Routes.ARG_BENCHMARK_ID).orEmpty()
            val viewModel = benchmarksViewModel(navController)
            BenchmarkHistoryScreen(
                viewModel = viewModel,
                benchmarkId = benchmarkId,
                onBack = { navController.popBackStack() },
                contentPadding = contentPadding,
            )
        }
    }
}

/** Same scoping trick as [sessionViewModel] but for the benchmarks graph. */
@Composable
private fun benchmarksViewModel(navController: NavHostController): BenchmarksViewModel {
    val parentEntry = remember(navController) {
        navController.getBackStackEntry(Routes.BENCHMARKS_GRAPH)
    }
    return hiltViewModel(parentEntry)
}

/**
 * Settings tab + child routes (currently just session history). The history
 * detail is its own top-level destination so the bottom bar can hide on it.
 */
private fun androidx.navigation.NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    contentPadding: PaddingValues,
) {
    composable(Routes.SETTINGS) {
        SettingsScreen(
            onOpenSessionHistory = { navController.navigate(Routes.SESSION_HISTORY) },
            contentPadding = contentPadding,
        )
    }
    composable(Routes.SESSION_HISTORY) {
        SessionHistoryScreen(onBack = { navController.popBackStack() })
    }
}
