package com.arcticfishtrail.game.integration

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Integration state on its own SharedPreferences file, so it can never collide with the
 * game's DataStore. [init] is called once, from the Application.
 */
object IntegrationStorage {

    private const val PREFS_NAME = "integration_prefs"
    private const val KEY_ATTRIBUTION = "attribution"
    private const val KEY_ATTRIBUTION_SETTLED = "attribution_settled"
    private const val KEY_APPSFLYER_ID = "appsflyer_id"
    private const val KEY_CACHED_OFFER_URL = "cached_offer_url"
    private const val KEY_WHITE_LOCKED = "white_locked"
    private const val KEY_APPSFLYER_HOLDBACK = "appsflyer_holdback"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- attribution ---------------------------------------------------------

    /** Full conversion data as JSON, exactly as stored. */
    val attribution: String?
        get() = prefs.getString(KEY_ATTRIBUTION, null)

    /** Replaces the stored conversion data wholesale. */
    fun saveAttribution(data: Map<String, Any?>) {
        writeAttribution(normalize(data))
    }

    /**
     * Adds to the stored conversion data. Later events (onAppOpenAttribution, re-attribution)
     * carry fewer keys than the install event; dropping the ones they omit would throw away
     * install attribution we already have.
     */
    fun mergeAttribution(data: Map<String, Any?>) {
        val merged = LinkedHashMap(attributionParams)
        merged.putAll(normalize(data))
        writeAttribution(merged)
    }

    fun attributionValue(key: String): String? = attributionParams[key]

    /** Everything stored, in the order it was written. */
    val attributionParams: LinkedHashMap<String, String>
        get() {
            val result = LinkedHashMap<String, String>()
            val raw = attribution ?: return result
            runCatching {
                val json = JSONObject(raw)
                // Android's JSONObject is backed by a LinkedHashMap: keys() keeps insertion order.
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    result[key] = json.optString(key, "")
                }
            }
            return result
        }

    val hasAttribution: Boolean get() = attributionParams.isNotEmpty()

    /**
     * True once AppsFlyer has answered at all - success or failure. Conversion data only
     * arrives on the install that carried it, so without this flag every later launch
     * would block on a callback that never fires.
     */
    var attributionSettled: Boolean
        get() = prefs.getBoolean(KEY_ATTRIBUTION_SETTLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ATTRIBUTION_SETTLED, value).apply()

    /**
     * The holdback decision for this install, or null before it has been made. Persisted so
     * it is rolled exactly once - a re-roll per launch would move a user in and out of the
     * holdback slice. Setting null is a no-op, so the decision can never be cleared by accident.
     */
    var appsFlyerHoldback: Boolean?
        get() = if (prefs.contains(KEY_APPSFLYER_HOLDBACK)) prefs.getBoolean(KEY_APPSFLYER_HOLDBACK, false) else null
        set(value) {
            if (value == null) return
            prefs.edit().putBoolean(KEY_APPSFLYER_HOLDBACK, value).apply()
        }

    var appsFlyerId: String?
        get() = prefs.getString(KEY_APPSFLYER_ID, null)
        set(value) {
            // Once we have a UID, an empty one arriving later must never replace it.
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_APPSFLYER_ID, value).apply()
        }

    // --- routing decision ----------------------------------------------------

    var cachedOfferUrl: String?
        get() = prefs.getString(KEY_CACHED_OFFER_URL, null)
        set(value) {
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_CACHED_OFFER_URL, value).apply()
        }

    val hasCachedOffer: Boolean get() = !cachedOfferUrl.isNullOrBlank()

    var whiteLocked: Boolean
        get() = prefs.getBoolean(KEY_WHITE_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_WHITE_LOCKED, value).apply()

    /** A branch has already been pinned, so startup can skip the network entirely. */
    val hasRouteDecision: Boolean get() = whiteLocked || hasCachedOffer

    // --- internals -----------------------------------------------------------

    private fun writeAttribution(params: Map<String, String>) {
        val json = JSONObject()
        params.forEach { (key, value) -> json.put(key, value) }
        prefs.edit().putString(KEY_ATTRIBUTION, json.toString()).apply()
    }

    /**
     * Conversion data is Map<String, Any?> - values may be numbers, booleans or null.
     * Everything present is stringified and kept, empty strings included, because the
     * tracker distinguishes "sent as empty" from "not sent". A real null is skipped rather
     * than stored as the four-character string "null".
     */
    private fun normalize(data: Map<String, Any?>): LinkedHashMap<String, String> {
        val result = LinkedHashMap<String, String>()
        data.forEach { (key, value) ->
            if (key.isBlank()) return@forEach
            if (value == null || value == JSONObject.NULL) return@forEach
            result[key] = value.toString()
        }
        return result
    }
}
