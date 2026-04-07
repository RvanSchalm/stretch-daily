package com.stretchdaily.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.stretchdaily.app.ui.home.HomeScreen
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
}

@Composable
fun StretchDailyNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onStartSession = { navController.navigate(Routes.SESSION_GRAPH) })
        }
        sessionGraph(navController)
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
