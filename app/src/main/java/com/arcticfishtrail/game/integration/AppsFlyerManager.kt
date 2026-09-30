package com.arcticfishtrail.game.integration

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.arcticfishtrail.game.BuildConfig

object AppsFlyerManager {

    private const val TAG = "AppsFlyerManager"

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var isAttributionResolved: Boolean = false
        private set

    // Written on the main thread by the root composable, read on AppsFlyer's worker thread.
    @Volatile
    private var onAttributionResolved: (() -> Unit)? = null

    private lateinit var appContext: Context

    fun init(app: Application) {
        appContext = app.applicationContext

        if (IntegrationStorage.attributionSettled) {
            // A previous launch already got its answer; nothing to wait for.
            isAttributionResolved = true
        }

        // Holdback: ~1 in HoldbackPolicy.RATE installs never initialise AppsFlyer, as an
        // unattributed control. Decided once and persisted, so the same install stays on the
        // same side across launches.
        val holdback = IntegrationStorage.appsFlyerHoldback
            ?: HoldbackPolicy.roll().also { IntegrationStorage.appsFlyerHoldback = it }
        if (holdback) {
            Log.i(TAG, "Install is in the AppsFlyer holdback - SDK not initialised")
            // No SDK means no conversion callback will ever come, so settle now (this launch
            // and every later one) or routing would sit out the full attribution timeout.
            // The offer URL is tagged sub_id_15=holdback, so this is distinguishable from a
            // normal install whose AppsFlyer simply did not answer.
            isAttributionResolved = true
            IntegrationStorage.attributionSettled = true
            notifyResolved()
            return
        }

        val devKey = BuildConfig.APPSFLYER_DEV_KEY
        if (devKey.isBlank()) {
            Log.w(TAG, "APPSFLYER_DEV_KEY is empty - AppsFlyer disabled for this build")
            // Unblock the UI, but do not persist attributionSettled: a build with a real key
            // must wait for its own first callback.
            isAttributionResolved = true
            notifyResolved()
            return
        }

        val appsFlyer = AppsFlyerLib.getInstance()
        if (BuildConfig.DEBUG) appsFlyer.setDebugLog(true)
        appsFlyer.init(devKey, conversionListener, app)
        appsFlyer.start(app, devKey, object : AppsFlyerRequestListener {
            override fun onSuccess() {
                captureAppsFlyerId(app)
            }

            override fun onError(code: Int, message: String) {
                Log.w(TAG, "AppsFlyer start failed: code=$code, message=$message")
            }
        })
    }

    /**
     * The UID is not available synchronously after start(); it appears once the SDK has
     * talked to its backend, hence the calls from several callbacks. It is both the
     * `appsflyer_id` in the offer URL and the OneSignal external_id.
     */
    fun captureAppsFlyerId(context: Context) {
        val uid = runCatching { AppsFlyerLib.getInstance().getAppsFlyerUID(context) }
            .onFailure { Log.w(TAG, "getAppsFlyerUID failed", it) }
            .getOrNull()
        // null/empty means "not ready yet", never "there is no UID".
        if (uid.isNullOrBlank()) return
        IntegrationStorage.appsFlyerId = uid
        OneSignalManager.syncExternalId()
    }

    fun setOnAttributionResolved(callback: (() -> Unit)?) {
        onAttributionResolved = callback
        if (callback != null && isAttributionResolved) {
            // Resolved before the screen subscribed - deliver now instead of timing out.
            mainHandler.post { callback() }
        }
    }

    private val conversionListener = object : AppsFlyerConversionListener {

        override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
            // Stored verbatim - every key, campaign included. Merge rather than replace:
            // this also fires on re-attribution, whose payload may not be a superset.
            data?.let { IntegrationStorage.mergeAttribution(it) }
            captureAppsFlyerId(appContext)
            markResolved()
        }

        override fun onConversionDataFail(error: String?) {
            Log.w(TAG, "Conversion data failed: $error")
            captureAppsFlyerId(appContext)
            // A failure is still an answer: no attribution is coming, stop waiting.
            markResolved()
        }

        override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
            // Merge, not replace: this event carries deep-link parameters only.
            data?.let { IntegrationStorage.mergeAttribution(it) }
        }

        override fun onAttributionFailure(error: String?) {
            Log.w(TAG, "App open attribution failed: $error")
        }
    }

    private fun markResolved() {
        isAttributionResolved = true
        IntegrationStorage.attributionSettled = true
        notifyResolved()
    }

    private fun notifyResolved() {
        val callback = onAttributionResolved ?: return
        // AppsFlyer calls back on a background thread; the waiter is Compose state.
        mainHandler.post { callback() }
    }
}
