package com.arcticfishtrail.game.ui.navigation

import kotlinx.serialization.Serializable

/*
 * Type-safe Navigation Compose routes. All args have defaults so a malformed/missing value
 * decodes to an obviously-invalid sentinel that the destination turns into a fallback screen.
 */

@Serializable data object HomeRoute
@Serializable data object MenuRoute
@Serializable data object QuizCategoriesRoute
@Serializable data class QuizRoute(val categoryId: String = "")
@Serializable data class QuizResultRoute(val categoryId: String = "", val score: Int = -1, val total: Int = 0)
@Serializable data object LevelsRoute
@Serializable data class PairsRoute(val level: Int = 0)
@Serializable data class PairsResultRoute(val level: Int = 0, val matched: Int = -1, val total: Int = 0, val won: Boolean = false)
@Serializable data object ResultsRoute
@Serializable data object SettingsRoute
@Serializable data object RulesRoute
