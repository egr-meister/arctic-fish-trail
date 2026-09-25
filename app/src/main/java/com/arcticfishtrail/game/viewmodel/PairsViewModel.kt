package com.arcticfishtrail.game.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcticfishtrail.game.audio.Sfx
import com.arcticfishtrail.game.audio.SoundManager
import com.arcticfishtrail.game.data.GameRepository
import com.arcticfishtrail.game.domain.Board
import com.arcticfishtrail.game.domain.FlipOutcome
import com.arcticfishtrail.game.domain.GameLogic
import com.arcticfishtrail.game.domain.LevelConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

data class CardUi(
    val id: Int,
    val itemIndex: Int,
    val isFaceUp: Boolean,
    val isMatched: Boolean,
)

enum class PairsStatus { LOADING, INVALID, LOCKED, PLAYING }

data class PairsFinished(val level: Int, val matched: Int, val total: Int, val won: Boolean)

data class PairsUiState(
    val status: PairsStatus = PairsStatus.LOADING,
    val level: Int = 0,
    val columns: Int = 2,
    val cards: List<CardUi> = emptyList(),
    val matchedPairs: Int = 0,
    val totalPairs: Int = 0,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val isPaused: Boolean = false,
    /** Round is over (input frozen); [finished] is set once the result has been persisted. */
    val isOver: Boolean = false,
    val finished: PairsFinished? = null,
) {
    val isTimeLow: Boolean get() = status == PairsStatus.PLAYING && remainingSeconds <= 10
}

class PairsViewModel(
    private val level: Int,
    private val repository: GameRepository,
    private val sound: SoundManager?,
) : ViewModel() {

    private val config: LevelConfig? = GameLogic.levelConfig(level)

    private val _uiState = MutableStateFlow(PairsUiState(level = level))
    val uiState: StateFlow<PairsUiState> = _uiState.asStateFlow()

    private var board = Board()
    private var remainingMs = 0L
    private var busy = false
    private var timerJob: Job? = null
    private var flipBackJob: Job? = null

    init {
        viewModelScope.launch {
            val cfg = config
            when {
                cfg == null -> _uiState.update { it.copy(status = PairsStatus.INVALID) }
                !GameLogic.isLevelUnlocked(level, repository.current().pairsBest) ->
                    _uiState.update { it.copy(status = PairsStatus.LOCKED) }
                else -> startLevel(cfg)
            }
        }
    }

    private fun startLevel(cfg: LevelConfig) {
        timerJob?.cancel()
        flipBackJob?.cancel()
        busy = false
        board = Board(cards = GameLogic.buildDeck(cfg.pairs))
        remainingMs = cfg.seconds * 1_000L
        _uiState.value = PairsUiState(
            status = PairsStatus.PLAYING,
            level = cfg.level,
            columns = cfg.columns,
            totalSeconds = cfg.seconds,
            remainingSeconds = cfg.seconds,
        ).withBoard(board)
        startTimer()
    }

    private fun PairsUiState.withBoard(b: Board): PairsUiState = copy(
        cards = b.cards.map { c ->
            CardUi(
                id = c.id,
                itemIndex = c.itemIndex,
                isFaceUp = c.id in b.openIds || c.id in b.matchedIds,
                isMatched = c.id in b.matchedIds,
            )
        },
        matchedPairs = b.matchedPairs,
        totalPairs = b.totalPairs,
    )

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(TIMER_TICK_MS)
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - last
                last = now
                val state = _uiState.value
                if (state.isPaused || state.isOver) continue
                remainingMs -= elapsed
                val seconds = ceil(remainingMs.coerceAtLeast(0) / 1000.0).toInt()
                if (seconds != state.remainingSeconds) _uiState.update { it.copy(remainingSeconds = seconds) }
                if (remainingMs <= 0) {
                    finish(won = false)
                    break
                }
            }
        }
    }

    fun onCardTap(cardId: Int) {
        val state = _uiState.value
        if (state.status != PairsStatus.PLAYING || state.isPaused || state.isOver || busy) return
        val (next, outcome) = GameLogic.flip(board, cardId)
        if (outcome == FlipOutcome.IGNORED) return
        board = next
        _uiState.update { it.withBoard(board) }
        when (outcome) {
            FlipOutcome.FIRST -> sound?.play(Sfx.CLICK)
            FlipOutcome.MATCH -> {
                sound?.play(Sfx.MATCH)
                if (board.isCleared) viewModelScope.launch { finish(won = true) }
            }
            FlipOutcome.MISMATCH -> {
                sound?.play(Sfx.CLICK)
                busy = true
                flipBackJob = viewModelScope.launch {
                    delay(MISMATCH_DELAY_MS)
                    board = GameLogic.closeOpenCards(board)
                    busy = false
                    _uiState.update { it.withBoard(board) }
                }
            }
            FlipOutcome.IGNORED -> Unit
        }
    }

    private suspend fun finish(won: Boolean) {
        if (_uiState.value.isOver) return
        _uiState.update { it.copy(isPaused = false, isOver = true) }
        val matched = board.matchedPairs
        val total = board.totalPairs
        sound?.play(if (won) Sfx.WIN else Sfx.LOSE)
        val result = PairsFinished(level, matched, total, won)
        // Persist even if the screen is left mid-save, then expose the result for navigation.
        withContext(NonCancellable) { repository.savePairsBest(level, matched) }
        _uiState.update { it.copy(finished = result) }
        // The timer loop idles while isOver; stopping it here is safe for both paths because
        // there is no suspension point after this line.
        timerJob?.cancel()
    }

    fun pause() {
        val s = _uiState.value
        if (s.status == PairsStatus.PLAYING && !s.isOver) _uiState.update { it.copy(isPaused = true) }
    }

    fun resume() {
        _uiState.update { it.copy(isPaused = false) }
    }

    fun restart() {
        val cfg = config ?: return
        if (_uiState.value.status == PairsStatus.PLAYING) startLevel(cfg)
    }

    companion object {
        private const val TIMER_TICK_MS = 100L
        const val MISMATCH_DELAY_MS = 700L

        fun factory(level: Int, repository: GameRepository, sound: SoundManager?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = PairsViewModel(level, repository, sound) as T
            }
    }
}
