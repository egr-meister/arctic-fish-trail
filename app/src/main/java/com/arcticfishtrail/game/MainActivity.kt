package com.arcticfishtrail.game

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arcticfishtrail.game.ui.LocalAppContainer
import com.arcticfishtrail.game.ui.LocalSoundManager
import com.arcticfishtrail.game.ui.navigation.AppNavHost
import com.arcticfishtrail.game.ui.theme.ArcticFishTrailTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val container = (application as ArcticFishTrailApp).container
        setContent {
            ArcticFishTrailTheme {
                CompositionLocalProvider(
                    LocalAppContainer provides container,
                    LocalSoundManager provides container.soundManager,
                ) {
                    AppNavHost()
                }
            }
        }
    }
}
