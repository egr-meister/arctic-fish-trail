package com.arcticfishtrail.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import com.arcticfishtrail.game.R
import java.util.Collections

/** Short sound effects. Synthesized placeholder WAVs live in res/raw and can be replaced 1:1. */
enum class Sfx(@RawRes val resId: Int) {
    CLICK(R.raw.sfx_click),
    CORRECT(R.raw.sfx_correct),
    WRONG(R.raw.sfx_wrong),
    MATCH(R.raw.sfx_match),
    WIN(R.raw.sfx_win),
    LOSE(R.raw.sfx_lose),
}

/**
 * SoundPool wrapper created once in the Application. [enabled] is synced from DataStore at
 * startup and live whenever the Settings toggle changes. All calls are failure-tolerant.
 */
class SoundManager(context: Context) {

    @Volatile
    var enabled: Boolean = false

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds = HashMap<Sfx, Int>()
    private val loadedIds: MutableSet<Int> = Collections.synchronizedSet(HashSet())

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedIds.add(sampleId)
        }
        val appContext = context.applicationContext
        Sfx.entries.forEach { sfx ->
            runCatching { soundPool.load(appContext, sfx.resId, 1) }
                .onSuccess { id -> if (id != 0) soundIds[sfx] = id }
        }
    }

    fun play(sfx: Sfx) {
        if (!enabled) return
        val id = soundIds[sfx] ?: return
        if (id !in loadedIds) return
        runCatching { soundPool.play(id, 1f, 1f, 1, 0, 1f) }
    }

    fun release() {
        runCatching { soundPool.release() }
    }
}
