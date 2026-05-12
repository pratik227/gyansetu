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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.gyansetu.data.MatchPair
import com.gyansetu.ui.theme.GyanColors
import com.gyansetu.viewmodel.AppViewModel
import kotlinx.coroutines.delay

/**
 * Pair-match game: 5 fresh vocabulary pairs generated on-device by Gemma 4
 * (with a SyllabusDatabase fallback when the model is unavailable). Two
 * columns (Gujarati ↔ English), tap pairs to match. Win triggers Results.
 */
@Composable
fun MatchScreen(viewModel: AppViewModel, onBack: () -> Unit, onWin: (Int) -> Unit) {
    LaunchedEffect(Unit) { viewModel.ensureMatchContent() }
    val state by viewModel.matchContent.collectAsState()

    // Snapshot once at first non-loading state — background regen must not
    // shuffle the cards while the kid is mid-game.
    var pool by remember { mutableStateOf<List<MatchPair>?>(null) }
    LaunchedEffect(state) {
        if (pool == null) {
            pool = when (val s = state) {
                is ContentState.Ready -> s.data
                is ContentState.Failed -> s.fallback
                ContentState.Loading -> null
            }?.shuffled()?.take(5)
        }
    }

    val cards = pool
    if (cards == null) {
        MatchLoading(onBack = onBack)
        return
    }

    MatchUI(
        pool = cards,
        showFallbackBanner = state is ContentState.Failed,
        onBack = onBack,
        onWin = { award ->
            // Kick off next batch so a follow-up play sees fresh words.
            viewModel.regenerateMatchContent()
            viewModel.addStars(award)
            onWin(award)
        },
    )
}

@Composable
private fun MatchLoading(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(GyanColors.PinkSoft, GyanColors.Bg)))
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
                Text("નવા શબ્દો શોધી રહ્યા છીએ…", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                Text("Picking fresh words for you ✨", fontSize = 13.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("(this takes ~15 seconds — first time only)", fontSize = 11.sp, color = GyanColors.InkSoft)
            }
        }
    }
}

@Composable
private fun MatchUI(
    pool: List<MatchPair>,
    showFallbackBanner: Boolean,
    onBack: () -> Unit,
    onWin: (Int) -> Unit,
) {
    // Stable id = list index. Order in `pool` is fixed for the game's lifetime,
    // so an index-based id is safe for matched-tracking.
    val leftCol = remember(pool) { pool.shuffled() }
    val rightCol = remember(pool) { pool.shuffled() }
    val matched = remember { mutableStateListOf<Int>() }
    var pickedLeft by remember { mutableStateOf<Int?>(null) }
    var pickedRight by remember { mutableStateOf<Int?>(null) }
    var wrongFlash by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(pickedLeft, pickedRight) {
        val l = pickedLeft; val r = pickedRight
        if (l != null && r != null) {
            if (l == r) {
                matched += l
                pickedLeft = null
                pickedRight = null
                if (matched.size == pool.size) {
                    delay(500)
                    onWin(pool.size)
                }
            } else {
                wrongFlash = l to r
                delay(700)
                pickedLeft = null
                pickedRight = null
                wrongFlash = null
            }
        }
    }

    val matchedCount by remember { derivedStateOf { matched.size } }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(GyanColors.PinkSoft, GyanColors.Bg)))
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
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text("🧩 જોડી મેળવો", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                Text("Match the pairs · $matchedCount/${pool.size}",
                    fontSize = 12.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .background(GyanColors.GreenSoft, RoundedCornerShape(999.dp))
                    .border(2.dp, GyanColors.Green, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("$matchedCount/${pool.size}", fontWeight = FontWeight.ExtraBold,
                    color = GyanColors.GreenDeep, fontSize = 14.sp)
            }
        }

        if (showFallbackBanner) {
            Spacer(Modifier.size(6.dp))
            Text(
                "📖 Classic words (model busy)",
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.InkSoft,
            )
        }

        Spacer(Modifier.size(12.dp))
        Text(
            "ગુજરાતી ↔ English · Tap pairs to match",
            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.InkSoft,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(10.dp))

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                leftCol.forEach { item ->
                    val idx = pool.indexOf(item)
                    MatchCard(
                        label = item.gu,
                        sub = item.icon,
                        isSubAtTop = true,
                        state = stateFor(idx, matched, pickedLeft, wrongFlash?.first),
                        onClick = {
                            if (idx !in matched) pickedLeft = idx
                        },
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rightCol.forEach { item ->
                    val idx = pool.indexOf(item)
                    MatchCard(
                        label = item.en.replaceFirstChar { c -> c.uppercase() },
                        sub = null,
                        isSubAtTop = false,
                        state = stateFor(idx, matched, pickedRight, wrongFlash?.second),
                        onClick = {
                            if (idx !in matched) pickedRight = idx
                        },
                    )
                }
            }
        }
    }
}

private enum class CardState { Default, Picked, Matched, Wrong }

private fun stateFor(idx: Int, matched: List<Int>, pickedId: Int?, wrongId: Int?): CardState =
    when {
        idx in matched -> CardState.Matched
        wrongId == idx -> CardState.Wrong
        pickedId == idx -> CardState.Picked
        else -> CardState.Default
    }

@Composable
private fun MatchCard(label: String, sub: String?, isSubAtTop: Boolean, state: CardState, onClick: () -> Unit) {
    val (bg, border) = when (state) {
        CardState.Matched -> GyanColors.GreenSoft to GyanColors.Green
        CardState.Picked  -> GyanColors.SaffronSoft to GyanColors.Saffron
        CardState.Wrong   -> GyanColors.PinkSoft to GyanColors.Pink
        CardState.Default -> Color.White to GyanColors.Ink
    }
    Column(
        modifier = Modifier.fillMaxWidth().height(74.dp)
            .background(bg, RoundedCornerShape(18.dp))
            .border(3.dp, border, RoundedCornerShape(18.dp))
            .clickable(enabled = state != CardState.Matched, onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isSubAtTop && sub != null) {
            Text(sub, fontSize = 22.sp)
        }
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
    }
}
