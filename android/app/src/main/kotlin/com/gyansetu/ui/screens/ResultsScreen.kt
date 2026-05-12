package com.gyansetu.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gyansetu.ai.ContentGenerator
import com.gyansetu.ui.components.ChunkyButton
import com.gyansetu.ui.theme.GyanColors
import com.gyansetu.viewmodel.AppViewModel
import kotlin.random.Random

/** Celebration screen — confetti + stars row + retry/home. The encouragement
 *  message is generated fresh by Gemma each time so a kid retrying the same
 *  game doesn't see the identical "Well done!" twice in a row. Static
 *  band-keyed message is shown instantly while the model catches up. */
@Composable
fun ResultsScreen(
    viewModel: AppViewModel,
    stars: Int,
    total: Int,
    onHome: () -> Unit,
    onRetry: () -> Unit,
) {
    LaunchedEffect(stars, total) { viewModel.generateEncouragement(stars, total) }
    val dynamic by viewModel.encouragement.collectAsState()

    val pct = if (total == 0) 0f else stars.toFloat() / total
    val band = when {
        pct == 1f -> "perfect"
        pct >= 0.5f -> "good"
        else -> "low"
    }
    val fallback = ContentGenerator.staticEncouragements[band]!!
    val gu = dynamic?.gu ?: fallback.gu
    val en = dynamic?.en ?: fallback.en

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(GyanColors.Yellow, GyanColors.Saffron))
        ),
    ) {
        Confetti(modifier = Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🦉", fontSize = 100.sp)
            Spacer(Modifier.size(12.dp))
            Text(gu, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(en, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.size(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(total) { i ->
                    Text(
                        if (i < stars) "⭐" else "☆",
                        fontSize = 48.sp,
                        color = if (i < stars) GyanColors.Yellow else Color.White,
                    )
                }
            }
            Spacer(Modifier.size(24.dp))
            Column(
                Modifier
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(3.dp, GyanColors.Ink, RoundedCornerShape(20.dp))
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("તમે મેળવ્યા · You earned", fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold, color = GyanColors.InkSoft)
                Text(
                    "$stars / $total ⭐",
                    fontSize = 36.sp, fontWeight = FontWeight.ExtraBold,
                    color = GyanColors.SaffronDeep,
                )
            }
            Spacer(Modifier.size(28.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChunkyButton(
                    "↻ ફરી રમો",
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    background = Color.White,
                    contentColor = GyanColors.SaffronDeep,
                )
                ChunkyButton(
                    "🏠 ઘરે",
                    onClick = onHome,
                    modifier = Modifier.weight(1f),
                    background = GyanColors.Green,
                    contentColor = Color.White,
                )
            }
        }
    }
}

/** Cheap confetti — 30 falling rotating squares. */
@Composable
private fun Confetti(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "confetti")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "confetti-t",
    )
    val rng = remember { Random(42) }
    val pieces = remember {
        List(30) {
            ConfettiPiece(
                xPct = rng.nextFloat(),
                phase = rng.nextFloat(),
                color = listOf(
                    Color(0xFFFF9933), Color(0xFF138808), Color(0xFFFFD43B),
                    Color(0xFF9C6ADE), Color(0xFFFF6F91), Color(0xFF4FC3F7),
                )[rng.nextInt(6)],
                rotSpin = rng.nextFloat() * 720f + 180f,
                size = 8f + rng.nextFloat() * 8f,
            )
        }
    }
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        for (p in pieces) {
            val travelT = ((t + p.phase) % 1f)
            val cy = -20f + travelT * (h + 40f)
            val cx = p.xPct * w
            val rotation = p.rotSpin * travelT
            val half = p.size / 2f
            withTransform({ rotate(rotation, pivot = Offset(cx, cy)) }) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(cx - half, cy - half),
                    size = Size(p.size, p.size),
                )
            }
        }
    }
}

private data class ConfettiPiece(
    val xPct: Float, val phase: Float, val color: Color,
    val rotSpin: Float, val size: Float,
)
