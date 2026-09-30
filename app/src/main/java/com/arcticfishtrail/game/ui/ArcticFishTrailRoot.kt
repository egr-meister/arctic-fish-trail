package com.arcticfishtrail.game.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arcticfishtrail.game.integration.AppsFlyerManager
import com.arcticfishtrail.game.integration.DeepLinkRouter
import com.arcticfishtrail.game.integration.IntegrationStorage
import com.arcticfishtrail.game.integration.OneSignalManager
import com.arcticfishtrail.game.integration.RouteDecision
import com.arcticfishtrail.game.integration.TrafficRouter
import com.arcticfishtrail.game.ui.components.GameBackground
import com.arcticfishtrail.game.ui.components.LoadingHint
import com.arcticfishtrail.game.ui.navigation.AppNavHost
import com.arcticfishtrail.game.ui.screens.OfferWebViewScreen
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val ATTRIBUTION_TIMEOUT_MS = 8_000L

/** Decides between the game (White) and the offer WebView (Black). */
@Composable
fun ArcticFishTrailRoot() {
    var deepLinkUrl by remember { mutableStateOf<String?>(null) }
    var routeGate by remember { mutableStateOf<RouteDecision?>(null) }

    DisposableEffect(Unit) {
        DeepLinkRouter.setHandler { url -> deepLinkUrl = url }
        onDispose { DeepLinkRouter.clearHandler() }
    }

    LaunchedEffect(Unit) {
        // On a first launch the offer URL is worthless until conversion data lands, so wait -
        // but bounded, because AppsFlyer may never answer.
        if (!IntegrationStorage.hasRouteDecision) {
            withTimeoutOrNull(ATTRIBUTION_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    AppsFlyerManager.setOnAttributionResolved {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    continuation.invokeOnCancellation {
                        AppsFlyerManager.setOnAttributionResolved(null)
                    }
                }
            }
            AppsFlyerManager.setOnAttributionResolved(null)
        }
        // TrafficRouter reads the full stored AppsFlyer map when it builds the URL.
        TrafficRouter.route { decision ->
            routeGate = decision
            // Only the offer side asks for notifications; the game itself never needs them.
            if (decision is RouteDecision.Black) OneSignalManager.requestPermission()
        }
    }

    val currentDeepLink = deepLinkUrl
    val gate = routeGate
    when {
        // A push link is an explicit instruction and outranks the routing decision.
        currentDeepLink != null -> OfferWebViewScreen(currentDeepLink)
        gate is RouteDecision.Black -> OfferWebViewScreen(gate.url)
        gate is RouteDecision.White -> AppNavHost()
        else -> GameBackground { LoadingHint() }
    }
}
