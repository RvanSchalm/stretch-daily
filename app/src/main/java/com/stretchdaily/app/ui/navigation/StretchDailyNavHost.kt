package com.stretchdaily.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stretchdaily.app.ui.screen.dashboard.DashboardScreen
import com.stretchdaily.app.ui.screen.log.BenchmarkLogScreen
import com.stretchdaily.app.ui.screen.placeholder.PlaceholderScreen
import com.stretchdaily.app.ui.screen.session.SessionCompleteScreen
import com.stretchdaily.app.ui.screen.session.SessionOverviewScreen
import com.stretchdaily.app.ui.screen.session.SessionPlayerScreen
import com.stretchdaily.app.ui.theme.Theme
import java.util.Locale

/**
 * Top-level routes. Matches spec §7.1.
 *
 * Five tabs (today, session-overview, log, progress, settings) plus two
 * nested overlay graphs:
 *  - session graph hosts overview → player → complete, sharing one VM
 *    via `hiltViewModel(parentEntry)`.
 *  - carousel graph hosts `carousel/{step}` pages.
 *
 * `session/player`, `session/complete`, and `carousel/{step}` hide the bottom
 * nav — they're immersive overlays.
 */
object Routes {
    const val TODAY = "today"

    const val SESSION_GRAPH = "session"
    const val SESSION_OVERVIEW = "session/overview"
    const val SESSION_PLAYER = "session/player"
    const val SESSION_COMPLETE = "session/complete"

    const val LOG = "log"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"

    const val CAROUSEL_GRAPH = "carousel"
    const val CAROUSEL_STEP = "carousel/{step}"
    const val ARG_CAROUSEL_STEP = "step"
    fun carouselStep(step: Int) = "carousel/$step"

    const val DEBUG_GALLERY = "debug/gallery"
}

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val BOTTOM_NAV_TABS = listOf(
    TabItem(Routes.TODAY, "Today", Icons.Outlined.Home),
    TabItem(Routes.SESSION_GRAPH, "Session", Icons.Outlined.PlayArrow),
    TabItem(Routes.LOG, "Log", Icons.Outlined.EditNote),
    TabItem(Routes.PROGRESS, "Progress", Icons.Outlined.BarChart),
    TabItem(Routes.SETTINGS, "Settings", Icons.Outlined.Settings),
)

/**
 * Routes that own the bottom navigation bar. Session-player, complete,
 * and the benchmark carousel are immersive and hide the bar.
 */
private val BOTTOM_NAV_ROUTES = setOf(
    Routes.TODAY,
    Routes.SESSION_OVERVIEW,
    Routes.LOG,
    Routes.PROGRESS,
    Routes.SETTINGS,
)

@Composable
fun StretchDailyNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in BOTTOM_NAV_ROUTES

    Scaffold(
        containerColor = Theme.colors.bg,
        // Inner screens own their status-bar insets; the outer Scaffold
        // only contributes the bottom-nav height via contentPadding.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
        ) {
            composable(Routes.TODAY) {
                DashboardScreen(
                    onStartSession = { navController.navigate(Routes.SESSION_OVERVIEW) },
                    onBenchmarkBannerTap = { navController.navigate(Routes.carouselStep(0)) },
                    contentPadding = padding,
                )
            }
            sessionGraph(padding, navController)
            composable(Routes.LOG) {
                BenchmarkLogScreen(
                    onStartCarousel = { navController.navigate(Routes.carouselStep(0)) },
                    contentPadding = padding,
                )
            }
            composable(Routes.PROGRESS) {
                PlaceholderScreen(
                    tabLabel = "Progress",
                    unlocksInPhase = "R6",
                    contentPadding = padding,
                )
            }
            composable(Routes.SETTINGS) {
                PlaceholderScreen(
                    tabLabel = "Settings",
                    unlocksInPhase = "R6",
                    contentPadding = padding,
                    onLongPress = { navController.navigate(Routes.DEBUG_GALLERY) },
                )
            }
            carouselGraph()
            composable(Routes.DEBUG_GALLERY) {
                com.stretchdaily.app.ui.debug.ComponentGalleryScreen(
                    contentPadding = padding,
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(containerColor = Theme.colors.surface) {
        BOTTOM_NAV_TABS.forEach { tab ->
            val selected = currentRoute.belongsToTab(tab.route)
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = {
                    Text(
                        text = tab.label.uppercase(Locale.getDefault()),
                        style = Theme.typo.monoCapsSm,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Theme.colors.accent,
                    selectedTextColor = Theme.colors.accent,
                    indicatorColor = Theme.colors.accentSoft,
                    unselectedIconColor = Theme.colors.ink3,
                    unselectedTextColor = Theme.colors.ink3,
                ),
            )
        }
    }
}

/**
 * Maps a destination route back to its owning bottom-nav tab root.
 * The session graph's entries (`session/overview` + player + complete)
 * all belong to the Session tab; carousel routes have no tab (no bar).
 */
private fun String?.belongsToTab(tabRoute: String): Boolean = when (tabRoute) {
    Routes.TODAY -> this == Routes.TODAY
    Routes.SESSION_GRAPH -> this == Routes.SESSION_OVERVIEW ||
            this == Routes.SESSION_PLAYER ||
            this == Routes.SESSION_COMPLETE
    Routes.LOG -> this == Routes.LOG
    Routes.PROGRESS -> this == Routes.PROGRESS
    Routes.SETTINGS -> this == Routes.SETTINGS
    else -> false
}

/** Pop to tab root, single-top, restore state — standard bottom-nav pattern. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Session graph stubs. R4 replaces each placeholder body with the real
 * screens; the graph structure (nested routes, parentEntry-scoped VM)
 * already matches the final shape.
 */
private fun androidx.navigation.NavGraphBuilder.sessionGraph(
    contentPadding: PaddingValues,
    navController: NavHostController,
) {
    navigation(startDestination = Routes.SESSION_OVERVIEW, route = Routes.SESSION_GRAPH) {
        composable(Routes.SESSION_OVERVIEW) {
            SessionOverviewScreen(
                onBeginSession = { navController.navigate(Routes.SESSION_PLAYER) },
                contentPadding = contentPadding,
            )
        }
        composable(Routes.SESSION_PLAYER) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.SESSION_GRAPH)
            }
            SessionPlayerScreen(
                parentEntry = parentEntry,
                onClose = {
                    navController.popBackStack(
                        route = Routes.TODAY,
                        inclusive = false,
                    )
                },
                onComplete = {
                    navController.navigate(Routes.SESSION_COMPLETE) {
                        // Clear the player from backstack so Back-to-today on
                        // Complete pops straight to the dashboard instead of
                        // showing the player again on the way out.
                        popUpTo(Routes.SESSION_PLAYER) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SESSION_COMPLETE) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.SESSION_GRAPH)
            }
            SessionCompleteScreen(
                parentEntry = parentEntry,
                onBackToToday = {
                    navController.popBackStack(
                        route = Routes.TODAY,
                        inclusive = false,
                    )
                },
            )
        }
    }
}

/** Benchmark carousel overlay graph — bottom-nav hidden on all steps. */
private fun androidx.navigation.NavGraphBuilder.carouselGraph() {
    navigation(startDestination = Routes.carouselStep(0), route = Routes.CAROUSEL_GRAPH) {
        composable(
            route = Routes.CAROUSEL_STEP,
            arguments = listOf(
                navArgument(Routes.ARG_CAROUSEL_STEP) { type = NavType.IntType }
            ),
        ) {
            PlaceholderScreen(
                tabLabel = "Benchmark carousel",
                unlocksInPhase = "R5",
                contentPadding = PaddingValues(0.dp),
            )
        }
    }
}
