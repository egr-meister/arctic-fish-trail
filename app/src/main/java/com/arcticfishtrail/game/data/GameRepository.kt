package com.arcticfishtrail.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "arctic_fish_trail")

/**
 * The single local source of truth. Stores [GameData] as one serialized JSON string.
 * Created once in the Application and shared through [com.arcticfishtrail.game.AppContainer].
 */
class GameRepository(private val dataStore: DataStore<Preferences>) {

    /** Observes the stored data. Read errors and bad JSON surface as defaults, never as crashes. */
    val gameData: Flow<GameData> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> GameSerializer.decode(prefs[KEY_GAME_DATA]) }
        .distinctUntilChanged()

    suspend fun current(): GameData = try {
        gameData.first()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        GameData()
    }

    /** Keeps the best (highest) score for a quiz category. */
    suspend fun saveQuizBest(categoryId: String, score: Int): Boolean = update { data ->
        val previous = data.quizBest[categoryId] ?: 0
        if (score > previous) data.copy(quizBest = data.quizBest + (categoryId to score)) else data
    }

    /** Keeps the best (highest) number of matched pairs for a level. */
    suspend fun savePairsBest(level: Int, matchedPairs: Int): Boolean = update { data ->
        val key = level.toString()
        val previous = data.pairsBest[key] ?: 0
        if (matchedPairs > previous) data.copy(pairsBest = data.pairsBest + (key to matchedPairs)) else data
    }

    suspend fun setSoundEnabled(enabled: Boolean): Boolean = update { data ->
        data.copy(settings = data.settings.copy(soundEnabled = enabled))
    }

    /** Wipes all progress and settings back to defaults. */
    suspend fun resetAll(): Boolean = update { GameData() }

    private suspend fun update(transform: (GameData) -> GameData): Boolean = try {
        dataStore.edit { prefs ->
            val current = GameSerializer.decode(prefs[KEY_GAME_DATA])
            prefs[KEY_GAME_DATA] = GameSerializer.encode(transform(current))
        }
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    companion object {
        val KEY_GAME_DATA = stringPreferencesKey("game_data_json")

        fun create(context: Context): GameRepository = GameRepository(context.applicationContext.gameDataStore)
    }
}
