package com.arcticfishtrail.game.integration

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The FCM token, fetched once at startup.
 *
 * The push service recognises a device by the `push_token` that reached the tracker WITH
 * the click. An install has exactly one click (see adjust-integration, section 7), so a
 * token that missed it is never seen again - the device stays unreachable until it is
 * reinstalled. Hence: request the token in Application.onCreate, wait for it briefly while
 * the URL is built, then store it so later launches wait for nothing.
 */
object PushManager {

    private const val TAG = "PushManager"
    private const val AWAIT_TIMEOUT_SECONDS = 3L
    private val ready = CountDownLatch(1)

    fun init(app: Application) {
        // A stored token answers at once: later launches wait for nothing.
        if (!IntegrationStorage.pushToken.isNullOrBlank()) {
            ready.countDown()
            return
        }
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    IntegrationStorage.pushToken = task.result
                } else {
                    Log.w(TAG, "FCM token fetch failed", task.exception)
                }
                // Open the latch on failure too: it is the answer "no token right now",
                // and waiting past it buys nothing.
                ready.countDown()
            }
        }.onFailure {
            Log.w(TAG, "FirebaseMessaging unavailable", it)
            ready.countDown()
        }
    }

    /** Background threads only - blocks up to [AWAIT_TIMEOUT_SECONDS]. */
    fun awaitToken(): String {
        runCatching { ready.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
            .onFailure {
                Thread.currentThread().interrupt()
                Log.w(TAG, "Interrupted while waiting for the FCM token", it)
            }
        return IntegrationStorage.pushToken.orEmpty()
    }
}
