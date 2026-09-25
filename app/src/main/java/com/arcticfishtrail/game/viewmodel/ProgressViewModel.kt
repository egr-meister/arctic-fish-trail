package com.arcticfishtrail.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcticfishtrail.game.data.GameData
import com.arcticfishtrail.game.data.GameRepository
import com.arcticfishtrail.game.domain.GameLogic
import com.arcticfishtrail.game.domain.QuizContent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class QuizCategoryUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconItem: Int,
    val best: Int?,
    val total: Int,
)

data class LevelUi(
    val level: Int,
    val pairs: Int,
    val seconds: Int,
    val best: Int,
    val unlocked: Boolean,
    val completed: Boolean,
)

/** Immutable snapshot of progress for Menu / Quiz categories / Levels / Results summary. */
data class ProgressUiState(
    val loaded: Boolean = false,
    val categories: List<QuizCategoryUi> = emptyList(),
    val levels: List<LevelUi> = emptyList(),
    val completedLevels: Int = 0,
    val totalLevels: Int = GameLogic.LEVEL_COUNT,
)

class ProgressViewModel(repository: GameRepository) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = repository.gameData
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    companion object {
        fun toUiState(data: GameData): ProgressUiState = ProgressUiState(
            loaded = true,
            categories = QuizContent.categories.map { c ->
                QuizCategoryUi(
                    id = c.id,
                    title = c.title,
                    subtitle = c.subtitle,
                    iconItem = c.iconItem,
                    best = data.quizBest[c.id]?.coerceIn(0, c.questions.size),
                    total = c.questions.size,
                )
            },
            levels = GameLogic.levels.map { cfg ->
                LevelUi(
                    level = cfg.level,
                    pairs = cfg.pairs,
                    seconds = cfg.seconds,
                    best = GameLogic.bestPairs(data.pairsBest, cfg.level).coerceIn(0, cfg.pairs),
                    unlocked = GameLogic.isLevelUnlocked(cfg.level, data.pairsBest),
                    completed = GameLogic.isLevelCompleted(cfg.level, data.pairsBest),
                )
            },
            completedLevels = GameLogic.completedLevelCount(data.pairsBest),
        )

        fun factory(repository: GameRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ProgressViewModel(repository) as T
        }
    }
}
