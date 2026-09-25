package com.arcticfishtrail.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
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
import com.arcticfishtrail.game.domain.QuizContent
import com.arcticfishtrail.game.ui.Assets
import com.arcticfishtrail.game.ui.LocalAppContainer
import com.arcticfishtrail.game.ui.components.BackIconButton
import com.arcticfishtrail.game.ui.components.FallbackScreen
import com.arcticfishtrail.game.ui.components.GameBackground
import com.arcticfishtrail.game.ui.components.GameTopBar
import com.arcticfishtrail.game.ui.components.GlassPanel
import com.arcticfishtrail.game.ui.components.IconPlateButton
import com.arcticfishtrail.game.ui.components.InfoChip
import com.arcticfishtrail.game.ui.components.ModalPanel
import com.arcticfishtrail.game.ui.components.PanelActions
import com.arcticfishtrail.game.ui.components.PlateButton
import com.arcticfishtrail.game.ui.components.TrailProgressBar
import com.arcticfishtrail.game.ui.theme.AuroraGreen
import com.arcticfishtrail.game.ui.theme.CorrectFill
import com.arcticfishtrail.game.ui.theme.FrostWhite
import com.arcticfishtrail.game.ui.theme.SunGold
import com.arcticfishtrail.game.ui.theme.WrongFill
import com.arcticfishtrail.game.viewmodel.QuizFinished
import com.arcticfishtrail.game.viewmodel.QuizViewModel
import kotlin.math.min

@Composable
fun QuizScreen(
    categoryId: String,
    onFinished: (QuizFinished) -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit,
) {
    if (QuizContent.category(categoryId) == null) {
        FallbackScreen(message = "This quiz category could not be found.", onBack = onBack)
        return
    }
    val container = LocalAppContainer.current
    val vm: QuizViewModel = viewModel(factory = QuizViewModel.factory(categoryId, container.repository, container.soundManager))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val question = state.question

    if (!state.isValid || question == null) {
        FallbackScreen(message = "This quiz could not be loaded.", onBack = onBack)
        return
    }

    // Auto-pause when the app goes to the background.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.pause() }
    BackHandler { if (state.isPaused) vm.resume() else vm.pause() }

    // Navigate once the result is persisted — and only while this screen is RESUMED.
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(state.finished, resumed) {
        val finished = state.finished
        if (finished != null && resumed) onFinished(finished)
    }

    GameBackground(dim = 0.45f) {
        Column(Modifier.fillMaxSize()) {
            GameTopBar(
                title = "${state.categoryTitle} Quiz",
                leading = { BackIconButton(onBack) },
                actions = { IconPlateButton(Assets.iconPause, "Pause", onClick = vm::pause) },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrailProgressBar(
                    progress = state.progress,
                    label = "Question ${state.questionIndex + 1} of ${state.total}",
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                InfoChip("${(state.questionIndex + 1).coerceAtMost(state.total)}/${state.total}", color = SunGold)
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val viewport = maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // At least one screen tall so the mascot strip can take the leftover space;
                    // with large fonts the content simply grows and scrolls.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = viewport)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GlassPanel(modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)) {
                            Text(
                                text = question.text,
                                style = MaterialTheme.typography.headlineSmall,
                                color = FrostWhite,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        question.options.forEachIndexed { index, option ->
                            val selected = state.selectedIndex
                            val isCorrect = index == question.correctIndex
                            val fill: Color? = when {
                                selected == null -> null
                                isCorrect -> CorrectFill
                                index == selected -> WrongFill
                                else -> null
                            }
                            val marker = when {
                                selected == null -> ""
                                isCorrect -> "✓ "
                                index == selected -> "✗ "
                                else -> ""
                            }
                            PlateButton(
                                text = marker + option,
                                onClick = { vm.answer(index) },
                                stateFill = fill,
                                enabled = selected == null || fill != null,
                                playClick = false, // correct/wrong SFX replaces the click
                                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                            )
                        }
                        val selected = state.selectedIndex
                        Text(
                            text = when {
                                selected == null -> " "
                                selected == question.correctIndex -> "Correct!"
                                else -> "Not quite — the right answer is marked ✓"
                            },
                            color = if (selected == question.correctIndex) AuroraGreen else SunGold,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        MascotStrip(
                            cheering = selected != null && selected == question.correctIndex,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (state.isPaused) {
            ModalPanel(title = "Paused") {
                Text("Score so far: ${state.score}", color = FrostWhite, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                PanelActions(onContinue = vm::resume, onRestart = vm::restart, onMenu = onMenu)
            }
        }
    }
}

@Composable
fun QuizResultScreen(
    categoryId: String,
    score: Int,
    total: Int,
    onContinue: () -> Unit,
    onRestart: (String) -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit,
) {
    val category = QuizContent.category(categoryId)
    if (category == null || !GameLogic.isValidQuizResult(score, total)) {
        FallbackScreen(message = "This result is no longer available.", onBack = onBack)
        return
    }
    val perfect = score == total
    val headline = when {
        perfect -> "Perfect catch!"
        score * 2 >= total -> "Nice fishing!"
        else -> "Keep exploring!"
    }
    GameBackground(dim = 0.4f) {
        ModalPanel(title = headline, borderColor = if (perfect) SunGold else AuroraGreen) {
            Row(verticalAlignment = Alignment.Bottom) {
                Image(
                    painter = painterResource(Assets.mascotA),
                    contentDescription = "Aya the angler",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(140.dp).aspectRatio(Assets.MASCOT_A_ASPECT),
                )
                Image(
                    painter = painterResource(if (perfect) Assets.mascotB else Assets.mascotC),
                    contentDescription = if (perfect) "A big red salmon catch" else "A blue arctic fish",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(64.dp).aspectRatio(if (perfect) Assets.MASCOT_B_ASPECT else Assets.MASCOT_C_ASPECT),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("${category.title} Quiz", color = FrostWhite, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Score: $score/$total",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth()) {
                PanelActions(
                    onContinue = onContinue,
                    onRestart = { onRestart(category.id) },
                    onMenu = onMenu,
                )
            }
        }
    }
}

/**
 * Fills the free space under the answers with the cast: Aya the angler plus the two fish.
 * Hidden when there is not enough room (small screens / large fonts).
 */
@Composable
private fun MascotStrip(cheering: Boolean, modifier: Modifier = Modifier) {
    val bob = rememberInfiniteTransition(label = "fish")
    val dy by bob.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "dy",
    )
    BoxWithConstraints(modifier, contentAlignment = Alignment.BottomCenter) {
        val h = maxHeight
        if (h < 72.dp) return@BoxWithConstraints
        val heroHeight = min(h.value, 260f).dp
        Row(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Image(
                painter = painterResource(Assets.mascotA),
                contentDescription = "Aya the angler",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(heroHeight).aspectRatio(Assets.MASCOT_A_ASPECT),
            )
            Column(horizontalAlignment = Alignment.End) {
                Image(
                    painter = painterResource(Assets.mascotB),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(heroHeight * 0.38f)
                        .aspectRatio(Assets.MASCOT_B_ASPECT)
                        .offset(y = (if (cheering) dy * 2 else dy).dp),
                )
                Image(
                    painter = painterResource(Assets.mascotC),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(heroHeight * 0.32f)
                        .aspectRatio(Assets.MASCOT_C_ASPECT)
                        .offset(y = (-dy).dp),
                )
            }
        }
    }
}
