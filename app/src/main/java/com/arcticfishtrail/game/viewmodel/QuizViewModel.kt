package com.arcticfishtrail.game.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.arcticfishtrail.game.audio.Sfx
import com.arcticfishtrail.game.audio.SoundManager
import com.arcticfishtrail.game.data.GameRepository
import com.arcticfishtrail.game.domain.QuizCategory
import com.arcticfishtrail.game.domain.QuizContent
import com.arcticfishtrail.game.domain.QuizQuestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class QuizFinished(val categoryId: String, val score: Int, val total: Int)

data class QuizUiState(
    val isValid: Boolean = true,
    val categoryId: String = "",
    val categoryTitle: String = "",
    val questionIndex: Int = 0,
    val total: Int = 0,
    val question: QuizQuestion? = null,
    val selectedIndex: Int? = null,
    val score: Int = 0,
    val isPaused: Boolean = false,
    val finished: QuizFinished? = null,
) {
    val isAnswerLocked: Boolean get() = selectedIndex != null || finished != null
    val progress: Float get() = if (total == 0) 0f else (questionIndex + if (selectedIndex != null) 1 else 0).toFloat() / total
}

class QuizViewModel(
    categoryId: String,
    private val savedState: SavedStateHandle,
    private val repository: GameRepository,
    private val sound: SoundManager?,
) : ViewModel() {

    private val category: QuizCategory? = QuizContent.category(categoryId)?.takeIf { it.questions.isNotEmpty() }

    private val _uiState = MutableStateFlow(initialState(categoryId))
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var advanceJob: Job? = null

    private fun initialState(categoryId: String): QuizUiState {
        val cat = category ?: return QuizUiState(isValid = false, categoryId = categoryId)
        // Restore progress after process death (SavedStateHandle survives it).
        val index = (savedState.get<Int>(KEY_INDEX) ?: 0).coerceIn(0, cat.questions.lastIndex)
        val score = (savedState.get<Int>(KEY_SCORE) ?: 0).coerceIn(0, index)
        return QuizUiState(
            categoryId = cat.id,
            categoryTitle = cat.title,
            questionIndex = index,
            total = cat.questions.size,
            question = cat.questions[index],
            score = score,
        )
    }

    fun answer(optionIndex: Int) {
        val state = _uiState.value
        val question = state.question ?: return
        if (state.isAnswerLocked || state.isPaused) return
        if (optionIndex !in question.options.indices) return

        val correct = optionIndex == question.correctIndex
        sound?.play(if (correct) Sfx.CORRECT else Sfx.WRONG)
        _uiState.update { it.copy(selectedIndex = optionIndex, score = if (correct) it.score + 1 else it.score) }

        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            pauseAwareDelay(ADVANCE_DELAY_MS)
            advance()
        }
    }

    private suspend fun advance() {
        val cat = category ?: return
        val state = _uiState.value
        val next = state.questionIndex + 1
        if (next >= cat.questions.size) {
            withContext(NonCancellable) { repository.saveQuizBest(cat.id, state.score) }
            savedState[KEY_INDEX] = 0
            savedState[KEY_SCORE] = 0
            _uiState.update { it.copy(finished = QuizFinished(cat.id, state.score, cat.questions.size)) }
        } else {
            savedState[KEY_INDEX] = next
            savedState[KEY_SCORE] = state.score
            _uiState.update {
                it.copy(questionIndex = next, question = cat.questions[next], selectedIndex = null)
            }
        }
    }

    /** Waits [totalMs] of *unpaused* time, so the auto-advance freezes while paused. */
    private suspend fun pauseAwareDelay(totalMs: Long) {
        var remaining = totalMs
        while (remaining > 0) {
            delay(TICK_MS)
            if (!_uiState.value.isPaused) remaining -= TICK_MS
        }
    }

    fun pause() {
        if (_uiState.value.finished == null && _uiState.value.isValid) _uiState.update { it.copy(isPaused = true) }
    }

    fun resume() {
        _uiState.update { it.copy(isPaused = false) }
    }

    fun restart() {
        val cat = category ?: return
        advanceJob?.cancel()
        savedState[KEY_INDEX] = 0
        savedState[KEY_SCORE] = 0
        _uiState.value = QuizUiState(
            categoryId = cat.id,
            categoryTitle = cat.title,
            questionIndex = 0,
            total = cat.questions.size,
            question = cat.questions.first(),
        )
    }

    companion object {
        const val ADVANCE_DELAY_MS = 750L
        private const val TICK_MS = 50L
        private const val KEY_INDEX = "quiz_index"
        private const val KEY_SCORE = "quiz_score"

        fun factory(categoryId: String, repository: GameRepository, sound: SoundManager?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                    QuizViewModel(categoryId, extras.createSavedStateHandle(), repository, sound) as T
            }
    }
}
