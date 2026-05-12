package com.gyansetu.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Per-device user preferences backed by Jetpack DataStore. */
private val Context.dataStore by preferencesDataStore(name = "gyansetu")

object Settings {
    private val LANG  = stringPreferencesKey("lang")        // "gu" | "both" | "en"
    private val FONT  = stringPreferencesKey("fontSize")    // "regular" | "large" | "xl"
    private val SOUND = booleanPreferencesKey("sound")
    private val STARS = intPreferencesKey("stars")
    private val STREAK     = intPreferencesKey("streak")
    private val BEST_STREAK = intPreferencesKey("bestStreak")
    /** "task" (~2 GB MediaPipe-native) | "litertlm" (~2.5 GB LiteRT-LM higher quality) */
    private val MODEL_VARIANT = stringPreferencesKey("modelVariant")
    /** Show on-screen overlay with backend / variant / token counts. */
    private val DEBUG = booleanPreferencesKey("debug")
    /** App-level locale: "system" | "gu" | "hi" | "mr" | "ta" | "en". */
    private val APP_LANG = stringPreferencesKey("appLang")
    /** Reading level for the LLM tutor: "1-2" | "3-4" | "5". */
    private val READING_LEVEL = stringPreferencesKey("readingLevel")
    /** Accessibility: high-contrast theme + dyslexia-friendly font. */
    private val HIGH_CONTRAST = booleanPreferencesKey("highContrast")
    private val DYSLEXIA = booleanPreferencesKey("dyslexia")
    /** JSON-encoded `{"animals": 7, "fruits": 4, …}` map. 0–10 score per topic. */
    private val TOPIC_MASTERY = stringPreferencesKey("topicMastery")
    /** Active student id for Teacher Mode multi-kid support. */
    private val ACTIVE_STUDENT = stringPreferencesKey("activeStudent")
    /** One-shot flag: have we already seeded demo mastery on first install? */
    private val MASTERY_SEEDED = booleanPreferencesKey("masterySeeded")

    data class Snapshot(
        val lang: String, val fontSize: String, val sound: Boolean,
        val stars: Int, val streak: Int, val bestStreak: Int,
        val modelVariant: String = "task",
        val debug: Boolean = false,
        val appLang: String = "system",
        val readingLevel: String = "3-4",
        val highContrast: Boolean = false,
        val dyslexia: Boolean = false,
        val topicMastery: Map<String, Int> = emptyMap(),
        val activeStudent: String = "kiran",
    )

    fun stream(ctx: Context): Flow<Snapshot> = ctx.dataStore.data.map { p ->
        Snapshot(
            lang = p[LANG] ?: "both",
            fontSize = p[FONT] ?: "regular",
            sound = p[SOUND] ?: true,
            stars = p[STARS] ?: 0,
            streak = p[STREAK] ?: 0,
            bestStreak = p[BEST_STREAK] ?: 0,
            modelVariant = p[MODEL_VARIANT] ?: "task",
            debug = p[DEBUG] ?: false,
            appLang = p[APP_LANG] ?: "system",
            readingLevel = p[READING_LEVEL] ?: "3-4",
            highContrast = p[HIGH_CONTRAST] ?: false,
            dyslexia = p[DYSLEXIA] ?: false,
            topicMastery = parseMastery(p[TOPIC_MASTERY]),
            activeStudent = p[ACTIVE_STUDENT] ?: "kiran",
        )
    }

    private fun parseMastery(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            org.json.JSONObject(raw).let { obj ->
                obj.keys().asSequence().associateWith { obj.optInt(it, 0) }
            }
        }.getOrElse { emptyMap() }
    }

    private fun encodeMastery(m: Map<String, Int>): String =
        org.json.JSONObject(m as Map<*, *>).toString()

    suspend fun setModelVariant(ctx: Context, v: String) = ctx.dataStore.edit { it[MODEL_VARIANT] = v }
    suspend fun setDebug(ctx: Context, v: Boolean) = ctx.dataStore.edit { it[DEBUG] = v }
    suspend fun setAppLang(ctx: Context, v: String) = ctx.dataStore.edit { it[APP_LANG] = v }
    suspend fun setReadingLevel(ctx: Context, v: String) = ctx.dataStore.edit { it[READING_LEVEL] = v }
    suspend fun setHighContrast(ctx: Context, v: Boolean) = ctx.dataStore.edit { it[HIGH_CONTRAST] = v }
    suspend fun setDyslexia(ctx: Context, v: Boolean) = ctx.dataStore.edit { it[DYSLEXIA] = v }
    suspend fun setActiveStudent(ctx: Context, v: String) = ctx.dataStore.edit { it[ACTIVE_STUDENT] = v }
    /** Bump per-topic mastery (0..10). [delta] can be negative. */
    suspend fun bumpMastery(ctx: Context, topic: String, delta: Int) = ctx.dataStore.edit { p ->
        val cur = parseMastery(p[TOPIC_MASTERY]).toMutableMap()
        val next = ((cur[topic] ?: 0) + delta).coerceIn(0, 10)
        cur[topic] = next
        p[TOPIC_MASTERY] = encodeMastery(cur)
    }

    /** Pre-seed a varied "lived-in" mastery matrix on first install so Teacher
     *  Mode looks alive. No-op if real progress already exists or we've seeded
     *  before — this ensures we never overwrite a real student's data. */
    suspend fun seedDemoMasteryIfEmpty(ctx: Context) = ctx.dataStore.edit { p ->
        val alreadySeeded = p[MASTERY_SEEDED] ?: false
        val existing = parseMastery(p[TOPIC_MASTERY])
        if (alreadySeeded || existing.isNotEmpty()) return@edit
        val demo = mapOf(
            "animals" to 7,
            "fruits" to 5,
            "classroom" to 8,
            "numbers" to 3,
            "facts" to 6,
            "geography" to 4,
        )
        p[TOPIC_MASTERY] = encodeMastery(demo)
        p[MASTERY_SEEDED] = true
    }

    suspend fun setLang(ctx: Context, v: String)     = ctx.dataStore.edit { it[LANG] = v }
    suspend fun setFontSize(ctx: Context, v: String) = ctx.dataStore.edit { it[FONT] = v }
    suspend fun setSound(ctx: Context, v: Boolean)   = ctx.dataStore.edit { it[SOUND] = v }
    suspend fun addStars(ctx: Context, n: Int)       = ctx.dataStore.edit {
        it[STARS] = (it[STARS] ?: 0) + n
    }
    suspend fun resetStars(ctx: Context)             = ctx.dataStore.edit {
        it[STARS] = 0; it[STREAK] = 0; it[BEST_STREAK] = 0
    }
    suspend fun bumpStreak(ctx: Context) = ctx.dataStore.edit { p ->
        val s = (p[STREAK] ?: 0) + 1
        p[STREAK] = s
        if (s > (p[BEST_STREAK] ?: 0)) p[BEST_STREAK] = s
    }
    suspend fun resetStreak(ctx: Context) = ctx.dataStore.edit { it[STREAK] = 0 }
}
