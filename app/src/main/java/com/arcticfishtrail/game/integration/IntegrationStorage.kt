package com.arcticfishtrail.game.integration

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.UUID

/**
 * A singleton over its own SharedPreferences file, so integration keys can never collide
 * with anything the game already stores. `init(context)` runs once, from the Application.
 *
 * Holds the whole attribution map, the Adjust `adid`, the push token and a stable push
 * id, the cached offer address with the time it was written, and the permanent White
 * verdict.
 */
object IntegrationStorage {

    private const val PREFS_NAME = "integration_prefs"
    private const val KEY_ATTRIBUTION = "attribution"
    private const val KEY_ADID = "adid"
    private const val KEY_GPS_ADID = "gps_adid"
    private const val KEY_PUSH_TOKEN = "push_token"
    private const val KEY_PUSH_ID = "push_id"
    private const val KEY_CACHED_OFFER_URL = "cached_offer_url"
    private const val KEY_OFFER_SAVED_AT = "offer_saved_at"
    private const val KEY_WHITE_LOCKED = "white_locked"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- attribution ---------------------------------------------------------

    /** Replaces the stored attribution wholesale. */
    fun saveAttribution(data: Map<String, Any?>) {
        writeAttribution(normalize(data))
    }

    /**
     * Adds to the stored attribution. Adjust can re-attribute, and those payloads are not
     * guaranteed to be supersets of the first, so dropping the keys they omit would throw
     * away attribution already stored.
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
            val raw = prefs.getString(KEY_ATTRIBUTION, null) ?: return result
            runCatching {
                val json = JSONObject(raw)
                // Android's JSONObject is backed by a LinkedHashMap, so keys() preserves
                // insertion order and sub1..subN stay in their original sequence.
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    result[key] = json.optString(key, "")
                }
            }
            return result
        }

    val hasAttribution: Boolean get() = attributionParams.isNotEmpty()

    // --- identifiers ---------------------------------------------------------

    /** Adjust adid. A blank read means "not ready yet", so it must not clear a stored one. */
    var adid: String?
        get() = prefs.getString(KEY_ADID, null)
        set(value) {
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_ADID, value).apply()
        }

    /**
     * Google Advertising ID (GAID) - a DIFFERENT identifier from the Adjust adid, and the
     * primary key the Adjust S2S postbacks match a conversion to this install by on Android.
     * A blank read means "not ready yet" (or LAT), so it must never clear a stored one.
     */
    var gpsAdid: String?
        get() = prefs.getString(KEY_GPS_ADID, null)
        set(value) {
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_GPS_ADID, value).apply()
        }

    /** The FCM token. Blank means "not ready yet"; it must never replace a stored token. */
    var pushToken: String?
        get() = prefs.getString(KEY_PUSH_TOKEN, null)
        set(value) {
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_PUSH_TOKEN, value).apply()
        }

    /**
     * A stable per-install id the push service can address alongside the FCM token. Drawn
     * once on first read and then kept: it must be identical across launches so the id that
     * reached the tracker in the one click still points at this install later.
     */
    val pushId: String
        get() {
            prefs.getString(KEY_PUSH_ID, null)?.let { return it }
            val fresh = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_PUSH_ID, fresh).apply()
            return fresh
        }

    // --- routing: offer address (24h TTL) ------------------------------------

    /** The cached address if it is still fresh, otherwise null. */
    fun freshOfferUrl(now: Long = System.currentTimeMillis()): String? {
        val url = prefs.getString(KEY_CACHED_OFFER_URL, null)
        if (url.isNullOrBlank()) return null
        val savedAt = prefs.getLong(KEY_OFFER_SAVED_AT, 0L)
        return if (OfferCache.isFresh(savedAt, now)) url else null
    }

    fun saveOfferUrl(url: String, now: Long = System.currentTimeMillis()) {
        if (url.isBlank()) return
        prefs.edit()
            .putString(KEY_CACHED_OFFER_URL, url)
            .putLong(KEY_OFFER_SAVED_AT, now)
            .apply()
    }

    /** Clears ONLY the cached address. The White verdict is deliberately untouched. */
    fun clearOfferUrl() {
        prefs.edit()
            .remove(KEY_CACHED_OFFER_URL)
            .remove(KEY_OFFER_SAVED_AT)
            .apply()
    }

    // --- routing: White verdict (permanent) ----------------------------------

    /**
     * Once the tracker has answered 404, this device opens White until it is reinstalled.
     * There is deliberately no time parameter and no reset: irreversibility is the point of
     * this flag, not an oversight. The only way to clear it is Clear data or a reinstall.
     */
    val isWhiteLocked: Boolean get() = prefs.getBoolean(KEY_WHITE_LOCKED, false)

    fun lockWhite() {
        prefs.edit().putBoolean(KEY_WHITE_LOCKED, true).apply()
    }

    /** A branch is already pinned, so startup can skip attribution and the network. */
    fun hasRouteDecision(now: Long = System.currentTimeMillis()): Boolean =
        isWhiteLocked || freshOfferUrl(now) != null

    // --- internals -----------------------------------------------------------

    private fun writeAttribution(params: Map<String, String>) {
        val json = JSONObject()
        params.forEach { (key, value) -> json.put(key, value) }
        prefs.edit().putString(KEY_ATTRIBUTION, json.toString()).apply()
    }

    /**
     * Attribution values may arrive as numbers, booleans or null. Everything present is
     * stringified and kept, empty strings included, because the tracker distinguishes
     * "sent as empty" from "not sent". A real null is skipped rather than stored: "null"
     * is a four-character value, not an absence.
     */
    private fun normalize(data: Map<String, Any?>): LinkedHashMap<String, String> {
        val result = LinkedHashMap<String, String>()
        data.forEach { (key, value) ->
            if (key.isBlank()) return@forEach
            if (value == null) return@forEach
            result[key] = value.toString()
        }
        return result
    }
}
