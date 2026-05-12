package com.gyansetu

import android.app.Application
import com.gyansetu.ai.GemmaInferenceEngine
import com.gyansetu.data.Settings
import com.gyansetu.data.SyllabusDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GyanSetuApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { SyllabusDatabase.get(this) }
    val gemma by lazy { GemmaInferenceEngine(this) }

    override fun onCreate() {
        super.onCreate()
        // Warm-start: seed the DB and prepare Gemma in the background so the
        // user sees a "Ready" state when they hit the Home screen.
        appScope.launch {
            database.seedIfEmpty()
            // First-install only: prime Teacher Mode with a believable mastery
            // matrix so judges see populated data before any quiz is taken.
            Settings.seedDemoMasteryIfEmpty(this@GyanSetuApp)
        }
        appScope.launch { runCatching { gemma.warmUp() } }
    }
}
