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
import com.gyansetu.ui.components.ChunkyButton
import com.gyansetu.ui.theme.GyanColors
import com.gyansetu.viewmodel.AppViewModel

@Composable
fun SettingsScreen(viewModel: AppViewModel, onBack: () -> Unit, onOpenBenchmark: () -> Unit = {}) {
    val s by viewModel.settings.collectAsState()

    Column(Modifier.fillMaxSize().background(GyanColors.Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)) {
            Box(
                Modifier.size(44.dp)
                    .background(Color.White, CircleShape)
                    .border(2.5.dp, GyanColors.Ink, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GyanColors.Ink) }
            Spacer(Modifier.size(12.dp))
            Column {
                Text("સેટિંગ્સ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                Text("Settings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GyanColors.InkSoft)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Language
            Card("🌐 ભાષા · Language") {
                Segmented(
                    options = listOf("gu" to "ગુજરાતી", "both" to "બન્ને", "en" to "English"),
                    selected = s.lang,
                    onSelect = { viewModel.setLang(it) },
                )
            }

            // Font size
            Card("🔤 અક્ષર કદ · Text size") {
                Segmented(
                    options = listOf("regular" to "સામાન્ય", "large" to "મોટું", "xl" to "ખૂબ મોટું"),
                    selected = s.fontSize,
                    onSelect = { viewModel.setFontSize(it) },
                )
            }

            // Sound toggle
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(2.5.dp, GyanColors.Ink, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("🔊 અવાજ · Sound", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                    Text("Voice & sound effects", fontSize = 12.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.Medium)
                }
                Box(
                    Modifier.size(56.dp, 32.dp)
                        .background(if (s.sound) GyanColors.Green else Color(0xFFCCCCCC), RoundedCornerShape(16.dp))
                        .border(2.dp, GyanColors.Ink, RoundedCornerShape(16.dp))
                        .clickable { viewModel.setSound(!s.sound) },
                ) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .padding(2.dp)
                            .background(Color.White, CircleShape)
                            .border(1.5.dp, GyanColors.Ink, CircleShape)
                            .align(if (s.sound) Alignment.CenterEnd else Alignment.CenterStart),
                    )
                }
            }

            // Storage display
            Card("💾 સ્ટોરેજ · Storage") {
                Text(
                    "Gemma 4 model: 2.0 GB · downloaded once on first launch",
                    fontSize = 12.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(10.dp)
                        .background(GyanColors.Bg, RoundedCornerShape(5.dp))
                        .border(1.5.dp, GyanColors.Ink, RoundedCornerShape(5.dp))
                ) {
                    Box(Modifier.fillMaxWidth(0.42f).height(10.dp)
                        .background(GyanColors.Saffron, RoundedCornerShape(5.dp)))
                }
            }

            // App language — multi-language equity story
            Card("🌍 App language · ઍપ ભાષા") {
                Text(
                    "Switch the entire UI between five Indian languages. The Gemma 4 " +
                        "tutor follows along — answers and tools shift to your mother tongue.",
                    fontSize = 11.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf(
                        "gu" to "ગુ",
                        "hi" to "हि",
                        "mr" to "मरा",
                        "ta" to "தமி",
                        "en" to "EN",
                    ),
                    selected = if (s.appLang == "system") "gu" else s.appLang,
                    onSelect = { viewModel.setAppLang(it) },
                )
            }

            // Reading level — adaptive difficulty for the agent
            Card("📚 Reading level · શિક્ષણ સ્તર") {
                Text(
                    "The tutor adjusts vocabulary to the chosen grade level.",
                    fontSize = 11.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf(
                        "1-2" to "Std 1-2",
                        "3-4" to "Std 3-4",
                        "5"   to "Std 5",
                    ),
                    selected = s.readingLevel,
                    onSelect = { viewModel.setReadingLevel(it) },
                )
            }

            // Accessibility row — high contrast + dyslexia-friendly font
            Card("♿ Accessibility · સુલભતા") {
                AccessibilityRow(
                    title = "🔆 High-contrast theme",
                    subtitle = "WCAG-AAA palette for low-vision kids",
                    on = s.highContrast,
                    onChange = { viewModel.setHighContrast(it) },
                )
                Spacer(Modifier.height(8.dp))
                AccessibilityRow(
                    title = "🔤 Dyslexia-friendly font",
                    subtitle = "OpenDyslexic typeface (drop .ttf in res/font/)",
                    on = s.dyslexia,
                    onChange = { viewModel.setDyslexia(it) },
                )
            }

            // Debug overlay toggle
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .border(2.5.dp, GyanColors.Ink, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("🔬 Debug overlay", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
                    Text("Live LiteRT backend / variant / token counter on every screen",
                        fontSize = 12.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.Medium)
                }
                Box(
                    Modifier.size(56.dp, 32.dp)
                        .background(if (s.debug) GyanColors.Green else Color(0xFFCCCCCC), RoundedCornerShape(16.dp))
                        .border(2.dp, GyanColors.Ink, RoundedCornerShape(16.dp))
                        .clickable { viewModel.setDebug(!s.debug) },
                ) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .padding(2.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.5.dp, GyanColors.Ink, RoundedCornerShape(12.dp))
                            .align(if (s.debug) Alignment.CenterEnd else Alignment.CenterStart),
                    )
                }
            }

            // Open benchmark
            ChunkyButton(
                "⚡ Run LiteRT benchmark",
                onClick = onOpenBenchmark,
                background = GyanColors.Saffron, contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            )

            // Reset stars
            ChunkyButton(
                "♻️ તારા રીસેટ કરો · Reset progress",
                onClick = { viewModel.resetStars() },
                background = Color.White, contentColor = GyanColors.Pink,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.size(12.dp))
            Text(
                "GyanSetu v1.0 · Powered by Gemma 4 + LiteRT · સંપૂર્ણ ઑફલાઇન",
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GyanColors.InkSoft,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(20.dp))
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(2.5.dp, GyanColors.Ink, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
        Spacer(Modifier.size(10.dp))
        content()
    }
}

@Composable
private fun AccessibilityRow(title: String, subtitle: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GyanColors.Ink)
            Text(subtitle, fontSize = 11.sp, color = GyanColors.InkSoft, fontWeight = FontWeight.Medium)
        }
        Box(
            Modifier.size(48.dp, 28.dp)
                .background(if (on) GyanColors.Green else Color(0xFFCCCCCC), RoundedCornerShape(14.dp))
                .border(2.dp, GyanColors.Ink, RoundedCornerShape(14.dp))
                .clickable { onChange(!on) },
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .padding(2.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(1.5.dp, GyanColors.Ink, RoundedCornerShape(10.dp))
                    .align(if (on) Alignment.CenterEnd else Alignment.CenterStart),
            )
        }
    }
}

@Composable
private fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(GyanColors.Bg, RoundedCornerShape(12.dp))
            .border(1.5.dp, GyanColors.Ink.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(4.dp),
    ) {
        options.forEach { (id, label) ->
            val isOn = id == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isOn) GyanColors.Saffron else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(id) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOn) Color.White else GyanColors.Ink,
                )
            }
        }
    }
}
