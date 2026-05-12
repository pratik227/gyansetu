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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gyansetu.ui.theme.GyanColors
import com.gyansetu.viewmodel.AppViewModel

private data class Achievement(val id: String, val gu: String, val en: String, val emoji: String, val unlock: (Int, Int) -> Boolean)

private val ACHIEVEMENTS = listOf(
    Achievement("first",  "પહેલો તારો", "First star",   "⭐",  { stars, _ -> stars >= 1 }),
    Achievement("ten",    "૧૦ તારા",     "10 stars",    "🌱",  { stars, _ -> stars >= 10 }),
    Achievement("fifty",  "૫૦ તારા",     "50 stars",    "🌟",  { stars, _ -> stars >= 50 }),
    Achievement("century","૧૦૦ તારા",   "100 stars",    "💯",  { stars, _ -> stars >= 100 }),
    Achievement("s3",     "૩ સળંગ",      "3 streak",    "🔥",  { _, best -> best >= 3 }),
    Achievement("s7",     "૭ સળંગ",      "7 streak",    "🚀",  { _, best -> best >= 7 }),
    Achievement("matcher","જોડી રાજા",  "Match king",  "🧩",  { stars, _ -> stars >= 5 }),
    Achievement("curious","જિજ્ઞાસુ",   "Curious",     "🦉",  { stars, _ -> stars >= 20 }),
)

@Composable
fun DashboardScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val s by viewModel.settings.collectAsState()
    val unlocked = ACHIEVEMENTS.filter { it.unlock(s.stars, s.bestStreak) }.map { it.id }.toSet()

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
                Text("કિરણની પ્રગતિ", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                Text("Kiran's Progress · Std 3", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GyanColors.InkSoft)
            }
            Box(
                Modifier.size(44.dp)
                    .background(GyanColors.Saffron, CircleShape)
                    .border(2.5.dp, GyanColors.Ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("કિ", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White) }
        }

        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Stats grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().height(220.dp),
            ) {
                items(
                    listOf(
                        StatCard("તારા · Stars",  s.stars.toString(),       "⭐", GyanColors.YellowSoft, GyanColors.Yellow),
                        StatCard("સળંગ · Streak", s.streak.toString(),      "🔥", GyanColors.PinkSoft,   GyanColors.Pink),
                        StatCard("શ્રેષ્ઠ · Best", s.bestStreak.toString(), "🚀", GyanColors.GreenSoft,  GyanColors.Green),
                        StatCard("બેજ · Badges",  "${unlocked.size}/${ACHIEVEMENTS.size}", "🏅", GyanColors.PurpleSoft, GyanColors.Purple),
                    ),
                ) { c ->
                    Column(
                        Modifier.fillMaxWidth().height(100.dp)
                            .background(c.bg, RoundedCornerShape(18.dp))
                            .border(2.dp, c.border, RoundedCornerShape(18.dp))
                            .padding(12.dp),
                    ) {
                        Text(c.icon, fontSize = 22.sp)
                        Text(c.value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                        Text(c.label, fontSize = 11.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Achievements
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(2.5.dp, GyanColors.Ink, RoundedCornerShape(20.dp))
                    .padding(14.dp),
            ) {
                Text(
                    "🏅 પ્રાપ્ત બેજ · Achievements (${unlocked.size}/${ACHIEVEMENTS.size})",
                    fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink,
                )
                Spacer(Modifier.size(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                ) {
                    items(ACHIEVEMENTS) { a ->
                        val got = a.id in unlocked
                        Column(
                            modifier = Modifier.fillMaxWidth().height(72.dp)
                                .background(
                                    if (got) GyanColors.YellowSoft else Color(0xFFF5F0E8),
                                    RoundedCornerShape(12.dp),
                                )
                                .border(
                                    2.dp,
                                    if (got) GyanColors.Yellow else GyanColors.Ink.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp),
                                )
                                .padding(4.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(a.emoji, fontSize = 22.sp)
                            Text(
                                a.gu,
                                fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                color = if (got) GyanColors.Ink else GyanColors.InkSoft,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.size(12.dp))
        }
    }
}

private data class StatCard(val label: String, val value: String, val icon: String, val bg: Color, val border: Color)
