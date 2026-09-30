package com.arcticfishtrail.game.integration

import android.app.Application
import android.util.Log
import com.arcticfishtrail.game.BuildConfig
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object OneSignalManager {

    private const val TAG = "OneSignalManager"

    @Volatile
    private var initialized = false

    /** Must run after AppsFlyerManager.init - the external_id is the AppsFlyer UID. */
    fun init(app: Application) {
        val appId = BuildConfig.ONESIGNAL_APP_ID
        if (appId.isBlank()) {
            Log.w(TAG, "ONESIGNAL_APP_ID is empty - OneSignal disabled for this build")
            return
        }
        if (BuildConfig.DEBUG) OneSignal.Debug.logLevel = LogLevel.VERBOSE

        OneSignal.initWithContext(app, appId)
        initialized = true

        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val url = event.notification.additionalData?.optString("url").orEmpty()
                if (url.isNotBlank()) DeepLinkRouter.handle(url)
            }
        })

        // A UID captured on an earlier launch is already available - link it right away.
        syncExternalId()
    }

    /** OneSignal external_id = AppsFlyer ID. Safe to call before init or without a UID. */
    fun syncExternalId() {
        if (!initialized) return
        val appsFlyerId = IntegrationStorage.appsFlyerId
        if (appsFlyerId.isNullOrBlank()) return
        runCatching { OneSignal.login(appsFlyerId) }
            .onFailure { Log.w(TAG, "OneSignal.login failed", it) }
    }

    /** requestPermission is suspend in OneSignal 5.x, so it must run in a coroutine. */
    fun requestPermission() {
        if (!initialized) return
        CoroutineScope(Dispatchers.Main).launch {
            runCatching {
                OneSignal.Notifications.requestPermission(true)
            }
        }
    }
}
