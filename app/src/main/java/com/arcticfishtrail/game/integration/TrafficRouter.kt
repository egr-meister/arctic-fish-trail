package com.arcticfishtrail.game.integration

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.arcticfishtrail.game.BuildConfig
import java.util.concurrent.Executors

object TrafficRouter {

    private const val TAG = "TrafficRouter"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Blocking: AccelerometerProbe.await() waits for the sampling window, so this must run on
     * a background thread. It is only ever called from route()'s executor below.
     */
    fun buildOfferUrl(): String = OfferUrlBuilder.build(
        baseUrl = BuildConfig.OFFER_BASE_URL,
        // The whole map. Hand-picking campaign/af_status/media_source here compiles, runs,
        // and quietly loses parameters.
        appsFlyerParams = IntegrationStorage.attributionParams,
        appsFlyerId = IntegrationStorage.appsFlyerId,
        deviceSignals = buildMap {
            put("sub12", DeviceSignals.battery)
            put("sub13", AccelerometerProbe.await())
            put("sub14", DeviceSignals.isTestEnvironment.toString())
            // Present only for held-back installs, so the tracker tells them apart from a
            // normal install whose AppsFlyer callback simply never arrived.
            if (IntegrationStorage.appsFlyerHoldback == true) {
                put(HoldbackPolicy.PARAM, HoldbackPolicy.MARKER)
            }
        }
    )

    /** Delivers the decision on the main thread. */
    fun route(probe: OfferProbe = HttpOfferProbe(), onResult: (RouteDecision) -> Unit) {
        // A pinned branch is answered without the network: instant relaunch, no flip-flopping.
        if (IntegrationStorage.whiteLocked) {
            deliver(RouteDecision.White, onResult)
            return
        }
        val cached = IntegrationStorage.cachedOfferUrl
        if (!cached.isNullOrBlank()) {
            deliver(RouteDecision.Black(cached), onResult)
            return
        }
        if (BuildConfig.OFFER_BASE_URL.isBlank()) {
            // Nothing to probe. White for now, pinned nothing.
            deliver(RouteDecision.White, onResult)
            return
        }

        executor.execute {
            val url = buildOfferUrl()
            val decision = runCatching { RoutingRules.decide(probe.probe(url), url) }
                .onFailure { Log.w(TAG, "Offer probe failed for $url", it) }
                .getOrNull()

            when (decision) {
                is RouteDecision.Black -> IntegrationStorage.cachedOfferUrl = decision.url
                RouteDecision.White -> IntegrationStorage.whiteLocked = true
                // Network/TLS error is not an answer about this install: White for this
                // launch, nothing pinned, the next launch asks again.
                null -> Unit
            }

            deliver(decision ?: RouteDecision.White, onResult)
        }
    }

    private fun deliver(decision: RouteDecision, onResult: (RouteDecision) -> Unit) {
        mainHandler.post { onResult(decision) }
    }
}
