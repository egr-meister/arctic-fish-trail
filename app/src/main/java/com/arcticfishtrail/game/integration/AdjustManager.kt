package com.arcticfishtrail.game.integration

import android.app.Application
import android.content.Context
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAttribution
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.arcticfishtrail.game.BuildConfig

/**
 * Attribution via the Adjust SDK (v5).
 *
 * v5 differs from v4 where it is easy to trip: initialisation is `Adjust.initSdk(config)`,
 * not `Adjust.onCreate`, and the Activity lifecycle callbacks are gone - sessions are
 * counted by the SDK itself.
 */
object AdjustManager {

    private const val TAG = "AdjustManager"

    private lateinit var appContext: Context

    fun init(app: Application) {
        appContext = app.applicationContext

        val token = BuildConfig.ADJUST_APP_TOKEN
        if (token.isBlank()) {
            // A build without a tracker still runs: White works, the offer opens unattributed.
            Log.w(TAG, "ADJUST_APP_TOKEN is empty - attribution disabled for this build")
            return
        }

        val environment =
            if (BuildConfig.DEBUG) AdjustConfig.ENVIRONMENT_SANDBOX
            else AdjustConfig.ENVIRONMENT_PRODUCTION

        val config = AdjustConfig(app, token, environment)
        config.setLogLevel(if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN)

        // Set BEFORE initSdk, or the first attribution passes the listener by. A plain
        // lambda is the correct SAM form here - the "override fun" variant printed in some
        // Adjust docs does not compile in Kotlin.
        config.setOnAttributionChangedListener { attribution ->
            attribution?.let { IntegrationStorage.mergeAttribution(it.toMap()) }
        }

        Adjust.initSdk(config)

        // adid is not available synchronously; it arrives on this callback.
        Adjust.getAdid { adid -> IntegrationStorage.adid = adid }
    }

    /**
     * Waits a bounded time for attribution. Adjust provides this out of the box, so no
     * CountDownLatch of our own: getAttributionWithTimeout returns null if nothing arrived.
     */
    fun awaitAttribution(timeoutMs: Long, onDone: () -> Unit) {
        if (!::appContext.isInitialized || BuildConfig.ADJUST_APP_TOKEN.isBlank()) {
            // No SDK to wait for - do not stall startup.
            onDone()
            return
        }
        runCatching {
            Adjust.getAttributionWithTimeout(appContext, timeoutMs) { attribution ->
                attribution?.let { IntegrationStorage.mergeAttribution(it.toMap()) }
                onDone()
            }
        }.onFailure {
            Log.w(TAG, "getAttributionWithTimeout failed", it)
            onDone()
        }
    }

    /**
     * AdjustAttribution is a plain object, not a map. Spread it into fields and keep every
     * non-null one. A null means the field was absent - "null" is a four-character string,
     * not an absence - while an empty string is kept, because the offer side tells "sent
     * empty" from "not sent".
     */
    private fun AdjustAttribution.toMap(): Map<String, String?> = linkedMapOf(
        "tracker_token" to trackerToken,
        "tracker_name" to trackerName,
        "network" to network,
        "campaign" to campaign,
        "adgroup" to adgroup,
        "creative" to creative,
        "click_label" to clickLabel
    )
}
