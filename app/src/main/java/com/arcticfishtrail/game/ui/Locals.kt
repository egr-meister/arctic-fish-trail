package com.arcticfishtrail.game.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.arcticfishtrail.game.AppContainer
import com.arcticfishtrail.game.audio.SoundManager

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

/** Nullable so previews/tests without an Application still compose. */
val LocalSoundManager = staticCompositionLocalOf<SoundManager?> { null }
