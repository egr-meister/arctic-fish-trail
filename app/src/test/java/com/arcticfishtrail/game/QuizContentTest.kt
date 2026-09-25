package com.arcticfishtrail.game

import com.arcticfishtrail.game.domain.GameLogic
import com.arcticfishtrail.game.domain.QuizContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizContentTest {

    @Test
    fun threeDifficulties_tenQuestionsEach_fourOptions_oneValidAnswer() {
        assertEquals(listOf("easy", "medium", "hard"), QuizContent.categories.map { it.id })
        QuizContent.categories.forEach { cat ->
            assertEquals(cat.id, GameLogic.QUESTIONS_PER_QUIZ, cat.questions.size)
            cat.questions.forEach { q ->
                assertEquals(q.text, 4, q.options.size)
                assertTrue(q.text, q.correctIndex in q.options.indices)
                assertEquals("duplicate options in: ${q.text}", 4, q.options.toSet().size)
            }
            assertTrue(cat.iconItem in 0 until GameLogic.ITEM_COUNT)
        }
    }

    @Test
    fun unknownCategory_isNull() {
        assertNull(QuizContent.category("nope"))
        assertNull(QuizContent.category(null))
    }
}
