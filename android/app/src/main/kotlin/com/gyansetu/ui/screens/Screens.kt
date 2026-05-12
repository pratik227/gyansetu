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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gyansetu.data.ContentState
import com.gyansetu.data.activeStudent
import com.gyansetu.ui.components.ChunkyButton
import com.gyansetu.ui.theme.GyanColors
import com.gyansetu.viewmodel.AppViewModel

/* ────────── Splash (no engine deps) ────────── */
@Composable
fun SplashScreen(onContinue: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFFFB85C), GyanColors.Saffron, GyanColors.SaffronDeep))
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🦉", fontSize = 120.sp)
            Spacer(Modifier.height(16.dp))
            Text("જ્ઞાનસેતુ", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("GyanSetu", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "તમારો શીખવાનો સાથી · Your Learning Friend",
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
            )
            Spacer(Modifier.height(36.dp))
            ChunkyButton(
                "ચાલો શરૂ કરીએ! · Let's Start!",
                onClick = onContinue,
                background = Color.White,
                contentColor = GyanColors.SaffronDeep,
            )
        }
    }
}

/* ────────── Home ────────── */
private data class Tile(val id: String, val gu: String, val en: String, val emoji: String, val color: Color)

@Composable
fun HomeScreen(viewModel: AppViewModel, onNavigate: (String) -> Unit) {
    val tiles = listOf(
        Tile("scan",   "ફોટો જુઓ",   "Camera Scan",  "📷", GyanColors.Saffron),
        Tile("ask",    "પ્રશ્ન પૂછો", "Ask Question", "❓", GyanColors.Green),
        Tile("topics", "વિષયો",       "Topics",        "📚", GyanColors.Sky),
        Tile("match",  "જોડી મેળવો",  "Match Game",   "🧩", GyanColors.Pink),
        Tile("quiz",   "જ્ઞાન રમત",   "GK Quiz",       "🏆", GyanColors.Purple),
    )
    val tipState by viewModel.dailyTip.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val student = activeStudent(settings.activeStudent)
    Column(Modifier.fillMaxSize().background(GyanColors.Bg).padding(16.dp)) {
        // Top bar — profile chip on the left, action icons on the right.
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Profile chip — tap to open Teacher Mode (where student switching lives).
            Row(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(999.dp))
                    .border(2.5.dp, GyanColors.Ink, RoundedCornerShape(999.dp))
                    .clickable { onNavigate("dashboard") }
                    .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(36.dp)
                        .background(GyanColors.Saffron, CircleShape)
                        .border(2.dp, GyanColors.Ink, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        student.gu.first().toString(),
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(student.name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                    Text(student.grade, fontSize = 10.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.weight(1f))
            HomeIconButton(icon = "👩‍🏫", label = "Teacher") { onNavigate("dashboard") }
            Spacer(Modifier.width(8.dp))
            HomeIconButton(icon = "⚙️", label = "Settings") { onNavigate("settings") }
        }
        Spacer(Modifier.height(14.dp))

        Text("નમસ્તે, ${student.gu}!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
        Text("Hello, ${student.name}!", fontSize = 14.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        // Model-generated fun-fact strip — refreshes when the kid taps it.
        DailyTipStrip(state = tipState, onRefresh = { viewModel.regenerateDailyTip() })
        Spacer(Modifier.height(12.dp))

        tiles.forEach { tile ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    .background(tile.color, RoundedCornerShape(20.dp))
                    .border(3.dp, GyanColors.Ink, RoundedCornerShape(20.dp))
                    .clickable { onNavigate(tile.id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tile.emoji, fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tile.gu, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(tile.en, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Text("›", fontSize = 28.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun HomeIconButton(icon: String, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.5.dp, GyanColors.Ink, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 20.sp)
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.InkSoft)
    }
}

@Composable
private fun DailyTipStrip(
    state: ContentState<com.gyansetu.data.DailyTip>,
    onRefresh: () -> Unit,
) {
    val (gu, en, isFresh) = when (state) {
        is ContentState.Ready -> Triple(state.data.gu, state.data.en, true)
        is ContentState.Failed -> Triple(state.fallback.gu, state.fallback.en, false)
        ContentState.Loading -> Triple("આજનું કુતૂહલ આવી રહ્યું છે…", "Today's curiosity coming up…", false)
    }
    Row(
        Modifier.fillMaxWidth()
            .background(GyanColors.YellowSoft, RoundedCornerShape(18.dp))
            .border(2.dp, GyanColors.Yellow, RoundedCornerShape(18.dp))
            .clickable { onRefresh() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (isFresh) "✨" else "💡", fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(gu, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
            Text(en, fontSize = 11.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
        }
        Text("🔄", fontSize = 16.sp)
    }
}

/* All other screens live in their own files (Topics/Match/Settings/Dashboard,
 * Camera/Quiz/Results/Ask/Loading). Only Splash + Home stay here. */
