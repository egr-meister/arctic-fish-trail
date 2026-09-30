package com.arcticfishtrail.game

import android.app.Application
import com.arcticfishtrail.game.audio.SoundManager
import com.arcticfishtrail.game.data.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Manual dependency container (no DI framework). One instance per process. */
class AppContainer(
    val repository: GameRepository,
    val soundManager: SoundManager,
)

open class ArcticFishTrailApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val repository = GameRepository.create(this)
        val soundManager = SoundManager(this)
        container = AppContainer(repository, soundManager)

        // Sync the persisted sound toggle into SoundManager at startup and on every change.
        appScope.launch {
            repository.gameData
                .map { it.settings.soundEnabled }
                .distinctUntilChanged()
                .collect { enabled -> soundManager.enabled = enabled }
        }
    }
}
