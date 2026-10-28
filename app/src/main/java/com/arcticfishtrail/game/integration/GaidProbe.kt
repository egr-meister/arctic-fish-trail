package com.arcticfishtrail.game.integration

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The Google Advertising ID (GAID), read once at startup.
 *
 * `gps_adid` is NOT the Adjust `adid` - it is a different identifier, and it is the primary
 * key by which Adjust's S2S postbacks and the offer's partner match a conversion back to this
 * install on Android (the campaign maps it to sub_id_7). So it has to ride in the single click
 * ALWAYS, alongside `adid`; a missing one is lost matching that only shows up a week later in
 * deposits that never reconcile.
 *
 * Reading the GAID is blocking and talks to Google Play Services, so it starts on a background
 * thread at launch (like [PushManager] and [AccelerometerProbe]) and the URL builder waits on
 * its latch briefly, off the main thread.
 */
object GaidProbe {

    private const val TAG = "GaidProbe"
    private const val AWAIT_TIMEOUT_SECONDS = 3L
    private const val ZEROED = "00000000-0000-0000-0000-000000000000"

    private val started = AtomicBoolean(false)
    private val ready = CountDownLatch(1)

    /** Called from Application.onCreate: reading the GAID blocks, so begin early. */
    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        // A GAID already read answers at once: later launches wait for nothing.
        if (!IntegrationStorage.gpsAdid.isNullOrBlank()) {
            ready.countDown()
            return
        }
        val app = context.applicationContext
        Thread {
            runCatching {
                val info = AdvertisingIdClient.getAdvertisingIdInfo(app)
                val id = info.id
                // LAT on, or a zeroed id, is not a key: store nothing, but the key still goes
                // out empty (so "no GAID" stays distinguishable from "not ready yet").
                if (!info.isLimitAdTrackingEnabled && !id.isNullOrBlank() && id != ZEROED) {
                    IntegrationStorage.gpsAdid = id
                }
            }.onFailure {
                Log.w(TAG, "AdvertisingIdClient unavailable", it)
            }
            // Open the latch on failure too: waiting for the GAID any longer buys nothing.
            ready.countDown()
        }.start()
    }

    /** Background threads only - blocks up to [AWAIT_TIMEOUT_SECONDS]. */
    fun await(): String {
        if (!started.get()) return IntegrationStorage.gpsAdid.orEmpty()
        runCatching { ready.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
            .onFailure {
                Thread.currentThread().interrupt()
                Log.w(TAG, "Interrupted while waiting for the GAID", it)
            }
        return IntegrationStorage.gpsAdid.orEmpty()
    }
}
