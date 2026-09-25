package com.arcticfishtrail.game.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcticfishtrail.game.domain.GameLogic
import com.arcticfishtrail.game.ui.Assets
import com.arcticfishtrail.game.ui.LocalAppContainer
import com.arcticfishtrail.game.ui.components.BackIconButton
import com.arcticfishtrail.game.ui.components.GameBackground
import com.arcticfishtrail.game.ui.components.GameTopBar
import com.arcticfishtrail.game.ui.components.GlassPanel
import com.arcticfishtrail.game.ui.components.InfoChip
import com.arcticfishtrail.game.ui.components.LoadingHint
import com.arcticfishtrail.game.ui.components.MenuPlateButton
import com.arcticfishtrail.game.ui.components.PlateButton
import com.arcticfishtrail.game.ui.components.rememberClickWithSound
import com.arcticfishtrail.game.ui.theme.AuroraGreen
import com.arcticfishtrail.game.ui.theme.FrostWhite
import com.arcticfishtrail.game.ui.theme.SlateIce
import com.arcticfishtrail.game.ui.theme.SunGold
import com.arcticfishtrail.game.viewmodel.LevelUi
import com.arcticfishtrail.game.viewmodel.ProgressViewModel

@Composable
private fun progressViewModel(): ProgressViewModel {
    val container = LocalAppContainer.current
    return viewModel(factory = ProgressViewModel.factory(container.repository))
}

/* ------------------------------------------------------------------ Menu (Base Camp) */

@Composable
fun MenuScreen(
    onQuiz: () -> Unit,
    onPairs: () -> Unit,
    onResults: () -> Unit,
    onSettings: () -> Unit,
    onRules: () -> Unit,
    onBack: () -> Unit,
) {
    GameBackground {
        Column(Modifier.fillMaxSize()) {
            GameTopBar(title = "Base Camp", leading = { BackIconButton(onBack) })
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                    Image(
                        painter = painterResource(Assets.mascotC),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(56.dp).aspectRatio(Assets.MASCOT_C_ASPECT),
                    )
                    Image(
                        painter = painterResource(Assets.mascotA),
                        contentDescription = "Aya the angler",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(170.dp).aspectRatio(Assets.MASCOT_A_ASPECT),
                    )
                    Image(
                        painter = painterResource(Assets.mascotB),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(62.dp).aspectRatio(Assets.MASCOT_B_ASPECT),
                    )
                }
                MenuPlateButton("Quiz", onQuiz, widthFraction = 0.78f)
                MenuPlateButton("Matching Pairs", onPairs, widthFraction = 0.78f)
                val plateMod = Modifier.fillMaxWidth(0.78f).widthIn(max = 420.dp)
                PlateButton("Results", onResults, plateMod)
                PlateButton("Game Rules", onRules, plateMod)
                PlateButton("Settings", onSettings, plateMod)
            }
        }
    }
}

/* ------------------------------------------------------------------ Quiz categories */

@Composable
fun QuizCategoriesScreen(onCategory: (String) -> Unit, onBack: () -> Unit) {
    val vm = progressViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    GameBackground {
        Column(Modifier.fillMaxSize()) {
            GameTopBar(title = "Choose Difficulty", leading = { BackIconButton(onBack) })
            if (!state.loaded) {
                LoadingHint()
                return@Column
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                state.categories.forEach { cat ->
                    GlassPanel(modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                        Image(
                            painter = painterResource(Assets.item(cat.iconItem)),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(64.dp),
                        )
                        Text(cat.title, style = MaterialTheme.typography.headlineSmall, color = SunGold, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Text(cat.subtitle, style = MaterialTheme.typography.bodyMedium, color = FrostWhite, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        InfoChip(
                            text = "10 questions · " + (cat.best?.let { "Best: $it/${cat.total}" } ?: "Not played yet"),
                            color = if (cat.best == cat.total) AuroraGreen else SunGold,
                        )
                        Spacer(Modifier.height(12.dp))
                        PlateButton("Play", { onCategory(cat.id) }, Modifier.fillMaxWidth(0.8f))
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ Levels */

@Composable
fun LevelsScreen(onLevel: (Int) -> Unit, onRules: () -> Unit, onBack: () -> Unit) {
    val vm = progressViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    GameBackground {
        Column(Modifier.fillMaxSize()) {
            GameTopBar(
                title = "Matching Pairs",
                leading = { BackIconButton(onBack) },
                trailing = if (state.loaded) "${state.completedLevels}/${state.totalLevels}" else null,
                trailingColor = SunGold,
            )
            if (!state.loaded) {
                LoadingHint()
                return@Column
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    listOf(Assets.ITEM_NET, Assets.ITEM_BOBBER, Assets.ITEM_LANTERN, Assets.ITEM_TENT).forEach { idx ->
                        Image(
                            painter = painterResource(Assets.item(idx)),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                Text(
                    "Clear a level to unlock the next one.",
                    color = FrostWhite,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                state.levels.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { level ->
                            LevelBubble(level = level, onClick = { onLevel(level.level) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                PlateButton("Game Rules", onRules, Modifier.fillMaxWidth(0.78f).widthIn(max = 420.dp))
            }
        }
    }
}

@Composable
private fun LevelBubble(level: LevelUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val click = rememberClickWithSound(onClick)
    val status = when {
        level.completed -> "cleared"
        level.unlocked -> "unlocked"
        else -> "locked"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(4.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = level.unlocked, role = Role.Button, onClick = click)
            .semantics(mergeDescendants = true) {
                contentDescription = "Level ${level.level}, $status, best ${level.best} of ${level.pairs} pairs"
            }
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 110.dp)
                .aspectRatio(1f)
                .alpha(if (level.unlocked) 1f else 0.45f),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Assets.plateRound),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
            Text(
                text = level.level.toString(),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        Spacer(Modifier.height(4.dp))
        // Text state label → colour is never the only signal.
        Text(
            text = when {
                level.completed -> "★ Cleared"
                level.unlocked -> "${level.pairs} pairs"
                else -> "Locked"
            },
            color = when {
                level.completed -> SunGold
                level.unlocked -> FrostWhite
                else -> SlateIce
            },
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/* ------------------------------------------------------------------ Results summary */

@Composable
fun ResultsSummaryScreen(onBack: () -> Unit) {
    val vm = progressViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    GameBackground {
        Column(Modifier.fillMaxSize()) {
            GameTopBar(title = "Trail Log", leading = { BackIconButton(onBack) })
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(
                    painter = painterResource(Assets.mascotA),
                    contentDescription = "Aya the angler",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(150.dp).aspectRatio(Assets.MASCOT_A_ASPECT),
                )
                GlassPanel(modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                    Image(
                        painter = painterResource(Assets.item(Assets.ITEM_COMPASS)),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                    )
                    Text("Quiz — best scores", style = MaterialTheme.typography.titleLarge, color = SunGold)
                    Spacer(Modifier.height(10.dp))
                    state.categories.forEach { cat ->
                        SummaryRow("${cat.title} Quiz", cat.best?.let { "$it/${cat.total}" } ?: "—")
                    }
                }
                GlassPanel(modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                    Image(
                        painter = painterResource(Assets.item(Assets.ITEM_TACKLE_BOX)),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                    )
                    Text("Matching Pairs", style = MaterialTheme.typography.titleLarge, color = SunGold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Levels cleared: ${state.completedLevels} / ${state.totalLevels}",
                        style = MaterialTheme.typography.titleMedium,
                        color = AuroraGreen,
                    )
                    Spacer(Modifier.height(10.dp))
                    state.levels.forEach { lvl ->
                        SummaryRow(
                            label = "Level ${lvl.level}" + if (lvl.completed) "  ★" else "",
                            value = if (lvl.unlocked) "${lvl.best}/${lvl.pairs}" else "Locked",
                        )
                    }
                }
                Text(
                    "Total levels in the trail: ${GameLogic.LEVEL_COUNT}",
                    color = SlateIce,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = FrostWhite, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
    }
}

/* ------------------------------------------------------------------ Game rules */

@Composable
fun GameRulesScreen(onBack: () -> Unit) {
    GameBackground {
        Column(Modifier.fillMaxSize()) {
            GameTopBar(title = "Game Rules", leading = { BackIconButton(onBack) })
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GlassPanel(modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)) {
                    Image(
                        painter = painterResource(Assets.logo),
                        contentDescription = "Arctic Fish Trail",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(120.dp).aspectRatio(Assets.LOGO_ASPECT),
                    )
                    Spacer(Modifier.height(12.dp))
                    RulesSection(
                        "Quiz",
                        "Pick a difficulty — Easy, Medium or Hard — and answer 10 fishing questions, one at a time. " +
                            "Tap an answer: the correct option turns green with a ✓, a wrong pick turns red with a ✗. " +
                            "The next question appears automatically. Each question has 4 options and exactly one is right. Your best score per difficulty is saved.",
                    )
                    RulesSection(
                        "Matching Pairs",
                        "Flip two cards at a time to find matching icons. Matches stay open, " +
                            "mismatches flip back. Clear every pair before the timer runs out to win the level " +
                            "and unlock the next one. There are 9 levels — more pairs, more columns and 18 different fishing items as you go.",
                    )
                    RulesSection(
                        "Pause",
                        "Use the pause button or the Back gesture during a game. " +
                            "The timer stops while paused. Continue, Restart or return to the Menu. " +
                            "The back arrow at the top leaves the game.",
                    )
                    RulesSection(
                        "Offline",
                        "Arctic Fish Trail works fully offline. Progress is stored only on this device.",
                    )
                    Spacer(Modifier.height(8.dp))
                    PlateButton("Got it", onBack, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun RulesSection(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = SunGold)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = FrostWhite)
    }
}
