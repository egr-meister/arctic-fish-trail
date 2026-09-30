package com.arcticfishtrail.game.integration

sealed interface RouteDecision {
    data object White : RouteDecision
    data class Black(val url: String) : RouteDecision
}

data class ProbeResult(
    val statusCode: Int,
    val finalUrl: String?
)

/** Injection seam: the real implementation talks to the network, a fake need not. */
fun interface OfferProbe {
    fun probe(url: String): ProbeResult
}

object RoutingRules {

    private const val NOT_FOUND = 404

    /**
     * 404 is the agreed "this install gets the app itself" answer. Any other HTTP status -
     * 200, a redirect chain that ended somewhere, even a 5xx - means the offer exists.
     */
    fun decide(result: ProbeResult, requestedUrl: String): RouteDecision =
        if (result.statusCode == NOT_FOUND) {
            RouteDecision.White
        } else {
            RouteDecision.Black(result.finalUrl?.takeIf { it.isNotBlank() } ?: requestedUrl)
        }
}
