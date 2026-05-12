package com.gyansetu.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gyansetu.data.ContentState
import com.gyansetu.data.ROSTER
import com.gyansetu.data.activeStudent
import com.gyansetu.ui.components.ChunkyButton
import com.gyansetu.ui.theme.GyanColors
import com.gyansetu.viewmodel.AppViewModel

private val TOPICS = listOf("animals", "fruits", "classroom", "numbers", "facts", "geography")

/**
 * Teacher Mode — replaces the cosmetic Dashboard with a real classroom tool.
 *
 *   - Switch between students (multi-kid roster)
 *   - See per-topic mastery matrix sourced from the live DataStore
 *   - One-tap progress report export via Android share-sheet (no cloud,
 *     teachers share via Bluetooth / WhatsApp / SMS — whichever they have)
 */
@Composable
fun TeacherModeScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val s by viewModel.settings.collectAsState()
    val ctx = LocalContext.current

    val active = activeStudent(s.activeStudent)

    Column(Modifier.fillMaxSize().background(GyanColors.Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                Modifier.size(44.dp)
                    .background(Color.White, CircleShape)
                    .border(2.5.dp, GyanColors.Ink, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GyanColors.Ink) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("👩‍🏫 Teacher Mode", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                Text("Classroom · ${ROSTER.size} students · all data on-device",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GyanColors.InkSoft)
            }
        }

        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Student switcher
            Text("STUDENTS · વિદ્યાર્થીઓ",
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.InkSoft)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ROSTER.forEach { r ->
                    val isActive = r.id == active.id
                    Column(
                        modifier = Modifier.weight(1f)
                            .background(
                                if (isActive) GyanColors.Saffron else Color.White,
                                RoundedCornerShape(16.dp),
                            )
                            .border(
                                2.5.dp,
                                if (isActive) GyanColors.SaffronDeep else GyanColors.Ink.copy(alpha = 0.2f),
                                RoundedCornerShape(16.dp),
                            )
                            .clickable { viewModel.setActiveStudent(r.id) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            r.gu.first().toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isActive) Color.White else GyanColors.Ink,
                        )
                        Text(
                            r.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isActive) Color.White else GyanColors.Ink,
                        )
                    }
                }
            }

            // Quick stats row for the active student
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("⭐ Stars",   s.stars.toString(),       GyanColors.YellowSoft, GyanColors.Yellow)
                StatChip("🔥 Streak",  s.streak.toString(),       GyanColors.PinkSoft,   GyanColors.Pink)
                StatChip("🚀 Best",    s.bestStreak.toString(),   GyanColors.GreenSoft,  GyanColors.Green)
            }

            // Topic mastery matrix — the heart of Teacher Mode
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(2.5.dp, GyanColors.Ink, RoundedCornerShape(20.dp))
                    .padding(14.dp),
            ) {
                Text("📊 Mastery by topic", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                Text(
                    "Score (0–10) updated live from Quiz answers",
                    fontSize = 11.sp, fontWeight = FontWeight.Medium, color = GyanColors.InkSoft,
                )
                Spacer(Modifier.height(10.dp))
                TOPICS.forEach { t ->
                    val score = s.topicMastery[t] ?: 0
                    MasteryBar(t, score)
                }
            }

            // Recommended actions — Gemma-generated based on the active
            // student + their weakest topic. Refreshes when either changes.
            val weakest = TOPICS.minByOrNull { s.topicMastery[it] ?: 0 }
            val weakestMastery = weakest?.let { s.topicMastery[it] ?: 0 } ?: 0
            LaunchedEffect(active.id, weakest, weakestMastery) {
                if (weakest != null) {
                    viewModel.generateTeacherRecommendation(active.name, weakest, weakestMastery)
                }
            }
            val recState by viewModel.teacherRec.collectAsState()
            if (weakest != null) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(GyanColors.SaffronSoft, RoundedCornerShape(20.dp))
                        .border(2.dp, GyanColors.Saffron, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (recState) {
                                is ContentState.Ready -> "✨ AI Recommendation"
                                else -> "💡 AI Recommendation"
                            },
                            fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.SaffronDeep,
                            modifier = Modifier.weight(1f),
                        )
                        if (recState !is ContentState.Loading) {
                            Box(
                                Modifier
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .border(1.5.dp, GyanColors.Saffron, RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.generateTeacherRecommendation(active.name, weakest, weakestMastery)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text("🔄", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    val recText = when (val r = recState) {
                        is ContentState.Ready -> r.data.text
                        is ContentState.Failed -> r.fallback.text
                        ContentState.Loading -> "Thinking about ${active.name}'s next lesson…"
                    }
                    Text(
                        recText,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GyanColors.Ink,
                    )
                }
            }

            // Export progress report — Bluetooth / WhatsApp / SMS via Android share-sheet
            ChunkyButton(
                "📤 Share progress report",
                onClick = {
                    val report = buildReport(active, s.stars, s.streak, s.bestStreak, s.topicMastery)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "${active.name}'s GyanSetu progress")
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    runCatching {
                        ctx.startActivity(Intent.createChooser(intent, "Share with parent / colleague"))
                    }
                },
                background = GyanColors.Green, contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, bg: Color, border: Color) {
    Column(
        modifier = androidx.compose.ui.Modifier
            .background(bg, RoundedCornerShape(16.dp))
            .border(2.dp, border, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GyanColors.InkSoft)
    }
}

@Composable
private fun MasteryBar(topic: String, score: Int) {
    val pct = (score / 10f).coerceIn(0f, 1f)
    val tier = when {
        score >= 7 -> GyanColors.Green
        score >= 4 -> GyanColors.Yellow
        else       -> GyanColors.Pink
    }
    Column(modifier = androidx.compose.ui.Modifier.padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
            Text(topic.replaceFirstChar { it.uppercase() },
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
            Text("$score / 10",
                fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GyanColors.InkSoft)
        }
        Box(
            androidx.compose.ui.Modifier.fillMaxWidth().height(8.dp)
                .background(GyanColors.Bg, RoundedCornerShape(4.dp))
                .border(1.dp, GyanColors.Ink.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
        ) {
            Box(
                androidx.compose.ui.Modifier.fillMaxWidth(pct).height(8.dp)
                    .background(tier, RoundedCornerShape(4.dp))
            )
        }
    }
}

private fun buildReport(
    student: com.gyansetu.data.Student, stars: Int, streak: Int, best: Int, mastery: Map<String, Int>,
): String = buildString {
    append("📚 GyanSetu Progress Report\n")
    append("─────────────────────────\n")
    append("Student: ${student.name} (${student.gu}) · ${student.grade}\n\n")
    append("Stars earned:   $stars ⭐\n")
    append("Current streak: $streak 🔥\n")
    append("Best streak:    $best 🚀\n\n")
    append("Topic mastery (0–10):\n")
    TOPICS.forEach { t ->
        val score = mastery[t] ?: 0
        val tier = when { score >= 7 -> "✅"; score >= 4 -> "🟡"; else -> "🔴" }
        append("  $tier ${t.padEnd(12)} $score/10\n")
    }
    append("\nRecommended focus: ")
    val weakest = TOPICS.minByOrNull { mastery[it] ?: 0 } ?: "—"
    append(weakest)
    append("\n\nGenerated offline by GyanSetu · ${java.time.LocalDate.now()}\n")
    append("Powered by Gemma 4 + Google AI Edge LiteRT\n")
}
