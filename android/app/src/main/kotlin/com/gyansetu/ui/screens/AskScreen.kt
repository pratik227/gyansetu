package com.gyansetu.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.gyansetu.ai.ContentGenerator
import com.gyansetu.data.AskSuggestion
import com.gyansetu.data.ContentState
import com.gyansetu.ui.components.ChunkyButton
import com.gyansetu.ui.theme.GyanColors
import com.gyansetu.viewmodel.AppViewModel
import java.util.Locale

/**
 * Ask flow with text + voice + Gemma-4 streaming + bilingual TTS playback.
 * Tap a preset, type a question, or hold mic to speak — all answered on-device.
 */
@Composable
fun AskScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val ask by viewModel.ask.collectAsState()
    val recording by viewModel.isRecording.collectAsState()
    val suggestionsState by viewModel.askSuggestions.collectAsState()
    var input by remember { mutableStateOf("") }
    val ctx = LocalContext.current

    LaunchedEffect(Unit) { viewModel.ensureAskSuggestions() }

    // Lazy-init TTS once per screen lifetime, release on dispose.
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

    // Auto-speak the streamed answer once it's done.
    LaunchedEffect(ask?.done) {
        if (ask?.done == true && !ask?.partial.isNullOrBlank()) {
            tts?.speak(ask!!.partial, TextToSpeech.QUEUE_FLUSH, null, "gyansetu-ans")
        }
    }

    val recordPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.listenAndAsk() }

    val onMicTap = {
        if (recording) {
            viewModel.stopRecording()
        } else {
            val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.listenAndAsk() else recordPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(Modifier.fillMaxSize().background(GyanColors.Bg).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChunkyButton("←", onClick = onBack, background = Color.White, contentColor = GyanColors.Ink)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("પ્રશ્ન પૂછો", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                Text("Ask GyanSetu — runs offline on Gemma 4",
                    fontSize = 12.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
            }
            Text("🦉", fontSize = 32.sp)
        }
        Spacer(Modifier.height(16.dp))

        // Conversation area
        Column(Modifier.weight(1f)) {
            if (ask == null) {
                val (suggestions, isFresh) = when (val s = suggestionsState) {
                    is ContentState.Ready -> s.data to true
                    is ContentState.Failed -> s.fallback to false
                    ContentState.Loading -> ContentGenerator.staticAskSuggestions to false
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isFresh) "✨ સૂચનો · Fresh ideas" else "💭 સૂચનો · Try these",
                        fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                        color = GyanColors.InkSoft,
                        modifier = Modifier.weight(1f),
                    )
                    if (suggestionsState !is ContentState.Loading) {
                        Box(
                            Modifier
                                .background(Color.White, RoundedCornerShape(14.dp))
                                .border(2.dp, GyanColors.Saffron, RoundedCornerShape(14.dp))
                                .clickable { viewModel.regenerateAskSuggestions() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "🔄 New",
                                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                color = GyanColors.SaffronDeep,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                suggestions.forEach { sug ->
                    SuggestionRow(sug.gu, sug.en) {
                        viewModel.askText(sug.en)
                        // Kick off the next batch in the background — by the time
                        // the kid finishes reading the answer and comes back, fresh
                        // suggestions are already prepared.
                        viewModel.regenerateAskSuggestions()
                    }
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                AnswerCard(
                    question = ask!!.question,
                    partial = ask!!.partial,
                    done = ask!!.done,
                    toolSteps = ask!!.toolSteps,
                )
            }
        }

        // Input row
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth()
                .background(Color.White, RoundedCornerShape(22.dp))
                .border(2.5.dp, GyanColors.Ink, RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                textStyle = TextStyle(
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GyanColors.Ink,
                ),
                singleLine = true,
            )
            if (input.isNotBlank()) {
                Box(
                    Modifier.size(44.dp)
                        .background(GyanColors.Green, RoundedCornerShape(22.dp))
                        .clickable {
                            viewModel.askText(input)
                            input = ""
                        },
                    contentAlignment = Alignment.Center,
                ) { Text("➤", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) }
            } else {
                Box(
                    Modifier.size(44.dp)
                        .background(if (recording) GyanColors.Pink else GyanColors.Saffron, RoundedCornerShape(22.dp))
                        .clickable { onMicTap() },
                    contentAlignment = Alignment.Center,
                ) { Text(if (recording) "■" else "🎙", color = Color.White, fontSize = 18.sp) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (recording) "🔴 સાંભળી રહ્યું છે · Listening…"
            else "માઇક દબાવો અથવા લખો · Tap mic or type",
            fontSize = 12.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SuggestionRow(gu: String, en: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(2.dp, GyanColors.Bg, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(gu, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
            Text(en, fontSize = 12.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.Medium)
        }
        Text("›", fontSize = 22.sp, color = GyanColors.InkSoft)
    }
}

@Composable
private fun AnswerCard(
    question: String,
    partial: String,
    done: Boolean,
    toolSteps: List<com.gyansetu.ai.ToolStep>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // user bubble
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                question,
                modifier = Modifier
                    .background(GyanColors.Saffron, RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
            )
        }
        // agent tool trace — visible chips between user and model so judges see
        // multi-tool agent behavior on stage.
        toolSteps.forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(GyanColors.YellowSoft, RoundedCornerShape(14.dp))
                    .border(2.dp, GyanColors.Yellow, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text("🔧", fontSize = 16.sp)
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${step.name}(${step.argsRepr})",
                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink,
                    )
                    Text(
                        "→ ${step.result.take(120)}${if (step.result.length > 120) "…" else ""}",
                        fontSize = 11.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        // gemma bubble
        Row(verticalAlignment = Alignment.Top) {
            Text("🦉", fontSize = 28.sp)
            Spacer(Modifier.size(6.dp))
            Box(
                Modifier
                    .background(Color.White, RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                    .border(2.5.dp, GyanColors.Green, RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = partial.ifBlank { "વિચારી રહ્યું છું… · Thinking…" },
                    fontSize = 16.sp,
                    fontWeight = if (partial.isBlank()) FontWeight.Medium else FontWeight.SemiBold,
                    color = GyanColors.Ink,
                )
            }
        }
        if (!done && partial.isNotBlank()) {
            Text("✏️ બોલી રહ્યું છે · streaming…",
                fontSize = 11.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
        }
    }
}
