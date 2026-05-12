package com.gyansetu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gyansetu.viewmodel.AppViewModel
import kotlinx.coroutines.delay

/**
 * Floating top-right overlay that shows live LiteRT engine info while the user
 * navigates the app. Toggleable via Settings → "Debug overlay". This is the
 * judge-facing proof that we're not just calling a black-box "run AI" function
 * — we know which backend, which variant, how many tokens per session.
 */
@Composable
fun DebugOverlay(viewModel: AppViewModel) {
    var info by remember { mutableStateOf(viewModel.engineDebug()) }
    LaunchedEffect(Unit) {
        while (true) { delay(700); info = viewModel.engineDebug() }
    }
    Box(Modifier.fillMaxWidth().padding(top = 6.dp, end = 6.dp), contentAlignment = Alignment.TopEnd) {
        Column(
            modifier = Modifier
                .background(Color(0xCC000000), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            DebugRow("LiteRT", info.backend)
            DebugRow("Variant", info.variant)
            DebugRow("File", info.modelFile.take(28))
            DebugRow("Tokens", info.sessionTokens.toString())
        }
    }
}

@Composable
private fun DebugRow(k: String, v: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$k:",
            fontSize = 9.sp, fontWeight = FontWeight.Medium,
            color = Color(0x99FFFFFF), fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            v,
            fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
            color = Color.White, fontFamily = FontFamily.Monospace,
        )
    }
}
