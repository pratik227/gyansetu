package com.gyansetu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gyansetu.ui.components.ChunkyButton
import com.gyansetu.ui.theme.GyanColors
import com.gyansetu.viewmodel.AppViewModel
import com.gyansetu.viewmodel.EngineState

/**
 * Subscribes to AppViewModel.engine. Shows download progress (3 GB Gemma 4
 * model on first launch), warm-up text, or error. Calls [onReady] once the
 * engine reaches the Ready state.
 */
@Composable
fun LoadingScreen(viewModel: AppViewModel, onReady: () -> Unit) {
    val state by viewModel.engine.collectAsState()

    LaunchedEffect(state) { if (state is EngineState.Ready) onReady() }

    Box(
        Modifier.fillMaxSize().background(GyanColors.Bg).padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🦉", fontSize = 100.sp)
            Spacer(Modifier.height(16.dp))
            Text("જ્ઞાનસેતુ", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
            Spacer(Modifier.height(4.dp))

            val (gu, en, pct) = describe(state)
            Text(gu, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GyanColors.Ink)
            Text(en, fontSize = 13.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.height(28.dp))
            Box(
                Modifier.fillMaxWidth(0.85f).height(22.dp)
                    .background(Color.White, RoundedCornerShape(999.dp))
                    .border(3.dp, GyanColors.Ink, RoundedCornerShape(999.dp))
            ) {
                Box(
                    Modifier.fillMaxWidth(pct.coerceIn(0f, 1f)).height(22.dp).background(
                        Brush.horizontalGradient(listOf(GyanColors.Yellow, GyanColors.Saffron, GyanColors.Green)),
                        RoundedCornerShape(999.dp),
                    )
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${(pct * 100).toInt()}%",
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.SaffronDeep,
            )

            if (state is EngineState.Error) {
                Spacer(Modifier.height(20.dp))
                Text(
                    (state as EngineState.Error).message,
                    fontSize = 13.sp, color = GyanColors.Pink, fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                ChunkyButton("ફરી પ્રયત્ન કરો · Retry",
                    onClick = { viewModel.prepareEngine() },
                    background = GyanColors.Saffron, contentColor = Color.White)
            }
        }
    }
}

private fun describe(state: EngineState): Triple<String, String, Float> = when (state) {
    EngineState.Idle -> Triple("તૈયારી કરી રહ્યું છે...", "Preparing…", 0.05f)
    is EngineState.Downloading -> Triple(
        "Gemma 4 ડાઉનલોડ થઈ રહ્યું છે...",
        "Downloading Gemma 4 (one-time, ~3 GB)…",
        state.progress,
    )
    EngineState.WarmingUp -> Triple("મોડેલ ગરમ થઈ રહ્યું છે...", "Warming up the offline brain…", 0.95f)
    EngineState.Ready -> Triple("તૈયાર!", "Ready!", 1f)
    is EngineState.Error -> Triple("કંઈક ખોટું થયું", "Error", 0f)
}
