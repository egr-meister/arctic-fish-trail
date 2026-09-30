package com.arcticfishtrail.game.integration

import com.arcticfishtrail.game.ArcticFishTrailApp

/**
 * Extends the game's own Application, so its AppContainer (repository, sound) is created
 * first and unchanged - MainActivity still casts to ArcticFishTrailApp.
 */
class ArcticFishTrailIntegrationApp : ArcticFishTrailApp() {

    override fun onCreate() {
        super.onCreate()
        // Order matters: storage before AppsFlyer (reads attributionSettled), the user agent
        // before anything can probe, OneSignal after AppsFlyer (external_id = AppsFlyer UID).
        IntegrationStorage.init(this)
        DeviceSignals.init(this)
        // Sampling takes a full second, so it must begin at launch - starting it where the
        // URL is built would block that thread for the whole window.
        AccelerometerProbe.start(this)
        UserAgentProvider.init(this)
        AppsFlyerManager.init(this)
        OneSignalManager.init(this)
    }
}
