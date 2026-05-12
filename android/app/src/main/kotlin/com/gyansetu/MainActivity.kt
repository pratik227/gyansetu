package com.gyansetu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gyansetu.ui.components.DebugOverlay
import com.gyansetu.ui.screens.AskScreen
import com.gyansetu.ui.screens.BenchmarkScreen
import com.gyansetu.ui.screens.CameraScanScreen
import com.gyansetu.ui.screens.DashboardScreen
import com.gyansetu.ui.screens.HomeScreen
import com.gyansetu.ui.screens.LoadingScreen
import com.gyansetu.ui.screens.MatchScreen
import com.gyansetu.ui.screens.QuizScreen
import com.gyansetu.ui.screens.ResultsScreen
import com.gyansetu.ui.screens.SettingsScreen
import com.gyansetu.ui.screens.SplashScreen
import com.gyansetu.ui.screens.TeacherModeScreen
import com.gyansetu.ui.screens.TopicsScreen
import com.gyansetu.ui.theme.GyanSetuTheme
import com.gyansetu.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GyanSetuTheme {
                Surface(modifier = Modifier.fillMaxSize().background(Color(0xFFFFFAF2))) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot(viewModel: AppViewModel = viewModel()) {
    var screen by remember { mutableStateOf("splash") }
    var quizStars by remember { mutableStateOf(0) }
    var quizTotal by remember { mutableStateOf(0) }
    val settings by viewModel.settings.collectAsState()

    // Apply per-app locale whenever the user changes appLang in Settings. Uses
    // AppCompatDelegate which is the cross-API surface (API 33+ would otherwise
    // use the system LocaleManager). Call is idempotent.
    LaunchedEffect(settings.appLang) {
        val tag = if (settings.appLang == "system") "" else settings.appLang
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    Box(Modifier.fillMaxSize()) {
        when (screen) {
            "splash"    -> SplashScreen { screen = "loading" }
            "loading"   -> LoadingScreen(viewModel) { screen = "home" }
            "home"      -> HomeScreen(viewModel = viewModel) { screen = it }
            "topics"    -> TopicsScreen(viewModel = viewModel) { screen = "home" }
            "match"     -> MatchScreen(
                viewModel = viewModel,
                onBack = { screen = "home" },
                onWin = { n -> quizStars = n; quizTotal = n; screen = "results" },
            )
            "scan"      -> CameraScanScreen(viewModel = viewModel) {
                viewModel.resetScan()
                screen = "home"
            }
            "ask"       -> AskScreen(viewModel = viewModel) {
                viewModel.clearAsk()
                screen = "home"
            }
            "quiz"      -> QuizScreen(
                viewModel = viewModel,
                onBack = { screen = "home" },
                onComplete = { s, t ->
                    quizStars = s; quizTotal = t
                    viewModel.addStars(s)
                    if (s == t) viewModel.bumpStreak() else viewModel.resetStreak()
                    screen = "results"
                },
                onAnswered = { topic, correct ->
                    // Per-question topic mastery feeds Teacher Mode + adaptive selection.
                    viewModel.bumpMastery(topic, if (correct) +1 else -1)
                },
                masteryProvider = { settings.topicMastery },
            )
            "results"   -> ResultsScreen(
                viewModel = viewModel,
                stars = quizStars,
                total = quizTotal,
                onHome = { screen = "home" },
                onRetry = { screen = "quiz" },
            )
            "settings"  -> SettingsScreen(
                viewModel = viewModel,
                onBack = { screen = "home" },
                onOpenBenchmark = { screen = "bench" },
            )
            "dashboard" -> TeacherModeScreen(viewModel = viewModel) { screen = "home" }
            "bench"     -> BenchmarkScreen(viewModel = viewModel) { screen = "settings" }
        }
        // Floating debug overlay — only when the user opted in via Settings.
        if (settings.debug) DebugOverlay(viewModel)
    }
}
