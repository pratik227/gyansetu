package com.gyansetu.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gyansetu.data.ContentState
import com.gyansetu.data.QuizQuestion
import com.gyansetu.ui.theme.GyanColors
import com.gyansetu.viewmodel.AppViewModel
import kotlinx.coroutines.delay

/**
 * GK quiz: 5 questions generated on-device by Gemma 4 (with a hand-curated
 * fallback pool when the model is unavailable). Adaptive selection sorts by
 * the kid's per-topic mastery so weak areas come up more often.
 */
@Composable
fun QuizScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onComplete: (stars: Int, total: Int) -> Unit,
    onAnswered: (topic: String, correct: Boolean) -> Unit = { _, _ -> },
    masteryProvider: () -> Map<String, Int> = { emptyMap() },
) {
    // Trigger eager generation if nothing's in flight yet — no-op when the
    // background pre-warm already produced a Ready batch.
    LaunchedEffect(Unit) { viewModel.ensureQuizContent() }

    val state by viewModel.quizContent.collectAsState()

    // Snapshot the question list once at first non-loading state so an
    // in-flight regeneration doesn't reshuffle the screen mid-game.
    var questions by remember { mutableStateOf<List<QuizQuestion>?>(null) }
    LaunchedEffect(state) {
        if (questions == null) {
            val pool = when (val s = state) {
                is ContentState.Ready -> s.data
                is ContentState.Failed -> s.fallback
                ContentState.Loading -> null
            }
            if (pool != null) {
                val mastery = masteryProvider()
                questions = pool.shuffled()
                    .sortedBy { mastery[it.topic] ?: 0 }   // weakest topics first
                    .take(5)
                    .shuffled()                             // randomise presentation order
            }
        }
    }

    val qs = questions
    if (qs == null) {
        QuizLoading(onBack = onBack)
        return
    }

    QuizUI(
        questions = qs,
        showFallbackBanner = state is ContentState.Failed,
        onBack = onBack,
        onComplete = { stars, total ->
            // Kick off the next batch so the kid sees fresh content next play.
            viewModel.regenerateQuizContent()
            onComplete(stars, total)
        },
        onAnswered = onAnswered,
    )
}

@Composable
private fun QuizLoading(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(GyanColors.PurpleSoft, GyanColors.Bg)))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp)
                    .background(Color.White, CircleShape)
                    .border(2.5.dp, GyanColors.Ink, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GyanColors.Ink) }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🦉", fontSize = 80.sp)
                Spacer(Modifier.height(12.dp))
                Text("નવા પ્રશ્નો બનાવી રહ્યા છીએ…", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                Text("Crafting fresh questions for you ✨", fontSize = 13.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("(this takes ~20 seconds — first time only)", fontSize = 11.sp, color = GyanColors.InkSoft)
            }
        }
    }
}

@Composable
private fun QuizUI(
    questions: List<QuizQuestion>,
    showFallbackBanner: Boolean,
    onBack: () -> Unit,
    onComplete: (stars: Int, total: Int) -> Unit,
    onAnswered: (topic: String, correct: Boolean) -> Unit,
) {
    var qIdx by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf(false) }
    var stars by remember { mutableStateOf(0) }

    val q = questions[qIdx]
    val total = questions.size

    LaunchedEffect(feedback) {
        if (!feedback) return@LaunchedEffect
        delay(1800)
        if (qIdx + 1 < total) {
            qIdx += 1
            selected = null
            feedback = false
        } else {
            onComplete(stars, total)
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(GyanColors.PurpleSoft, GyanColors.Bg))
            )
            .padding(16.dp),
    ) {
        // Top bar with progress + stars
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp)
                    .background(Color.White, CircleShape)
                    .border(2.5.dp, GyanColors.Ink, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GyanColors.Ink) }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "પ્રશ્ન · Question ${qIdx + 1}/$total",
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.InkSoft,
                )
                Spacer(Modifier.size(4.dp))
                Box(
                    Modifier.fillMaxWidth().height(10.dp)
                        .background(Color.White, RoundedCornerShape(5.dp))
                        .border(2.dp, GyanColors.Ink, RoundedCornerShape(5.dp))
                ) {
                    val pct = ((qIdx + (if (feedback) 1 else 0)).toFloat() / total).coerceIn(0f, 1f)
                    Box(
                        Modifier.fillMaxWidth(pct).height(10.dp).background(
                            Brush.horizontalGradient(listOf(GyanColors.Purple, GyanColors.Saffron)),
                            RoundedCornerShape(5.dp),
                        )
                    )
                }
            }
            Spacer(Modifier.size(10.dp))
            Row(
                modifier = Modifier
                    .background(GyanColors.YellowSoft, RoundedCornerShape(999.dp))
                    .border(2.dp, GyanColors.Yellow, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⭐", fontSize = 16.sp)
                Spacer(Modifier.size(4.dp))
                Text("$stars", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
            }
        }

        if (showFallbackBanner) {
            Spacer(Modifier.size(8.dp))
            Text(
                "📖 Classic questions (model busy)",
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.InkSoft,
            )
        }

        Spacer(Modifier.size(14.dp))

        // Question card
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(3.dp, GyanColors.Ink, RoundedCornerShape(24.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(80.dp)
                    .background(GyanColors.PurpleSoft, RoundedCornerShape(20.dp))
                    .border(2.5.dp, GyanColors.Purple, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(q.emoji, fontSize = 48.sp) }
            Spacer(Modifier.size(10.dp))
            Text(q.gu, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
            Text(q.en, fontSize = 13.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.size(14.dp))

        val palette = listOf(
            Color(0xFFFFB85C),
            Color(0xFF6BD662),
            Color(0xFF4FC3F7),
            Color(0xFFFF6F91),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            listOf(0..1, 2..3).forEach { rowRange ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (i in rowRange) {
                        val opt = q.options[i]
                        val bgBase = palette[i]
                        val isSelected = selected == i
                        val showCorrect = feedback && opt.correct
                        val showWrong = feedback && isSelected && !opt.correct
                        val bg = when {
                            showCorrect -> GyanColors.Green
                            showWrong -> Color(0xFF999999)
                            feedback -> Color(0xFFE8E0D4)
                            else -> bgBase
                        }
                        val textColor = if (feedback && !opt.correct && !isSelected)
                            GyanColors.InkSoft else Color.White
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(124.dp)
                                .background(bg, RoundedCornerShape(20.dp))
                                .border(3.dp, GyanColors.Ink, RoundedCornerShape(20.dp))
                                .clickable(enabled = selected == null) {
                                    if (selected != null) return@clickable
                                    selected = i
                                    feedback = true
                                    if (opt.correct) stars += 1
                                    onAnswered(q.topic, opt.correct)
                                }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(opt.icon, fontSize = 28.sp)
                            Spacer(Modifier.size(4.dp))
                            Text(opt.gu, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                            Text(opt.en, fontSize = 11.sp, color = textColor.copy(alpha = 0.95f), fontWeight = FontWeight.SemiBold)
                            if (showCorrect) Text("✓", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            if (showWrong) Text("✗", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // Feedback bubble
        if (feedback) {
            Spacer(Modifier.size(12.dp))
            val correct = q.options[selected!!].correct
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        if (correct) GyanColors.GreenSoft else GyanColors.PinkSoft,
                        RoundedCornerShape(16.dp),
                    )
                    .border(
                        2.5.dp,
                        if (correct) GyanColors.Green else GyanColors.Pink,
                        RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (correct) "🎉" else "💪", fontSize = 26.sp)
                Spacer(Modifier.size(8.dp))
                Column {
                    Text(
                        if (correct) "વાહ! શાબાશ!" else "કોઈ વાંધો નહીં!",
                        fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink,
                    )
                    Text(
                        if (correct) "Wahoo! Well done!" else "No problem, keep trying!",
                        fontSize = 12.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
