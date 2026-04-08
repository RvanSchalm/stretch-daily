package com.stretchdaily.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import com.stretchdaily.app.ui.session.SessionPreviewScreen
import com.stretchdaily.app.ui.session.SessionUiState
import com.stretchdaily.app.ui.session.SessionViewModel

/**
 * Top-level routes. Each session leg lives inside the [Routes.SESSION_GRAPH]
 * nested graph so they can share a single [SessionViewModel] instance via the
 * graph's NavBackStackEntry.
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
    fun benchmarkHistory(benchmarkId: String) = "benchmarks/history/$benchmarkId"
    const val ARG_BENCHMARK_ID = "benchmarkId"
}

@Composable
fun StretchDailyNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            // Refresh the banner each time Home re-enters composition.
            LaunchedEffect(Unit) { viewModel.refresh() }
            HomeScreen(
                state = state,
                onStartSession = { navController.navigate(Routes.SESSION_GRAPH) },
                onOpenBenchmarks = { navController.navigate(Routes.BENCHMARKS_GRAPH) },
            )
        }
        sessionGraph(navController)
        benchmarksGraph(navController)
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
 * The benchmarks leg — list + per-benchmark history — shares one
 * [BenchmarksViewModel] so the log dialog state and the freshly-saved logs
 * are consistent across the two destinations.
 */
private fun androidx.navigation.NavGraphBuilder.benchmarksGraph(
    navController: NavHostController,
) {
    navigation(startDestination = Routes.BENCHMARKS_LIST, route = Routes.BENCHMARKS_GRAPH) {
        composable(Routes.BENCHMARKS_LIST) {
            val viewModel = benchmarksViewModel(navController)
            BenchmarksScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onOpenHistory = { benchmark ->
                    navController.navigate(Routes.benchmarkHistory(benchmark.id))
                },
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
