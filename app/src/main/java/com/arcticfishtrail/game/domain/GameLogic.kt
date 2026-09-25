package com.arcticfishtrail.game.domain

import java.util.Locale
import kotlin.random.Random

/** One Matching Pairs level: number of pairs, grid columns and time limit in seconds. */
data class LevelConfig(val level: Int, val pairs: Int, val columns: Int, val seconds: Int)

/** A card on the board. [id] is unique on the board, [itemIndex] (0..11) selects the icon. */
data class Card(val id: Int, val itemIndex: Int)

/** Immutable board state used by the Pairs game. */
data class Board(
    val cards: List<Card> = emptyList(),
    val matchedIds: Set<Int> = emptySet(),
    val openIds: List<Int> = emptyList(),
) {
    val totalPairs: Int get() = cards.size / 2
    val matchedPairs: Int get() = matchedIds.size / 2
    val isCleared: Boolean get() = cards.isNotEmpty() && matchedIds.size == cards.size
}

enum class FlipOutcome { IGNORED, FIRST, MATCH, MISMATCH }

/**
 * Pure, context-free game rules. No Android types → fully unit-testable.
 */
object GameLogic {

    const val ITEM_COUNT = 18
    const val QUESTIONS_PER_QUIZ = 10

    /** pairs / columns / seconds, levels 1..9 of increasing difficulty. */
    val levels: List<LevelConfig> = listOf(
        LevelConfig(1, pairs = 2, columns = 2, seconds = 60),
        LevelConfig(2, pairs = 3, columns = 2, seconds = 70),
        LevelConfig(3, pairs = 4, columns = 2, seconds = 70),
        LevelConfig(4, pairs = 6, columns = 3, seconds = 80),
        LevelConfig(5, pairs = 6, columns = 3, seconds = 80),
        LevelConfig(6, pairs = 8, columns = 4, seconds = 100),
        LevelConfig(7, pairs = 8, columns = 4, seconds = 100),
        LevelConfig(8, pairs = 10, columns = 4, seconds = 110),
        LevelConfig(9, pairs = 12, columns = 4, seconds = 120),
    )

    val LEVEL_COUNT: Int get() = levels.size

    fun levelConfig(level: Int): LevelConfig? = levels.firstOrNull { it.level == level }

    /**
     * Builds a shuffled deck of `pairs` distinct items, each present exactly twice.
     * `pairs` is clamped to 0..[itemCount].
     */
    fun buildDeck(pairs: Int, random: Random = Random.Default, itemCount: Int = ITEM_COUNT): List<Card> {
        val safePairs = pairs.coerceIn(0, itemCount.coerceAtLeast(0))
        val chosen = (0 until itemCount).shuffled(random).take(safePairs)
        return (chosen + chosen).shuffled(random).mapIndexed { index, item -> Card(id = index, itemIndex = item) }
    }

    /** Applies a tap on [cardId]. Returns the new board and what happened. */
    fun flip(board: Board, cardId: Int): Pair<Board, FlipOutcome> {
        val card = board.cards.firstOrNull { it.id == cardId } ?: return board to FlipOutcome.IGNORED
        if (cardId in board.matchedIds || cardId in board.openIds || board.openIds.size >= 2) {
            return board to FlipOutcome.IGNORED
        }
        if (board.openIds.isEmpty()) {
            return board.copy(openIds = listOf(cardId)) to FlipOutcome.FIRST
        }
        val first = board.cards.first { it.id == board.openIds.first() }
        return if (first.itemIndex == card.itemIndex) {
            board.copy(matchedIds = board.matchedIds + first.id + card.id, openIds = emptyList()) to FlipOutcome.MATCH
        } else {
            board.copy(openIds = board.openIds + cardId) to FlipOutcome.MISMATCH
        }
    }

    /** Turns the unmatched open cards face down again. */
    fun closeOpenCards(board: Board): Board = board.copy(openIds = emptyList())

    fun bestPairs(pairsBest: Map<String, Int>, level: Int): Int = pairsBest[level.toString()] ?: 0

    fun isLevelCompleted(level: Int, pairsBest: Map<String, Int>): Boolean {
        val config = levelConfig(level) ?: return false
        return bestPairs(pairsBest, level) >= config.pairs
    }

    /** Level 1 is always unlocked; any other level unlocks after the previous one is cleared. */
    fun isLevelUnlocked(level: Int, pairsBest: Map<String, Int>): Boolean = when {
        level == 1 -> true
        levelConfig(level) == null -> false
        else -> isLevelCompleted(level - 1, pairsBest)
    }

    fun completedLevelCount(pairsBest: Map<String, Int>): Int =
        levels.count { isLevelCompleted(it.level, pairsBest) }

    fun nextLevel(level: Int): Int? = (level + 1).takeIf { levelConfig(it) != null }

    fun isValidQuizResult(score: Int, total: Int): Boolean = total in 1..100 && score in 0..total

    /** "m:ss" for the countdown; negative input is shown as 0:00. */
    fun formatTime(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
    }
}
