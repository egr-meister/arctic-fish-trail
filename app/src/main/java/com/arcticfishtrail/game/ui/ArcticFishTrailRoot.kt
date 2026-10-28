package com.arcticfishtrail.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arcticfishtrail.game.integration.AdjustManager
import com.arcticfishtrail.game.integration.DeepLinkRouter
import com.arcticfishtrail.game.integration.IntegrationStorage
import com.arcticfishtrail.game.integration.RouteDecision
import com.arcticfishtrail.game.integration.TrafficRouter
import com.arcticfishtrail.game.ui.components.GameBackground
import com.arcticfishtrail.game.ui.components.LoadingHint
import com.arcticfishtrail.game.ui.navigation.AppNavHost
import com.arcticfishtrail.game.ui.screens.OfferWebViewScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val ATTRIBUTION_TIMEOUT_MS = 8_000L

/**
 * Startup orchestrator. Decides, once per install, whether this device sees the game
 * (White) or the offer (Black). The WebView opens the campaign link itself and its client
 * turns the tracker's answer into the verdict; this composable only reacts to it.
 *
 * A push deep link outranks the routing decision and pins/changes nothing.
 */
@Composable
fun ArcticFishTrailRoot() {
    val scope = rememberCoroutineScope()

    var deepLinkUrl by remember { mutableStateOf<String?>(null) }
    var deepLinkFinal by remember { mutableStateOf<String?>(null) }

    var gate by remember { mutableStateOf<RouteDecision?>(null) }
    var painted by remember { mutableStateOf(false) }
    var reroutes by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        DeepLinkRouter.setHandler { url -> deepLinkUrl = url }
        onDispose { DeepLinkRouter.clearHandler() }
    }

    // Route, and re-route when a cached address turns out dead. Attribution is awaited only
    // when nothing is pinned yet (the first install): the link is worthless without source
    // data, but a splash that never ends is worse, so the wait is bounded.
    LaunchedEffect(reroutes) {
        if (!withContext(Dispatchers.IO) { IntegrationStorage.hasRouteDecision() }) {
            withTimeoutOrNull(ATTRIBUTION_TIMEOUT_MS + 2_000L) {
                // Attribution and adid are awaited in parallel, not in sequence: an organic
                // verdict can land in a second or two, and if we built the URL right then the
                // adid (which arrives with the session response) would miss the single click.
                // Both were kicked off back in Application.onCreate / init.
                coroutineScope {
                    launch {
                        suspendCancellableCoroutine { continuation ->
                            AdjustManager.awaitAttribution(ATTRIBUTION_TIMEOUT_MS) {
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                        }
                    }
                    launch(Dispatchers.IO) { AdjustManager.awaitAdid() }
                }
            }
        }
        TrafficRouter.route { decision -> gate = decision }
    }

    // A pushed link is opened as sent, with only push_token/push_id appended (off-thread:
    // appending waits on the token latch).
    LaunchedEffect(deepLinkUrl) {
        val url = deepLinkUrl
        deepLinkFinal = if (url.isNullOrBlank()) null
        else withContext(Dispatchers.IO) { TrafficRouter.appendPushParams(url) }
    }

    val currentDeepLink = deepLinkFinal
    val decision = gate
    when {
        // A deep link is an explicit instruction: show it, change no verdict, pin nothing.
        currentDeepLink != null -> OfferWebViewScreen(
            url = currentDeepLink,
            fromCache = false,
            onWhiteVerdict = {},
            onOfferCommitted = {},
            onReroute = {},
            onTransientWhite = {}
        )

        decision is RouteDecision.Black -> Box(Modifier.fillMaxSize()) {
            // Keyed by url AND source: a rebuilt campaign link can match a dead cached one
            // byte for byte, yet it is a different load with different error meaning.
            key(decision.url, decision.fromCache) {
                OfferWebViewScreen(
                    url = decision.url,
                    fromCache = decision.fromCache,
                    onWhiteVerdict = {
                        IntegrationStorage.lockWhite()
                        gate = RouteDecision.White
                    },
                    onOfferCommitted = { url ->
                        scope.launch(Dispatchers.IO) { IntegrationStorage.saveOfferUrl(url) }
                        painted = true
                    },
                    onReroute = {
                        scope.launch(Dispatchers.IO) { IntegrationStorage.clearOfferUrl() }
                        painted = false
                        reroutes++
                    },
                    onTransientWhite = {
                        // White for this launch only: nothing is locked, nothing pinned.
                        gate = RouteDecision.White
                    }
                )
            }
            // The same loading screen stays over the offer until a page renders, so a White
            // install never sees the offer flash past on the way to its 404.
            if (!painted) GameBackground { LoadingHint() }
        }

        decision is RouteDecision.White -> AppNavHost()
        else -> GameBackground { LoadingHint() }
    }
}
