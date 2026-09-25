package com.arcticfishtrail.game.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arcticfishtrail.game.data.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loaded: Boolean = false,
    val soundEnabled: Boolean = true,
    /** One-shot message ("Progress reset"); consumed by the screen. */
    val message: String? = null,
)

class SettingsViewModel(private val repository: GameRepository) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(repository.gameData, message.asStateFlow()) { data, msg ->
        SettingsUiState(loaded = true, soundEnabled = data.settings.soundEnabled, message = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!repository.setSoundEnabled(enabled)) message.value = "Could not save the setting"
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            message.value = if (repository.resetAll()) "All progress has been reset" else "Reset failed, please try again"
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    companion object {
        fun factory(repository: GameRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(repository) as T
        }
    }
}
