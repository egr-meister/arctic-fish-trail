package com.arcticfishtrail.game

import com.arcticfishtrail.game.data.GameData
import com.arcticfishtrail.game.data.GameSerializer
import com.arcticfishtrail.game.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GameSerializerTest {

    @Test
    fun nullEmptyAndBlank_returnDefaults() {
        assertEquals(GameData(), GameSerializer.decode(null))
        assertEquals(GameData(), GameSerializer.decode(""))
        assertEquals(GameData(), GameSerializer.decode("   \n"))
    }

    @Test
    fun corruptedJson_returnsDefaults() {
        listOf(
            "{",
            "not json at all",
            "[1,2,3]",
            "null",
            "{\"quizBest\": \"oops\"}",
            "{\"pairsBest\": {\"1\": \"two\"}}",
            "\u0000\u0001garbage",
        ).forEach { raw -> assertEquals("input: $raw", GameData(), GameSerializer.decode(raw)) }
    }

    @Test
    fun unknownKeys_areIgnored() {
        val raw = """{"quizBest":{"easy":7},"futureFeature":{"a":1},"settings":{"soundEnabled":false,"vibration":true}}"""
        val data = GameSerializer.decode(raw)
        assertEquals(mapOf("easy" to 7), data.quizBest)
        assertFalse(data.settings.soundEnabled)
    }

    @Test
    fun missingFields_takeDefaults() {
        assertEquals(GameData(), GameSerializer.decode("{}"))
        val data = GameSerializer.decode("""{"pairsBest":{"1":2}}""")
        assertEquals(mapOf("1" to 2), data.pairsBest)
        assertEquals(emptyMap<String, Int>(), data.quizBest)
        assertEquals(Settings(), data.settings)
    }

    @Test
    fun nullFields_areCoercedToDefaults() {
        val data = GameSerializer.decode("""{"quizBest":null,"settings":null}""")
        assertEquals(GameData(), data)
    }

    @Test
    fun roundTrip_preservesData() {
        val original = GameData(
            quizBest = mapOf("easy" to 10, "hard" to 4),
            pairsBest = mapOf("1" to 2, "2" to 3),
            settings = Settings(soundEnabled = false),
        )
        assertEquals(original, GameSerializer.decode(GameSerializer.encode(original)))
    }

    @Test
    fun sanitize_dropsBadKeysAndClampsValues() {
        val data = GameSerializer.decode("""{"quizBest":{"easy":-5,"":3},"pairsBest":{"abc":4,"2":-1,"3":4}}""")
        assertEquals(mapOf("easy" to 0), data.quizBest)
        assertEquals(mapOf("2" to 0, "3" to 4), data.pairsBest)
    }
}
