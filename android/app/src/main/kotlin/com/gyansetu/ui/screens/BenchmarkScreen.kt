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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gyansetu.ui.components.ChunkyButton
import com.gyansetu.ui.theme.GyanColors
import com.gyansetu.viewmodel.AppViewModel

/**
 * On-device LiteRT benchmark. Runs three Gemma 4 prompts and reports:
 *   - first-token latency (ms)
 *   - end-to-end wall time (ms)
 *   - approximate tokens generated
 *   - tokens per second
 *   - peak heap delta (MB)
 *
 * The numbers feed directly into the DEVPOST narrative; running this on a
 * real device gives you the LiteRT-Prize-grade evidence you need.
 */
@Composable
fun BenchmarkScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val samples by viewModel.bench.collectAsState()
    val running by viewModel.benchRunning.collectAsState()
    val dbg = remember { viewModel.engineDebug() }

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
            Column {
                Text("⚡ LiteRT બેન્ચમાર્ક", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                Text("LiteRT benchmark · runs Gemma 4 on this device",
                    fontSize = 12.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)
            }
        }

        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Engine info card
            Column(
                Modifier.fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(2.5.dp, GyanColors.Ink, RoundedCornerShape(20.dp))
                    .padding(14.dp),
            ) {
                Text("Engine", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.InkSoft)
                Spacer(Modifier.size(6.dp))
                InfoRow("Backend", dbg.backend)
                InfoRow("Variant", dbg.variant)
                InfoRow("Model file", dbg.modelFile)
                InfoRow("Session tokens", dbg.sessionTokens.toString())
            }

            ChunkyButton(
                if (running) "⏳ Running…" else "▶ Run benchmark",
                onClick = { if (!running) viewModel.runBenchmark() },
                background = if (running) GyanColors.YellowSoft else GyanColors.Saffron,
                contentColor = if (running) GyanColors.Ink else Color.White,
                modifier = Modifier.fillMaxWidth(),
            )

            samples.forEachIndexed { i, s -> SampleCard(i + 1, s) }

            if (samples.size >= 3) {
                val avgFirst = samples.map { it.firstTokenMs }.average().toLong()
                val avgTps   = samples.map { it.tokensPerSec.toDouble() }.average().toFloat()
                val peakRam  = samples.maxOfOrNull { it.peakRamMb } ?: 0L
                Column(
                    Modifier.fillMaxWidth()
                        .background(GyanColors.GreenSoft, RoundedCornerShape(20.dp))
                        .border(2.5.dp, GyanColors.Green, RoundedCornerShape(20.dp))
                        .padding(14.dp),
                ) {
                    Text("📊 Summary", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.GreenDeep)
                    Spacer(Modifier.size(6.dp))
                    InfoRow("Avg first-token latency", "$avgFirst ms")
                    InfoRow("Avg throughput", String.format("%.1f tok/s", avgTps))
                    InfoRow("Peak heap delta", "$peakRam MB")
                }
            }

            Spacer(Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SampleCard(idx: Int, s: com.gyansetu.ai.GemmaInferenceEngine.BenchSample) {
    Column(
        Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(2.dp, GyanColors.Ink.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .padding(12.dp),
    ) {
        Text(
            "Run $idx · \"${s.prompt.take(50)}${if (s.prompt.length > 50) "…" else ""}\"",
            fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.InkSoft,
        )
        Spacer(Modifier.size(6.dp))
        InfoRow("First token", "${s.firstTokenMs} ms")
        InfoRow("Total time", "${s.totalMs} ms")
        InfoRow("Tokens (~)", s.tokensApprox.toString())
        InfoRow("Throughput", String.format("%.1f tok/s", s.tokensPerSec))
        InfoRow("Heap delta", "${s.peakRamMb} MB")
    }
}

@Composable
private fun InfoRow(k: String, v: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(k, fontSize = 12.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.Medium)
        Text(v, fontSize = 13.sp, color = GyanColors.Ink, fontWeight = FontWeight.ExtraBold)
    }
}
