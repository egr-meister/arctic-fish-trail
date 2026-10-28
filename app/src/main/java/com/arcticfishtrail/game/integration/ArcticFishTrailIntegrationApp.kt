package com.arcticfishtrail.game.integration

import com.arcticfishtrail.game.ArcticFishTrailApp

/**
 * Extends the game's own Application, so its AppContainer (repository, sound) is created
 * first and unchanged - MainActivity still casts to ArcticFishTrailApp.
 *
 * Order matters: storage first (everything else reads from it); the accelerometer and the
 * push token are started here, not where the URL is built, because each needs a window and
 * building the URL would otherwise block for it.
 */
class ArcticFishTrailIntegrationApp : ArcticFishTrailApp() {

    override fun onCreate() {
        super.onCreate()
        IntegrationStorage.init(this)
        DeviceSignals.init(this)
        // Sampling takes a full second, so it must begin at launch - starting it where the
        // URL is built would block that thread for the whole window.
        AccelerometerProbe.start(this)
        AdjustManager.init(this)
        // The token must exist before the single click, so request it at launch.
        PushManager.init(this)
    }
}
