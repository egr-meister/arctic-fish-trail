package com.arcticfishtrail.game

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arcticfishtrail.game.integration.DeepLinkRouter
import com.arcticfishtrail.game.integration.PushPayload
import com.arcticfishtrail.game.integration.PushService
import com.arcticfishtrail.game.ui.LocalAppContainer
import com.arcticfishtrail.game.ui.LocalSoundManager
import com.arcticfishtrail.game.ui.ArcticFishTrailRoot
import com.arcticfishtrail.game.ui.theme.ArcticFishTrailTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        // A tap on a push notification lands here; deliver its link to the WebView.
        consumeIntent(intent)
        requestNotificationPermission()

        val container = (application as ArcticFishTrailApp).container
        setContent {
            ArcticFishTrailTheme {
                CompositionLocalProvider(
                    LocalAppContainer provides container,
                    LocalSoundManager provides container.soundManager,
                ) {
                    ArcticFishTrailRoot()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    /**
     * Turns a push tap into a deep link, exactly once. The mark goes on the intent itself,
     * not a field on the Activity, so a recreation cannot replay the same push.
     */
    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(EXTRA_CONSUMED, false)) return
        // A launcher intent also has an action and a category - require our own marker or a
        // url extra (the latter is how the system delivers a background notification's data).
        val fromPush = intent.getBooleanExtra(PushService.EXTRA_FROM_PUSH, false) ||
            intent.hasExtra(PushPayload.KEY_URL)
        if (!fromPush) return
        intent.putExtra(EXTRA_CONSUMED, true)
        val url = intent.getStringExtra(PushPayload.KEY_URL)
        if (!url.isNullOrBlank()) DeepLinkRouter.handle(url)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            // Non-blocking: the FCM token arrives regardless, so a refusal breaks nothing.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }

    private companion object {
        const val EXTRA_CONSUMED = "arctic_push_consumed"
    }
}
