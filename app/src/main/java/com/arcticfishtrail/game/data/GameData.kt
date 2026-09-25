package com.arcticfishtrail.game.data

import kotlinx.serialization.Serializable

/**
 * Everything the game persists, stored as ONE JSON string in DataStore Preferences.
 * Every field has a default so older/newer JSON always decodes (forward compatible).
 *
 * @property quizBest best quiz score per category id (0..10)
 * @property pairsBest best matched pairs per level, keyed by the level number as a string ("1".."9")
 */
@Serializable
data class GameData(
    val quizBest: Map<String, Int> = emptyMap(),
    val pairsBest: Map<String, Int> = emptyMap(),
    val settings: Settings = Settings(),
)

@Serializable
data class Settings(
    val soundEnabled: Boolean = true,
)
