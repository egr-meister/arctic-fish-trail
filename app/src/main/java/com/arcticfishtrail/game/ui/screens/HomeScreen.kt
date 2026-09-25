package com.arcticfishtrail.game.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arcticfishtrail.game.ui.Assets
import com.arcticfishtrail.game.ui.components.GameBackground
import com.arcticfishtrail.game.ui.components.MenuPlateButton
import com.arcticfishtrail.game.ui.components.rememberClickWithSound
import com.arcticfishtrail.game.ui.theme.AuroraGreen
import com.arcticfishtrail.game.ui.theme.FrostWhite
import com.arcticfishtrail.game.ui.theme.SunGold

/**
 * Home — "Aurora Trail" composition (not a mascot/title/stats/button-stack dashboard):
 *  • the logo hangs tilted on the top-left while the red salmon leaps on the top-right,
 *  • a dotted fishing trail winds down across the ice,
 *  • three round ice "trail stops" (Rules, Results, Settings) sit ON the trail, each holding
 *    a fishing item from the art set,
 *  • Aya the angler stands at the side of the trail; the blue fish swims at the trail's end,
 *    where the ornate START plate waits.
 */
@Composable
fun HomeScreen(
    onStart: () -> Unit,
    onResults: () -> Unit,
    onRules: () -> Unit,
    onSettings: () -> Unit,
) {
    val bob = rememberInfiniteTransition(label = "bob")
    val bobOffset by bob.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "bobOffset",
    )
    val tilt by bob.animateFloat(
        initialValue = -8f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "tilt",
    )

    GameBackground(dim = 0.15f) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header: tilted logo + leaping salmon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Image(
                    painter = painterResource(Assets.logo),
                    contentDescription = "Arctic Fish Trail",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .weight(0.62f)
                        .aspectRatio(Assets.LOGO_ASPECT)
                        .rotate(-3f),
                )
                Box(Modifier.weight(0.38f).padding(top = 24.dp), contentAlignment = Alignment.TopEnd) {
                    Image(
                        painter = painterResource(Assets.mascotB),
                        contentDescription = "A red salmon leaping out of the water",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(Assets.MASCOT_B_ASPECT)
                            .offset(y = bobOffset.dp)
                            .rotate(tilt),
                    )
                }
            }

            // The trail: stops on the left/centre, Aya standing on the right
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            ) {
                val w = maxWidth
                val h = maxHeight
                val stops = listOf(0.2f to 0.12f, 0.46f to 0.46f, 0.2f to 0.8f)
                TrailPath(stops = listOf(0.62f to -0.05f) + stops + (0.5f to 1.08f))
                TrailStop(Assets.ITEM_COMPASS, "Rules", onRules, stops[0], w, h)
                TrailStop(Assets.ITEM_TACKLE_BOX, "Results", onResults, stops[1], w, h)
                TrailStop(Assets.ITEM_LANTERN, "Settings", onSettings, stops[2], w, h)
                Image(
                    painter = painterResource(Assets.mascotA),
                    contentDescription = "Aya the angler, ready for the trail",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp)
                        .height(h * 0.98f)
                        .aspectRatio(Assets.MASCOT_A_ASPECT),
                )
            }

            Spacer(Modifier.height(8.dp))
            MenuPlateButton(text = "START", onClick = onStart, widthFraction = 0.72f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(Assets.mascotC),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(64.dp)
                        .aspectRatio(Assets.MASCOT_C_ASPECT)
                        .offset(y = (-bobOffset).dp),
                )
                Text(
                    text = "Follow the trail,\ncatch every fish!",
                    style = MaterialTheme.typography.titleMedium,
                    color = FrostWhite,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TrailPath(stops: List<Pair<Float, Float>>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val pts = stops.map { (fx, fy) -> Offset(size.width * fx, size.height * fy) }
        val path = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val a = pts[i - 1]
                val b = pts[i]
                val midY = (a.y + b.y) / 2f
                cubicTo(a.x, midY, b.x, midY, b.x, b.y)
            }
        }
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(
                width = 6.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 16.dp.toPx())),
            ),
        )
        drawPath(
            path = path,
            color = AuroraGreen.copy(alpha = 0.25f),
            style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun TrailStop(
    itemIndex: Int,
    label: String,
    onClick: () -> Unit,
    centre: Pair<Float, Float>,
    boxWidth: Dp,
    boxHeight: Dp,
) {
    val bubble = 78.dp
    val click = rememberClickWithSound(onClick)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(x = boxWidth * centre.first - 55.dp, y = boxHeight * centre.second - bubble / 2)
            .width(110.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = click)
            .semantics(mergeDescendants = true) {},
    ) {
        Box(Modifier.size(bubble), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(Assets.plateRound),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            Image(
                painter = painterResource(Assets.item(itemIndex)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(0.52f),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = SunGold,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}
