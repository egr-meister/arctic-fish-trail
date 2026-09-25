package com.arcticfishtrail.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcticfishtrail.game.domain.GameLogic
import com.arcticfishtrail.game.ui.Assets
import com.arcticfishtrail.game.ui.LocalAppContainer
import com.arcticfishtrail.game.ui.components.BackIconButton
import com.arcticfishtrail.game.ui.components.FallbackScreen
import com.arcticfishtrail.game.ui.components.GameBackground
import com.arcticfishtrail.game.ui.components.GameTopBar
import com.arcticfishtrail.game.ui.components.IconPlateButton
import com.arcticfishtrail.game.ui.components.InfoChip
import com.arcticfishtrail.game.ui.components.LoadingHint
import com.arcticfishtrail.game.ui.components.ModalPanel
import com.arcticfishtrail.game.ui.components.PanelActions
import com.arcticfishtrail.game.ui.theme.AuroraGreen
import com.arcticfishtrail.game.ui.theme.FrostWhite
import com.arcticfishtrail.game.ui.theme.GlacierBlue
import com.arcticfishtrail.game.ui.theme.IceTeal
import com.arcticfishtrail.game.ui.theme.PolarNight
import com.arcticfishtrail.game.ui.theme.SalmonCoral
import com.arcticfishtrail.game.ui.theme.SunGold
import com.arcticfishtrail.game.viewmodel.CardUi
import com.arcticfishtrail.game.viewmodel.PairsFinished
import com.arcticfishtrail.game.viewmodel.PairsStatus
import com.arcticfishtrail.game.viewmodel.PairsViewModel
import kotlin.math.ceil
import kotlin.math.min

/** Card proportions (width / height), like a playing card. */
private const val CARD_ASPECT = 0.78f

@Composable
fun PairsScreen(
    level: Int,
    onFinished: (PairsFinished) -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit,
) {
    if (GameLogic.levelConfig(level) == null) {
        FallbackScreen(message = "This level does not exist.", onBack = onBack)
        return
    }
    val container = LocalAppContainer.current
    val vm: PairsViewModel = viewModel(factory = PairsViewModel.factory(level, container.repository, container.soundManager))
    val state by vm.uiState.collectAsStateWithLifecycle()

    when (state.status) {
        PairsStatus.LOADING -> {
            GameBackground { LoadingHint() }
            return
        }
        PairsStatus.INVALID -> {
            FallbackScreen(message = "This level does not exist.", onBack = onBack)
            return
        }
        PairsStatus.LOCKED -> {
            FallbackScreen(
                title = "Level locked",
                message = "Clear level ${level - 1} first to unlock level $level.",
                onBack = onBack,
            )
            return
        }
        PairsStatus.PLAYING -> Unit
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.pause() }
    BackHandler { if (state.isPaused) vm.resume() else vm.pause() }

    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(state.finished, resumed) {
        val finished = state.finished
        if (finished != null && resumed) onFinished(finished)
    }

    GameBackground(dim = 0.35f) {
        Column(Modifier.fillMaxSize()) {
            GameTopBar(
                title = "Level ${state.level}",
                leading = { BackIconButton(onBack) },
                actions = { IconPlateButton(Assets.iconPause, "Pause", onClick = vm::pause) },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                InfoChip(
                    text = (if (state.isTimeLow) "⏱ " else "") + GameLogic.formatTime(state.remainingSeconds),
                    color = if (state.isTimeLow) SalmonCoral else IceTeal,
                    modifier = Modifier.semantics {
                        contentDescription = "Time left ${state.remainingSeconds} seconds"
                    },
                )
                InfoChip(
                    text = "Pairs ${state.matchedPairs}/${state.totalPairs}",
                    color = SunGold,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            CardGrid(
                cards = state.cards,
                columns = state.columns,
                onTap = vm::onCardTap,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }

        if (state.isPaused) {
            ModalPanel(title = "Paused") {
                Text(
                    "Pairs ${state.matchedPairs}/${state.totalPairs} · ${GameLogic.formatTime(state.remainingSeconds)} left",
                    color = FrostWhite,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PanelActions(onContinue = vm::resume, onRestart = vm::restart, onMenu = onMenu)
            }
        }
    }
}

/**
 * Grid that always fits the available space WITHOUT scrolling: the card size is the largest
 * that fits both the width (columns) and the height (rows) at [CARD_ASPECT].
 */
@Composable
private fun CardGrid(
    cards: List<CardUi>,
    columns: Int,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) return
    val cols = columns.coerceAtLeast(1)
    val rows = ceil(cards.size / cols.toFloat()).toInt().coerceAtLeast(1)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val gap = if (rows >= 5) 8.dp else 12.dp
        val byWidth = (maxWidth - gap * (cols - 1)) / cols
        val byHeight = ((maxHeight - gap * (rows - 1)) / rows) * CARD_ASPECT
        val cardWidth = min(byWidth.value, byHeight.value).coerceAtLeast(24f).dp
        val cardHeight = cardWidth / CARD_ASPECT
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            cards.chunked(cols).forEach { rowCards ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    rowCards.forEach { card ->
                        MatchCard(
                            card = card,
                            onTap = { onTap(card.id) },
                            modifier = Modifier.width(cardWidth).height(cardHeight),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchCard(card: CardUi, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFaceUp) 180f else 0f,
        animationSpec = tween(320),
        label = "flip",
    )
    val density = LocalDensity.current.density
    val showFront = rotation > 90f
    val shape = RoundedCornerShape(14.dp)
    val borderColor = when {
        card.isMatched -> AuroraGreen
        showFront -> IceTeal
        else -> SunGold
    }
    val description = when {
        card.isMatched -> "${Assets.itemName(card.itemIndex)}, matched"
        card.isFaceUp -> Assets.itemName(card.itemIndex)
        else -> "Hidden card"
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clip(shape)
            .background(
                if (showFront) {
                    Brush.verticalGradient(listOf(Color(0xFFF4FBFF), Color(0xFFBFE3FA)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF1F5BD6), GlacierBlue, PolarNight))
                },
            )
            .border(if (card.isMatched) 3.dp else 2.dp, borderColor, shape)
            .clickable(enabled = !card.isFaceUp, role = Role.Button, onClick = onTap)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (showFront) {
            // Counter-rotate so the face is not mirrored.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Assets.item(card.itemIndex)),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(0.78f),
                )
                if (card.isMatched) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF17804F)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✓", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.clearAndSetSemantics {})
                    }
                }
            }
        } else {
            Image(
                painter = painterResource(Assets.logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .aspectRatio(Assets.LOGO_ASPECT),
            )
        }
    }
}

@Composable
fun PairsResultScreen(
    level: Int,
    matched: Int,
    total: Int,
    won: Boolean,
    onNextLevel: (Int) -> Unit,
    onLevels: () -> Unit,
    onRestart: (Int) -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit,
) {
    val config = GameLogic.levelConfig(level)
    if (config == null || total != config.pairs || matched !in 0..total) {
        FallbackScreen(message = "This result is no longer available.", onBack = onBack)
        return
    }
    val next = GameLogic.nextLevel(level)
    GameBackground(dim = 0.4f) {
        ModalPanel(
            title = if (won) "Well Done" else "Out of Time",
            borderColor = if (won) SunGold else SalmonCoral,
        ) {
            Image(
                painter = painterResource(if (won) Assets.mascotA else Assets.mascotC),
                contentDescription = if (won) "Aya the angler celebrating" else "The blue arctic fish swimming away",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(if (won) 150.dp else 100.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text("Level $level", color = FrostWhite, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Pairs: $matched/$total",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    won && next != null -> "Level $next is now unlocked!"
                    won -> "You cleared the whole trail!"
                    else -> "Clear every pair before the timer ends."
                },
                color = if (won) AuroraGreen else SunGold,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            PanelActions(
                continueLabel = if (won && next != null) "Next Level" else "Continue",
                onContinue = { if (won && next != null) onNextLevel(next) else onLevels() },
                onRestart = { onRestart(level) },
                onMenu = onMenu,
            )
        }
    }
}
