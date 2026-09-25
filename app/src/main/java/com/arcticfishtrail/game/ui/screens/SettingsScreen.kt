package com.arcticfishtrail.game.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcticfishtrail.game.R
import com.arcticfishtrail.game.ui.Assets
import com.arcticfishtrail.game.ui.LocalAppContainer
import com.arcticfishtrail.game.ui.components.BackIconButton
import com.arcticfishtrail.game.ui.components.GameBackground
import com.arcticfishtrail.game.ui.components.GameTopBar
import com.arcticfishtrail.game.ui.components.GlassPanel
import com.arcticfishtrail.game.ui.components.LoadingHint
import com.arcticfishtrail.game.ui.components.PlateButton
import com.arcticfishtrail.game.ui.theme.FrostWhite
import com.arcticfishtrail.game.ui.theme.GlacierBlue
import com.arcticfishtrail.game.ui.theme.IceTeal
import com.arcticfishtrail.game.ui.theme.PolarNight
import com.arcticfishtrail.game.ui.theme.SalmonCoral
import com.arcticfishtrail.game.ui.theme.SlateIce
import com.arcticfishtrail.game.ui.theme.SunGold
import com.arcticfishtrail.game.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container.repository))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeMessage()
        }
    }

    GameBackground {
        Column(Modifier.fillMaxSize()) {
            GameTopBar(title = "Settings", leading = { BackIconButton(onBack) })
            if (!state.loaded) {
                LoadingHint()
                return@Column
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GlassPanel(modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .toggleable(
                                value = state.soundEnabled,
                                role = Role.Switch,
                                onValueChange = vm::setSoundEnabled,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(Assets.item(Assets.ITEM_FISH_FINDER)),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("Sound effects", color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (state.soundEnabled) "On" else "Off",
                                color = if (state.soundEnabled) IceTeal else SlateIce,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Switch(
                            checked = state.soundEnabled,
                            onCheckedChange = null, // handled by the whole row (bigger target)
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PolarNight,
                                checkedTrackColor = IceTeal,
                                uncheckedThumbColor = SlateIce,
                                uncheckedTrackColor = GlacierBlue,
                            ),
                        )
                    }
                }

                val plateMod = Modifier.fillMaxWidth().widthIn(max = 480.dp)
                PlateButton("Privacy Policy", { showPrivacy = true }, plateMod)
                PlateButton("Rate the App", { openStorePage(context) }, plateMod)
                PlateButton("Reset Progress", { showResetDialog = true }, plateMod)

                Spacer(Modifier.height(8.dp))
                Image(
                    painter = painterResource(Assets.mascotC),
                    contentDescription = "The blue arctic fish",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(90.dp),
                )
                Text(
                    "Arctic Fish Trail · fully offline",
                    color = SlateIce,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = PolarNight,
            titleContentColor = SunGold,
            textContentColor = FrostWhite,
            title = { Text("Reset all progress?") },
            text = { Text("Best quiz scores, cleared levels and settings will be erased. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    vm.resetAll()
                }) { Text("Reset", color = SalmonCoral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel", color = IceTeal) }
            },
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            containerColor = PolarNight,
            titleContentColor = SunGold,
            textContentColor = FrostWhite,
            title = { Text("Privacy") },
            text = {
                Text(
                    "Arctic Fish Trail works fully offline. It does not collect, share or transmit any " +
                        "personal data, has no ads, analytics or accounts, and requests no permissions. " +
                        "Your scores and settings are stored only on this device and can be erased any " +
                        "time with Reset Progress or by uninstalling the app.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPrivacy = false
                    openUrl(context, context.getString(R.string.privacy_policy_url))
                }) { Text("Full policy", color = IceTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacy = false }) { Text("Close", color = FrostWhite) }
            },
        )
    }
}

/** Opens the store page via an external app. The game itself never uses the network. */
private fun openStorePage(context: Context) {
    val pkg = context.packageName
    val opened = tryStart(context, Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))) ||
        tryStart(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
    if (!opened) Toast.makeText(context, "No app available to open the store", Toast.LENGTH_SHORT).show()
}

private fun openUrl(context: Context, url: String) {
    if (!tryStart(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))) {
        Toast.makeText(context, "No browser available", Toast.LENGTH_SHORT).show()
    }
}

private fun tryStart(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    false
} catch (e: SecurityException) {
    false
}
