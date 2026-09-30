package com.arcticfishtrail.game.integration

/**
 * Assembles the offer URL from the base URL and everything AppsFlyer reported:
 *
 * `https://domain/{sub1}?{all AppsFlyer params}&campaign=...&sub1=...&subN=...&appsflyer_id=...`
 *
 * Built by string concatenation on purpose: android.net.Uri.Builder percent-encodes values
 * that are already encoded, so a campaign carrying `%26` would reach the tracker as `%2526`.
 */
object OfferUrlBuilder {

    private const val CAMPAIGN_KEY = "campaign"
    private const val SUB1_KEY = "sub1"
    private const val APPSFLYER_ID_KEY = "appsflyer_id"

    fun build(
        baseUrl: String,
        appsFlyerParams: Map<String, String>,
        appsFlyerId: String?,
        // Device signals (sub12..sub14). They override same-named campaign segments: a
        // campaign of 12+ parts produces its own sub12, and the URL must not carry it twice.
        deviceSignals: Map<String, String> = emptyMap()
    ): String {
        val base = baseUrl.trim()
        if (base.isEmpty()) return ""

        // Split without decoding - anything already encoded must pass through as-is.
        val hashIndex = base.indexOf('#')
        val fragment = if (hashIndex >= 0) base.substring(hashIndex) else ""
        val withoutFragment = if (hashIndex >= 0) base.substring(0, hashIndex) else base

        val questionIndex = withoutFragment.indexOf('?')
        val path = if (questionIndex >= 0) withoutFragment.substring(0, questionIndex) else withoutFragment
        val baseQuery = if (questionIndex >= 0) withoutFragment.substring(questionIndex + 1) else ""

        val params = LinkedHashMap<String, String>()
        parseQuery(baseQuery, params)
        // Every key AppsFlyer sent, in arrival order. Filtering to a known subset here is the
        // single most common way this integration silently loses attribution.
        params.putAll(appsFlyerParams)

        val campaign = appsFlyerParams[CAMPAIGN_KEY].orEmpty()
        val subs = CampaignParser.parse(campaign)

        // The tracker reads the raw campaign as well as the exploded parts, so it stays.
        if (campaign.isNotEmpty()) params[CAMPAIGN_KEY] = campaign

        // Parsed subs win over any sub1..subN AppsFlyer happened to send itself. Re-putting
        // an existing key replaces it in place: deterministic order, no duplicate keys.
        params.putAll(subs)

        // Device signals go in after the campaign subs, so a colliding sub12 from a long
        // campaign is replaced in place: deterministic order, no duplicate key.
        params.putAll(deviceSignals)

        // A blank id would read as "attributed to nothing" rather than "not known yet".
        if (!appsFlyerId.isNullOrBlank()) params[APPSFLYER_ID_KEY] = appsFlyerId.trim()

        val sub1 = subs[SUB1_KEY].orEmpty()
        val finalPath = if (sub1.isEmpty()) path else appendPathSegment(path, sub1)
        val query = CampaignParser.toQuery(params)

        return buildString {
            append(finalPath)
            if (query.isNotEmpty()) {
                append('?')
                append(query)
            }
            append(fragment)
        }
    }

    /**
     * Appends sub1 as a path segment after whatever path the base URL already has.
     * Idempotent: if the path already ends with that segment (build() fed its own output),
     * it is not added a second time.
     */
    private fun appendPathSegment(path: String, rawSegment: String): String {
        val segment = PercentEncoder.encodePathSegment(rawSegment)
        val trimmed = path.trimEnd('/')
        val schemeEnd = trimmed.indexOf("://")
        val hasPath = schemeEnd >= 0 && trimmed.indexOf('/', schemeEnd + 3) >= 0
        if (hasPath && trimmed.substringAfterLast('/') == segment) return trimmed
        return "$trimmed/$segment"
    }

    private fun parseQuery(query: String, into: LinkedHashMap<String, String>) {
        if (query.isEmpty()) return
        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            if (eq < 0) into[pair] = "" else into[pair.substring(0, eq)] = pair.substring(eq + 1)
        }
    }
}
