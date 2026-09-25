package com.arcticfishtrail.game.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.arcticfishtrail.game.audio.Sfx
import com.arcticfishtrail.game.ui.Assets
import com.arcticfishtrail.game.ui.LocalSoundManager
import com.arcticfishtrail.game.ui.theme.GlacierBlue
import com.arcticfishtrail.game.ui.theme.IceTeal
import com.arcticfishtrail.game.ui.theme.PolarNight
import com.arcticfishtrail.game.ui.theme.Scrim
import com.arcticfishtrail.game.ui.theme.SlateIce
import com.arcticfishtrail.game.ui.theme.SunGold

/** Returns an onClick that plays the click SFX first. */
@Composable
fun rememberClickWithSound(onClick: () -> Unit): () -> Unit {
    val sound = LocalSoundManager.current
    return remember(onClick, sound) {
        {
            sound?.play(Sfx.CLICK)
            onClick()
        }
    }
}

/**
 * Draws a plate image as a horizontal 3-slice: the image is scaled uniformly to the target
 * height, both rounded ends ([capFraction] of the source width) are kept intact and only the
 * straight middle section stretches. The ice frame therefore never looks squashed, whatever
 * width or height the button gets.
 */
@Composable
fun SlicedPlate(
    @DrawableRes res: Int,
    modifier: Modifier = Modifier,
    capFraction: Float = Assets.BTN_PLATE_CAP_FRACTION,
) {
    val image = ImageBitmap.imageResource(res)
    Canvas(modifier) {
        val iw = image.width
        val ih = image.height
        if (iw < 4 || ih <= 0 || size.width <= 0f || size.height <= 0f) return@Canvas
        val dstH = size.height.roundToInt().coerceAtLeast(1)
        val dstW = size.width.roundToInt().coerceAtLeast(1)
        val capSrc = (iw * capFraction).roundToInt().coerceIn(1, iw / 2 - 1)
        val scale = size.height / ih
        val capDst = (capSrc * scale).roundToInt().coerceAtMost(dstW / 2)
        val midSrc = (iw - 2 * capSrc).coerceAtLeast(1)
        val midDst = (dstW - 2 * capDst).coerceAtLeast(0)
        drawImage(image, IntOffset(0, 0), IntSize(capSrc, ih), IntOffset(0, 0), IntSize(capDst, dstH), filterQuality = FilterQuality.High)
        if (midDst > 0) {
            drawImage(image, IntOffset(capSrc, 0), IntSize(midSrc, ih), IntOffset(capDst, 0), IntSize(midDst, dstH), filterQuality = FilterQuality.High)
        }
        drawImage(image, IntOffset(iw - capSrc, 0), IntSize(capSrc, ih), IntOffset(dstW - capDst, 0), IntSize(capDst, dstH), filterQuality = FilterQuality.High)
    }
}

/**
 * Standard game button: the ice plate ([Assets.btnPlate]) with a WHITE bold label drawn on top.
 * [stateFill] (answer feedback) paints a coloured overlay inside the frame; callers also add a
 * text marker (✓ / ✗) so colour is never the only signal.
 */
@Composable
fun PlateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    stateFill: Color? = null,
    playClick: Boolean = true,
    minHeight: Dp = 60.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val click = if (playClick) rememberClickWithSound(onClick) else onClick
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .scale(if (pressed) 0.97f else 1f)
            .alpha(if (enabled) 1f else 0.55f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = click,
            ),
        contentAlignment = Alignment.Center,
    ) {
        SlicedPlate(Assets.btnPlate, Modifier.matchParentSize())
        if (stateFill != null) {
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .matchParentSize()
                    .padding(horizontal = 9.dp, vertical = 7.dp)
                    .clip(shape)
                    .background(stateFill.copy(alpha = 0.9f))
                    .border(2.dp, Color.White.copy(alpha = 0.9f), shape),
            )
        }
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 14.dp),
        )
    }
}

/**
 * Ornate framed plate ([Assets.btnMenu]) for main-menu CTAs. Width is constrained to ~70% of
 * the available width and height follows the art's aspect ratio, so the frame never distorts.
 */
@Composable
fun MenuPlateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.7f,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val click = rememberClickWithSound(onClick)
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .widthIn(max = 420.dp)
            .aspectRatio(Assets.BTN_MENU_ASPECT)
            .scale(if (pressed) 0.96f else 1f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = click,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Assets.btnMenu),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize(),
        )
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 36.dp),
        )
    }
}

/** Round image button (back / pause). 48dp+ touch target with a content description. */
@Composable
fun IconPlateButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val click = rememberClickWithSound(onClick)
    Box(
        modifier = modifier
            .size(size.coerceAtLeast(48.dp))
            .clip(RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = click)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Full-screen themed background (bg_main) with a readability gradient; content gets safe insets. */
@Composable
fun GameBackground(
    modifier: Modifier = Modifier,
    dim: Float = 0.35f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().background(PolarNight)) {
        Image(
            painter = painterResource(Assets.bgMain),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(PolarNight.copy(alpha = dim), PolarNight.copy(alpha = dim * 0.4f), PolarNight.copy(alpha = dim)),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            content = content,
        )
    }
}

/** Top bar: optional leading icon button, centred title, optional trailing text. */
@Composable
fun GameTopBar(
    title: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: String? = null,
    trailingColor: Color = Color.White,
    actions: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.widthIn(min = 56.dp)) { leading?.invoke() }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = SunGold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        Box(Modifier.widthIn(min = 56.dp), contentAlignment = Alignment.CenterEnd) {
            if (actions != null) {
                actions()
            } else if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.titleMedium,
                    color = trailingColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Standard back button for the top bar. */
@Composable
fun BackIconButton(onBack: () -> Unit) {
    IconPlateButton(icon = Assets.iconBack, contentDescription = "Back", onClick = onBack)
}

/** Themed progress bar with accessibility range info. */
@Composable
fun TrailProgressBar(progress: Float, modifier: Modifier = Modifier, label: String) {
    val p = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(GlacierBlue)
            .border(1.dp, IceTeal.copy(alpha = 0.6f), RoundedCornerShape(7.dp))
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f)
            },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(p)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.horizontalGradient(listOf(IceTeal, SunGold))),
        )
    }
}

/**
 * A panel DRAWN IN COMPOSE (rounded rect + gradient fill + accent border). Grows with content;
 * never a stretched bitmap frame.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = IceTeal,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(GlacierBlue.copy(alpha = 0.96f), PolarNight.copy(alpha = 0.97f))))
            .border(3.dp, borderColor, shape)
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

/**
 * Modal overlay (pause / results / rules): scrim that swallows touches + a centred [GlassPanel].
 * The panel scrolls when content is taller than the screen (large font scales).
 */
@Composable
fun ModalPanel(
    title: String,
    modifier: Modifier = Modifier,
    borderColor: Color = IceTeal,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        GlassPanel(
            borderColor = borderColor,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp)
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = SunGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

/** Vertical stack of Continue / Restart / Menu plates used by pause & results panels. */
@Composable
fun PanelActions(
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    continueLabel: String = "Continue",
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        PlateButton(continueLabel, onContinue, Modifier.fillMaxWidth())
        PlateButton("Restart", onRestart, Modifier.fillMaxWidth())
        PlateButton("Menu", onMenu, Modifier.fillMaxWidth())
    }
}

/** Friendly fallback for invalid/missing navigation args or unavailable data. Never crashes. */
@Composable
fun FallbackScreen(
    message: String,
    onBack: () -> Unit,
    title: String = "Off the trail",
) {
    GameBackground(dim = 0.55f) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            GlassPanel(modifier = Modifier.fillMaxWidth().widthIn(max = 440.dp)) {
                Image(
                    painter = painterResource(Assets.mascotA),
                    contentDescription = "Aya the angler looking around",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(140.dp).aspectRatio(Assets.MASCOT_A_ASPECT),
                )
                Spacer(Modifier.height(8.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, color = SunGold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                PlateButton("Back", onBack, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Centered loading hint used while the first DataStore value arrives. */
@Composable
fun LoadingHint(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Loading…", color = SlateIce, style = MaterialTheme.typography.titleMedium)
    }
}

/** Small labelled value chip ("Best 7/10"). */
@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, color: Color = SunGold) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(PolarNight.copy(alpha = 0.75f))
            .border(1.dp, color, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}
