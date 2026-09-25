package com.arcticfishtrail.game.data

import kotlinx.serialization.json.Json

/**
 * Context-free JSON encode/decode for [GameData]. Pure Kotlin → unit-testable on the JVM.
 *
 * Decoding NEVER throws: null, empty, blank, corrupted or wrongly-typed JSON all fall back to
 * defaults. Unknown keys are ignored and missing fields take their default values.
 */
object GameSerializer {

    /** Upper bound for any stored counter; protects against absurd values in tampered data. */
    private const val MAX_STORED_VALUE = 1_000

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    fun encode(data: GameData): String = json.encodeToString(GameData.serializer(), data)

    fun decode(raw: String?): GameData {
        if (raw.isNullOrBlank()) return GameData()
        return try {
            sanitize(json.decodeFromString(GameData.serializer(), raw))
        } catch (e: Exception) {
            // SerializationException / IllegalArgumentException / anything unexpected → defaults
            GameData()
        }
    }

    /** Drops blank keys and clamps values into a sane range. */
    fun sanitize(data: GameData): GameData = data.copy(
        quizBest = data.quizBest
            .filterKeys { it.isNotBlank() }
            .mapValues { (_, v) -> v.coerceIn(0, MAX_STORED_VALUE) },
        pairsBest = data.pairsBest
            .filterKeys { key -> key.toIntOrNull() != null }
            .mapValues { (_, v) -> v.coerceIn(0, MAX_STORED_VALUE) },
    )
}
