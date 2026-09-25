package com.arcticfishtrail.game

import com.arcticfishtrail.game.domain.Board
import com.arcticfishtrail.game.domain.Card
import com.arcticfishtrail.game.domain.FlipOutcome
import com.arcticfishtrail.game.domain.GameLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameLogicTest {

    // ---------------------------------------------------------------- deck building

    @Test
    fun deck_hasTwoOfEachItem_forEveryLevel() {
        GameLogic.levels.forEach { cfg ->
            val deck = GameLogic.buildDeck(cfg.pairs, Random(cfg.level))
            assertEquals("level ${cfg.level} card count", cfg.pairs * 2, deck.size)
            val counts = deck.groupingBy { it.itemIndex }.eachCount()
            assertEquals("level ${cfg.level} distinct items", cfg.pairs, counts.size)
            assertTrue("every item exactly twice", counts.values.all { it == 2 })
            assertEquals("unique card ids", deck.size, deck.map { it.id }.toSet().size)
            assertTrue("item indices in range", deck.all { it.itemIndex in 0 until GameLogic.ITEM_COUNT })
        }
    }

    @Test
    fun deck_clampsInvalidPairCounts() {
        assertTrue(GameLogic.buildDeck(0).isEmpty())
        assertTrue(GameLogic.buildDeck(-3).isEmpty())
        assertEquals(GameLogic.ITEM_COUNT * 2, GameLogic.buildDeck(999).size)
    }

    @Test
    fun deck_isShuffled_andDeterministicWithSeed() {
        val a = GameLogic.buildDeck(12, Random(42))
        val b = GameLogic.buildDeck(12, Random(42))
        assertEquals(a, b)
        val sorted = a.sortedBy { it.itemIndex }.map { it.itemIndex }
        assertFalse("deck should not come out sorted", a.map { it.itemIndex } == sorted)
    }

    @Test
    fun levelTable_matchesSpec() {
        val expected = listOf(2 to 2, 3 to 2, 4 to 2, 6 to 3, 6 to 3, 8 to 4, 8 to 4, 10 to 4, 12 to 4)
        val seconds = listOf(60, 70, 70, 80, 80, 100, 100, 110, 120)
        assertEquals(9, GameLogic.LEVEL_COUNT)
        GameLogic.levels.forEachIndexed { i, cfg ->
            assertEquals(i + 1, cfg.level)
            assertEquals(expected[i].first, cfg.pairs)
            assertEquals(expected[i].second, cfg.columns)
            assertEquals(seconds[i], cfg.seconds)
            assertTrue("enough icons", cfg.pairs <= GameLogic.ITEM_COUNT)
        }
        assertNull(GameLogic.levelConfig(0))
        assertNull(GameLogic.levelConfig(10))
    }

    // ---------------------------------------------------------------- flipping

    private val board = Board(listOf(Card(0, 5), Card(1, 7), Card(2, 5), Card(3, 7)))

    @Test
    fun flip_matchAndMismatch() {
        val (b1, o1) = GameLogic.flip(board, 0)
        assertEquals(FlipOutcome.FIRST, o1)
        val (b2, o2) = GameLogic.flip(b1, 2)
        assertEquals(FlipOutcome.MATCH, o2)
        assertEquals(setOf(0, 2), b2.matchedIds)
        assertEquals(1, b2.matchedPairs)

        val (b3, _) = GameLogic.flip(b2, 1)
        val (b4, o4) = GameLogic.flip(b3, 0) // already matched → ignored
        assertEquals(FlipOutcome.IGNORED, o4)
        assertEquals(b3, b4)
    }

    @Test
    fun flip_mismatchBlocksThirdCard_untilClosed() {
        val (b1, _) = GameLogic.flip(board, 0)
        val (b2, o2) = GameLogic.flip(b1, 1)
        assertEquals(FlipOutcome.MISMATCH, o2)
        val (_, o3) = GameLogic.flip(b2, 2)
        assertEquals(FlipOutcome.IGNORED, o3)
        val closed = GameLogic.closeOpenCards(b2)
        assertTrue(closed.openIds.isEmpty())
        assertEquals(FlipOutcome.FIRST, GameLogic.flip(closed, 2).second)
    }

    @Test
    fun flip_sameCardTwiceOrUnknownId_isIgnored() {
        val (b1, _) = GameLogic.flip(board, 0)
        assertEquals(FlipOutcome.IGNORED, GameLogic.flip(b1, 0).second)
        assertEquals(FlipOutcome.IGNORED, GameLogic.flip(board, 99).second)
    }

    @Test
    fun board_clearedAfterAllMatches() {
        var b = board
        listOf(0, 2, 1, 3).forEach { b = GameLogic.flip(b, it).first }
        assertTrue(b.isCleared)
        assertEquals(2, b.matchedPairs)
        assertFalse(Board().isCleared)
    }

    // ---------------------------------------------------------------- unlock & completion

    @Test
    fun level1_alwaysUnlocked_othersLockedOnFreshData() {
        val empty = emptyMap<String, Int>()
        assertTrue(GameLogic.isLevelUnlocked(1, empty))
        (2..9).forEach { assertFalse(GameLogic.isLevelUnlocked(it, empty)) }
        assertFalse(GameLogic.isLevelUnlocked(0, empty))
        assertFalse(GameLogic.isLevelUnlocked(10, mapOf("9" to 12)))
    }

    @Test
    fun clearingALevel_unlocksOnlyTheNext() {
        val progress = mapOf("1" to 2)
        assertTrue(GameLogic.isLevelCompleted(1, progress))
        assertTrue(GameLogic.isLevelUnlocked(2, progress))
        assertFalse(GameLogic.isLevelUnlocked(3, progress))
    }

    @Test
    fun partialBest_doesNotCompleteOrUnlock() {
        val progress = mapOf("1" to 1, "4" to 5)
        assertFalse(GameLogic.isLevelCompleted(1, progress))
        assertFalse(GameLogic.isLevelUnlocked(2, progress))
        assertFalse(GameLogic.isLevelCompleted(4, progress))
    }

    @Test
    fun completedCount_countsOnlyFullyClearedLevels() {
        assertEquals(0, GameLogic.completedLevelCount(emptyMap()))
        val progress = mapOf("1" to 2, "2" to 3, "3" to 3, "5" to 6, "x" to 50)
        assertEquals(3, GameLogic.completedLevelCount(progress))
        val all = GameLogic.levels.associate { it.level.toString() to it.pairs }
        assertEquals(9, GameLogic.completedLevelCount(all))
    }

    @Test
    fun nextLevel_andHelpers() {
        assertEquals(2, GameLogic.nextLevel(1))
        assertNull(GameLogic.nextLevel(9))
        assertEquals("1:05", GameLogic.formatTime(65))
        assertEquals("0:00", GameLogic.formatTime(-4))
        assertTrue(GameLogic.isValidQuizResult(7, 10))
        assertFalse(GameLogic.isValidQuizResult(-1, 10))
        assertFalse(GameLogic.isValidQuizResult(11, 10))
        assertFalse(GameLogic.isValidQuizResult(0, 0))
    }
}
