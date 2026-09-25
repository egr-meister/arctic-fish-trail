package com.arcticfishtrail.game.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.arcticfishtrail.game.ui.screens.GameRulesScreen
import com.arcticfishtrail.game.ui.screens.HomeScreen
import com.arcticfishtrail.game.ui.screens.LevelsScreen
import com.arcticfishtrail.game.ui.screens.MenuScreen
import com.arcticfishtrail.game.ui.screens.PairsResultScreen
import com.arcticfishtrail.game.ui.screens.PairsScreen
import com.arcticfishtrail.game.ui.screens.QuizCategoriesScreen
import com.arcticfishtrail.game.ui.screens.QuizResultScreen
import com.arcticfishtrail.game.ui.screens.QuizScreen
import com.arcticfishtrail.game.ui.screens.ResultsSummaryScreen
import com.arcticfishtrail.game.ui.screens.SettingsScreen

/** Navigates only from a RESUMED entry (ignores double taps mid-transition) and never throws. */
fun NavHostController.navigateSafely(route: Any) {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return
    runCatching { navigate(route) { launchSingleTop = true } }
}

/** Replaces the current destination (game → result, result → game) without growing the stack. */
fun NavHostController.replaceSafely(route: Any) {
    val current = currentBackStackEntry ?: return
    if (!current.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
    runCatching {
        navigate(route) {
            launchSingleTop = true
            popUpTo(current.destination.id) { inclusive = true }
        }
    }
}

/** Back that is safe even when the stack is somehow empty (falls back to Menu). */
fun NavHostController.backSafely() {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return
    val popped = runCatching { popBackStack() }.getOrDefault(false)
    if (!popped) runCatching { navigate(MenuRoute) }
}

/** Returns to the Menu, recreating it if it is no longer on the back stack. */
fun NavHostController.toMenu() {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return
    val popped = runCatching { popBackStack(MenuRoute, inclusive = false) }.getOrDefault(false)
    if (!popped) runCatching { navigate(MenuRoute) { popUpTo(HomeRoute) { inclusive = false } } }
}

private inline fun <reified T : Any> NavBackStackEntry.safeRoute(): T? = runCatching { toRoute<T>() }.getOrNull()

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onStart = { navController.navigateSafely(MenuRoute) },
            )
        }
        composable<MenuRoute> {
            MenuScreen(
                onQuiz = { navController.navigateSafely(QuizCategoriesRoute) },
                onPairs = { navController.navigateSafely(LevelsRoute) },
                onResults = { navController.navigateSafely(ResultsRoute) },
                onSettings = { navController.navigateSafely(SettingsRoute) },
                onRules = { navController.navigateSafely(RulesRoute) },
                onBack = { navController.backSafely() },
            )
        }
        composable<QuizCategoriesRoute> {
            QuizCategoriesScreen(
                onCategory = { id -> navController.navigateSafely(QuizRoute(id)) },
                onBack = { navController.backSafely() },
            )
        }
        composable<QuizRoute> { entry ->
            val route = entry.safeRoute<QuizRoute>()
            QuizScreen(
                categoryId = route?.categoryId.orEmpty(),
                onFinished = { r -> navController.replaceSafely(QuizResultRoute(r.categoryId, r.score, r.total)) },
                onMenu = { navController.toMenu() },
                onBack = { navController.backSafely() },
            )
        }
        composable<QuizResultRoute> { entry ->
            val route = entry.safeRoute<QuizResultRoute>()
            QuizResultScreen(
                categoryId = route?.categoryId.orEmpty(),
                score = route?.score ?: -1,
                total = route?.total ?: 0,
                onContinue = { navController.backSafely() },
                onRestart = { id -> navController.replaceSafely(QuizRoute(id)) },
                onMenu = { navController.toMenu() },
                onBack = { navController.backSafely() },
            )
        }
        composable<LevelsRoute> {
            LevelsScreen(
                onLevel = { level -> navController.navigateSafely(PairsRoute(level)) },
                onRules = { navController.navigateSafely(RulesRoute) },
                onBack = { navController.backSafely() },
            )
        }
        composable<PairsRoute> { entry ->
            val route = entry.safeRoute<PairsRoute>()
            PairsScreen(
                level = route?.level ?: 0,
                onFinished = { r -> navController.replaceSafely(PairsResultRoute(r.level, r.matched, r.total, r.won)) },
                onMenu = { navController.toMenu() },
                onBack = { navController.backSafely() },
            )
        }
        composable<PairsResultRoute> { entry ->
            val route = entry.safeRoute<PairsResultRoute>()
            PairsResultScreen(
                level = route?.level ?: 0,
                matched = route?.matched ?: -1,
                total = route?.total ?: 0,
                won = route?.won ?: false,
                onNextLevel = { next -> navController.replaceSafely(PairsRoute(next)) },
                onLevels = { navController.backSafely() },
                onRestart = { level -> navController.replaceSafely(PairsRoute(level)) },
                onMenu = { navController.toMenu() },
                onBack = { navController.backSafely() },
            )
        }
        composable<ResultsRoute> {
            ResultsSummaryScreen(onBack = { navController.backSafely() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.backSafely() })
        }
        composable<RulesRoute> {
            GameRulesScreen(onBack = { navController.backSafely() })
        }
    }
}
