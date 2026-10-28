package com.arcticfishtrail.game.integration

import java.util.concurrent.TimeUnit

/**
 * The two independent routing outcomes.
 *
 * `fromCache` is carried on Black so the WebView screen can tell a reused address (which,
 * if dead, may be re-routed once) from a freshly built campaign link (whose main-frame
 * 404 is the tracker's verdict and pins White).
 */
sealed interface RouteDecision {
    data object White : RouteDecision
    data class Black(val url: String, val fromCache: Boolean) : RouteDecision
}

object RoutingRules {

    const val NOT_FOUND = 404

    /**
     * 404 is the agreed "this install gets the app itself" answer. Everything else means
     * the offer exists: 200, a redirect chain that ended somewhere, even a 500 where the
     * server is simply having a moment and the WebView can retry.
     *
     * 403 is deliberately NOT White: it is Cloudflare or a WAF, not the tracker's decision,
     * and the White verdict is permanent - writing an install off on someone else's error
     * would strand it forever.
     */
    fun isWhite(mainFrameStatus: Int): Boolean = mainFrameStatus == NOT_FOUND
}

/**
 * The offer address is cached for a day so repeated launches cost one click, not one per
 * cold start. Unlike the White verdict it has a TTL and resets itself: addresses rot as
 * tokens expire and flows are switched off, and a cache with no expiry would strand the
 * install on a dead page.
 */
object OfferCache {

    val OFFER_TTL_MS: Long = TimeUnit.HOURS.toMillis(24)

    fun isFresh(savedAt: Long, now: Long, ttlMs: Long = OFFER_TTL_MS): Boolean {
        // 0 is also a record written before the timestamp existed: treat it as expired, so
        // an update heals installs that got stuck on an un-timed address.
        if (savedAt <= 0L) return false
        val age = now - savedAt
        // A negative age means the device clock moved back. Rebuilding the link is cheaper
        // than leaving the install on an eternal cache.
        if (age < 0L) return false
        return age < ttlMs
    }
}
