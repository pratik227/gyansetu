package com.gyansetu.ui.screens

import android.speech.tts.TextToSpeech
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gyansetu.data.SyllabusEntity
import com.gyansetu.ui.components.ChunkyButton
import com.gyansetu.ui.theme.GyanColors
import com.gyansetu.viewmodel.AppViewModel
import java.util.Locale

private data class TopicMeta(
    val id: String, val gu: String, val en: String, val emoji: String,
    val color: Color, val deep: Color,
)

private val TOPICS = listOf(
    TopicMeta("animals",    "પ્રાણીઓ",        "Animals",     "🐾", Color(0xFFFFB85C), GyanColors.SaffronDeep),
    TopicMeta("fruits",     "ફળો",             "Fruits",      "🍎", GyanColors.Pink,    Color(0xFFD14570)),
    TopicMeta("classroom",  "વર્ગ",            "Classroom",   "📚", GyanColors.Purple,  Color(0xFF6E3FAB)),
    TopicMeta("numbers",    "સંખ્યા",          "Numbers",     "🔢", GyanColors.Yellow,  Color(0xFFD9A800)),
    TopicMeta("facts",      "જનરલ નોલેજ",     "GK Facts",    "💡", GyanColors.Sky,     Color(0xFF0288D1)),
)

@Composable
fun TopicsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var pickedTopic by remember { mutableStateOf<TopicMeta?>(null) }
    var pickedItem by remember { mutableStateOf<SyllabusEntity?>(null) }
    var items by remember { mutableStateOf<List<SyllabusEntity>>(emptyList()) }

    val ctx = LocalContext.current
    val tts = remember {
        var ref: TextToSpeech? = null
        ref = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ref?.language = Locale("gu", "IN")
                if (ref?.language?.language != "gu") ref?.language = Locale("hi", "IN")
                if (ref?.language?.language != "hi") ref?.language = Locale.US
            }
        }
        ref
    }
    DisposableEffect(Unit) { onDispose { tts?.stop(); tts?.shutdown() } }

    // Load items from Room when a topic is picked
    LaunchedEffect(pickedTopic) {
        pickedTopic?.let { items = viewModel.topicItems(it.id) }
    }

    // Auto-speak when an item opens
    LaunchedEffect(pickedItem) {
        pickedItem?.let {
            val msg = "${it.en}. ${it.gu}. ${it.storyEn ?: ""}. ${it.storyGu ?: ""}"
            tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "topic-${it.en}")
        }
    }

    when {
        pickedItem != null -> ItemDetail(
            viewModel = viewModel,
            item = pickedItem!!,
            onBack = { pickedItem = null },
            onListen = {
                val it = pickedItem!!
                val msg = "${it.en}. ${it.gu}. ${it.storyEn ?: ""}. ${it.storyGu ?: ""}"
                tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "topic-replay")
            },
        )
        pickedTopic != null -> ItemList(
            topic = pickedTopic!!,
            items = items,
            onBack = { pickedTopic = null },
            onPick = { pickedItem = it },
        )
        else -> CategoryGrid(
            onBack = onBack,
            onPick = { pickedTopic = it },
        )
    }
}

@Composable
private fun CategoryGrid(onBack: () -> Unit, onPick: (TopicMeta) -> Unit) {
    Column(Modifier.fillMaxSize().background(GyanColors.Bg)) {
        Header("વિષયો", "Pick a topic to explore", onBack)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        ) {
            items(TOPICS) { t ->
                Column(
                    modifier = Modifier.fillMaxWidth().height(130.dp)
                        .background(t.color, RoundedCornerShape(22.dp))
                        .border(3.dp, GyanColors.Ink, RoundedCornerShape(22.dp))
                        .clickable { onPick(t) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(t.emoji, fontSize = 40.sp)
                    Spacer(Modifier.size(6.dp))
                    Text(t.gu, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(t.en, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ItemList(topic: TopicMeta, items: List<SyllabusEntity>, onBack: () -> Unit, onPick: (SyllabusEntity) -> Unit) {
    Column(Modifier.fillMaxSize().background(GyanColors.Bg)) {
        Header("${topic.emoji} ${topic.gu}", "${topic.en} · ${items.size} વસ્તુ", onBack)
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No items yet", color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            ) {
                items(items) { it ->
                    Column(
                        modifier = Modifier.fillMaxWidth().height(130.dp)
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .border(3.dp, topic.deep, RoundedCornerShape(20.dp))
                            .clickable { onPick(it) }
                            .padding(10.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(it.icon ?: "✨", fontSize = 40.sp)
                        Spacer(Modifier.size(4.dp))
                        Text(it.gu, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                        Text(it.en, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GyanColors.InkSoft)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemDetail(
    viewModel: AppViewModel,
    item: SyllabusEntity,
    onBack: () -> Unit,
    onListen: () -> Unit,
) {
    // Stream a fresh "tell me more" explanation every time the kid opens an
    // item — different wording each visit keeps revisits interesting.
    var liveExplanation by remember(item.id) { mutableStateOf("") }
    var streamDone by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) {
        liveExplanation = ""
        streamDone = false
        runCatching {
            viewModel.streamItemExplanation(en = item.en, gu = item.gu).collect { partial ->
                liveExplanation = partial
            }
        }
        streamDone = true
    }
    Column(
        Modifier.fillMaxSize().background(GyanColors.SaffronSoft),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                Modifier.size(44.dp)
                    .background(Color.White, CircleShape)
                    .border(2.5.dp, GyanColors.Ink, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GyanColors.Ink) }
        }
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(180.dp)
                    .background(Color.White, RoundedCornerShape(28.dp))
                    .border(3.dp, GyanColors.Ink, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(item.icon ?: "✨", fontSize = 88.sp) }

            Text(item.en.uppercase(), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
            Text(item.gu, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.SaffronDeep)
            if (!item.phon.isNullOrBlank())
                Text(item.phon, fontSize = 13.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.Medium)

            if (item.storyEn != null || item.storyGu != null) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .border(2.dp, GyanColors.Saffron, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                ) {
                    item.storyGu?.let {
                        Text(it, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GyanColors.Ink)
                    }
                    item.storyEn?.let {
                        Text(it, fontSize = 13.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Gemma-streamed "Tell me more" — fresh every visit.
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(2.dp, GyanColors.Green, RoundedCornerShape(20.dp))
                    .padding(14.dp),
            ) {
                Text(
                    if (streamDone) "✨ વધુ જાણો · Tell me more"
                    else "✨ વિચારી રહ્યું છે… · Thinking…",
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Green,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = liveExplanation.ifBlank { "GyanSetu is preparing a fresh story for you…" },
                    fontSize = 14.sp,
                    fontWeight = if (liveExplanation.isBlank()) FontWeight.Medium else FontWeight.SemiBold,
                    color = GyanColors.Ink,
                )
            }

            ChunkyButton(
                "🔊 સાંભળો · Listen again",
                onClick = onListen,
                background = Color.White,
                contentColor = GyanColors.SaffronDeep,
            )
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String, onBack: () -> Unit) {
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
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
            Text(subtitle, fontSize = 13.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
        }
        Text("🦉", fontSize = 32.sp)
    }
}
