package com.gyansetu.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gyansetu.GyanSetuApp
import com.gyansetu.ai.AudioCapture
import com.gyansetu.ai.ContentGenerator
import com.gyansetu.ai.GemmaInferenceEngine
import com.gyansetu.ai.OfflineRAG
import com.gyansetu.ai.ToolCall
import com.gyansetu.ai.ToolRegistry
import com.gyansetu.ai.ToolStep
import com.gyansetu.data.AskSuggestion
import com.gyansetu.data.ContentState
import com.gyansetu.data.DailyTip
import com.gyansetu.data.Encouragement
import com.gyansetu.data.MatchPair
import com.gyansetu.data.QuizQuestion
import com.gyansetu.data.TeacherRec
import com.gyansetu.data.Settings
import com.gyansetu.data.SyllabusDatabase
import com.gyansetu.data.SyllabusEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

/** What the engine is currently doing. Drives the LoadingScreen UI. */
sealed interface EngineState {
    data object Idle : EngineState
    data class Downloading(val progress: Float) : EngineState
    data object WarmingUp : EngineState
    data object Ready : EngineState
    data class Error(val message: String) : EngineState
}

/** A streamed Gemma answer in progress, with a trace of any tool calls the
 *  agent made along the way. The UI shows [toolSteps] as inline chips. */
data class AskResponse(
    val question: String,
    val partial: String = "",
    val done: Boolean = false,
    val ragHit: SyllabusEntity? = null,
    val toolSteps: List<ToolStep> = emptyList(),
)

/** Image-recognition state for the Camera Scan flow. */
sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Result(val raw: String, val parsed: ScanResult) : ScanState
    data class Error(val message: String) : ScanState
}

/** Parsed bilingual fields from Gemma 4's image-analysis JSON. */
data class ScanResult(
    val en: String,
    val gu: String,
    val phonetic: String,
    val sentenceEn: String,
    val sentenceGu: String,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val gyanApp = app as GyanSetuApp
    private val gemma: GemmaInferenceEngine = gyanApp.gemma
    private val db: SyllabusDatabase = gyanApp.database
    private val rag: OfflineRAG = OfflineRAG(db.dao())
    private val tools: ToolRegistry = ToolRegistry(db.dao()).apply {
        // ToolRegistry.find_weak_topics asks the ViewModel for the latest snapshot.
        masteryProvider = { settings.value.topicMastery }
    }
    private val contentGen: ContentGenerator = ContentGenerator(gemma)

    private val _engine = MutableStateFlow<EngineState>(EngineState.Idle)
    val engine: StateFlow<EngineState> = _engine.asStateFlow()

    private val _ask = MutableStateFlow<AskResponse?>(null)
    val ask: StateFlow<AskResponse?> = _ask.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _scan = MutableStateFlow<ScanState>(ScanState.Idle)
    val scan: StateFlow<ScanState> = _scan.asStateFlow()

    /** Persisted preferences (lang, fontSize, sound, stars, streak, bestStreak). */
    val settings: StateFlow<Settings.Snapshot> =
        Settings.stream(app).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = Settings.Snapshot(
                lang = "both", fontSize = "regular", sound = true,
                stars = 0, streak = 0, bestStreak = 0,
            ),
        )

    fun setLang(v: String) { viewModelScope.launch { Settings.setLang(gyanApp, v) } }
    fun setFontSize(v: String) { viewModelScope.launch { Settings.setFontSize(gyanApp, v) } }
    fun setSound(v: Boolean) { viewModelScope.launch { Settings.setSound(gyanApp, v) } }
    fun setModelVariant(v: String) { viewModelScope.launch { Settings.setModelVariant(gyanApp, v) } }
    fun setDebug(v: Boolean) { viewModelScope.launch { Settings.setDebug(gyanApp, v) } }
    fun setAppLang(v: String) { viewModelScope.launch { Settings.setAppLang(gyanApp, v) } }
    fun setReadingLevel(v: String) { viewModelScope.launch { Settings.setReadingLevel(gyanApp, v) } }
    fun setHighContrast(v: Boolean) { viewModelScope.launch { Settings.setHighContrast(gyanApp, v) } }
    fun setDyslexia(v: Boolean) { viewModelScope.launch { Settings.setDyslexia(gyanApp, v) } }
    fun setActiveStudent(v: String) { viewModelScope.launch { Settings.setActiveStudent(gyanApp, v) } }
    fun bumpMastery(topic: String, delta: Int) {
        viewModelScope.launch { Settings.bumpMastery(gyanApp, topic, delta) }
    }
    fun addStars(n: Int) { viewModelScope.launch { Settings.addStars(gyanApp, n) } }
    fun resetStars() { viewModelScope.launch { Settings.resetStars(gyanApp) } }
    fun bumpStreak() { viewModelScope.launch { Settings.bumpStreak(gyanApp) } }
    fun resetStreak() { viewModelScope.launch { Settings.resetStreak(gyanApp) } }

    private var generationJob: Job? = null
    private var capture: AudioCapture? = null

    /** Live debug HUD info: backend chosen, variant loaded, session token count. */
    data class EngineDebug(
        val backend: String,
        val variant: String,
        val sessionTokens: Long,
        val modelFile: String,
    )
    fun engineDebug(): EngineDebug = EngineDebug(
        backend = gemma.lastBackend,
        variant = gemma.variant,
        sessionTokens = gemma.sessionTokens,
        modelFile = gemma.modelFile.name,
    )

    init {
        // Start preparing the engine immediately. UI subscribes to [engine] and
        // navigates from Loading → Home when it transitions to Ready.
        prepareEngine()
        // As soon as Gemma is loaded, eagerly pre-generate one batch of quiz +
        // match content so the kid sees fresh, model-authored cards instead of
        // a 25-second loading screen on their first Quiz/Match tap.
        viewModelScope.launch {
            engine.first { it is EngineState.Ready }
            ensureQuizContent()
            ensureMatchContent()
            ensureAskSuggestions()
            ensureDailyTip()
        }
    }

    /* ─── Dynamic Quiz + Match content (model-generated) ────────────────── */

    private val _quizContent = MutableStateFlow<ContentState<List<QuizQuestion>>>(ContentState.Loading)
    val quizContent: StateFlow<ContentState<List<QuizQuestion>>> = _quizContent.asStateFlow()

    private val _matchContent = MutableStateFlow<ContentState<List<MatchPair>>>(ContentState.Loading)
    val matchContent: StateFlow<ContentState<List<MatchPair>>> = _matchContent.asStateFlow()

    private val _askSuggestions = MutableStateFlow<ContentState<List<AskSuggestion>>>(ContentState.Loading)
    val askSuggestions: StateFlow<ContentState<List<AskSuggestion>>> = _askSuggestions.asStateFlow()

    private val _dailyTip = MutableStateFlow<ContentState<DailyTip>>(ContentState.Loading)
    val dailyTip: StateFlow<ContentState<DailyTip>> = _dailyTip.asStateFlow()

    /** ResultsScreen sets this on entry; null means fallback static message. */
    private val _encouragement = MutableStateFlow<Encouragement?>(null)
    val encouragement: StateFlow<Encouragement?> = _encouragement.asStateFlow()

    /** TeacherMode sets this on entry; reset between students. */
    private val _teacherRec = MutableStateFlow<ContentState<TeacherRec>>(ContentState.Loading)
    val teacherRec: StateFlow<ContentState<TeacherRec>> = _teacherRec.asStateFlow()

    private var quizGenJob: Job? = null
    private var matchGenJob: Job? = null
    private var askSuggestionsJob: Job? = null
    private var dailyTipJob: Job? = null
    private var encouragementJob: Job? = null
    private var teacherRecJob: Job? = null

    /** Stale-while-revalidate: if we already have a Ready batch, leave it visible
     *  while we fetch the next one in the background. Only show Loading when the
     *  current state has nothing usable. Idempotent — safe to spam. */
    fun ensureQuizContent(force: Boolean = false) {
        if (!force && quizGenJob?.isActive == true) return
        quizGenJob?.cancel()
        quizGenJob = viewModelScope.launch {
            if (_quizContent.value !is ContentState.Ready || force) {
                _quizContent.value = ContentState.Loading
            }
            val out = runCatching { contentGen.generateQuiz(QUIZ_BATCH) }
                .onFailure { android.util.Log.w("AppVM", "quiz gen failed: ${it.message}") }
                .getOrNull()
            _quizContent.value = if (out != null && out.size >= QUIZ_MIN_VALID) {
                android.util.Log.d("AppVM", "quiz batch READY (${out.size} questions): ${out.map { it.en }}")
                ContentState.Ready(out)
            } else {
                android.util.Log.w("AppVM", "quiz batch FELL BACK to static pool (model returned ${out?.size ?: 0} valid items)")
                ContentState.Failed(
                    reason = "model unavailable or returned too few items",
                    fallback = ContentGenerator.staticQuizPool.shuffled().take(QUIZ_BATCH),
                )
            }
        }
    }

    fun ensureMatchContent(force: Boolean = false) {
        if (!force && matchGenJob?.isActive == true) return
        matchGenJob?.cancel()
        matchGenJob = viewModelScope.launch {
            if (_matchContent.value !is ContentState.Ready || force) {
                _matchContent.value = ContentState.Loading
            }
            val out = runCatching { contentGen.generateMatchPairs(MATCH_BATCH) }
                .onFailure { android.util.Log.w("AppVM", "match gen failed: ${it.message}") }
                .getOrNull()
            _matchContent.value = if (out != null && out.size >= MATCH_MIN_VALID) {
                android.util.Log.d("AppVM", "match batch READY (${out.size} pairs): ${out.map { it.en }}")
                ContentState.Ready(out)
            } else {
                android.util.Log.w("AppVM", "match batch FELL BACK to DB (model returned ${out?.size ?: 0} valid items)")
                // Fallback: pull from the seeded SyllabusDatabase. More variety
                // than a hand-coded array, and it's already on disk.
                val dbItems = listOf("animals", "fruits", "classroom")
                    .flatMap { db.dao().byTopic(it) }
                    .shuffled()
                    .take(MATCH_BATCH)
                    .map { MatchPair(topic = it.topic, gu = it.gu, en = it.en, icon = it.icon ?: "✨") }
                ContentState.Failed(reason = "model unavailable", fallback = dbItems)
            }
        }
    }

    fun ensureAskSuggestions(force: Boolean = false) {
        if (!force && askSuggestionsJob?.isActive == true) return
        askSuggestionsJob?.cancel()
        askSuggestionsJob = viewModelScope.launch {
            if (_askSuggestions.value !is ContentState.Ready || force) {
                _askSuggestions.value = ContentState.Loading
            }
            val out = runCatching { contentGen.generateAskSuggestions(ASK_SUGGESTIONS_BATCH) }
                .onFailure { android.util.Log.w("AppVM", "ask suggestions gen failed: ${it.message}") }
                .getOrNull()
            _askSuggestions.value = if (out != null && out.size >= ASK_SUGGESTIONS_MIN_VALID) {
                ContentState.Ready(out.take(ASK_SUGGESTIONS_BATCH))
            } else {
                ContentState.Failed(reason = "model unavailable", fallback = ContentGenerator.staticAskSuggestions)
            }
        }
    }

    /** Pre-empt any in-flight eager pre-cache jobs so a user-initiated action
     *  (camera scan, ask, item-detail, results, teacher rec) doesn't queue
     *  behind 20+ seconds of background quiz/match generation on the engine's
     *  single-conversation mutex. Background jobs auto-restart later when the
     *  user re-enters the screen that needs them. */
    private fun cancelBackgroundJobs() {
        quizGenJob?.cancel()
        matchGenJob?.cancel()
        askSuggestionsJob?.cancel()
        dailyTipJob?.cancel()
    }

    fun ensureDailyTip(force: Boolean = false) {
        if (!force && dailyTipJob?.isActive == true) return
        dailyTipJob?.cancel()
        dailyTipJob = viewModelScope.launch {
            if (_dailyTip.value !is ContentState.Ready || force) _dailyTip.value = ContentState.Loading
            val out = runCatching { contentGen.generateDailyTip() }
                .onFailure { android.util.Log.w("AppVM", "daily tip gen failed: ${it.message}") }
                .getOrNull()
            _dailyTip.value = if (out != null) ContentState.Ready(out)
            else ContentState.Failed("model unavailable", ContentGenerator.staticDailyTips.random())
        }
    }

    /** Called by ResultsScreen on entry. Falls back to a band-keyed static
     *  message immediately so the screen always has something visible while
     *  the model generates. */
    fun generateEncouragement(stars: Int, total: Int) {
        cancelBackgroundJobs()
        encouragementJob?.cancel()
        _encouragement.value = null
        encouragementJob = viewModelScope.launch {
            val out = runCatching { contentGen.generateEncouragement(stars, total) }
                .onFailure { android.util.Log.w("AppVM", "encouragement gen failed: ${it.message}") }
                .getOrNull()
            if (out != null) _encouragement.value = out
        }
    }

    /** Called by TeacherModeScreen when the active student or weakest topic changes. */
    fun generateTeacherRecommendation(studentName: String, weakestTopic: String, mastery: Int) {
        cancelBackgroundJobs()
        teacherRecJob?.cancel()
        teacherRecJob = viewModelScope.launch {
            _teacherRec.value = ContentState.Loading
            val out = runCatching {
                contentGen.generateTeacherRecommendation(studentName, weakestTopic, mastery)
            }
                .onFailure { android.util.Log.w("AppVM", "teacher rec gen failed: ${it.message}") }
                .getOrNull()
            _teacherRec.value = if (out != null) ContentState.Ready(out)
            else ContentState.Failed(
                reason = "model unavailable",
                fallback = TeacherRec(
                    text = "Tomorrow's focus for $studentName: drill $weakestTopic " +
                        "(mastery $mastery/10). Try the Quiz screen — it adaptively " +
                        "weights questions toward this kid's weakest topics.",
                ),
            )
        }
    }

    /** Stream a fresh kid-friendly explanation for one syllabus item. The
     *  caller (TopicsScreen ItemDetail) collects this directly into the UI. */
    fun streamItemExplanation(en: String, gu: String): kotlinx.coroutines.flow.Flow<String> {
        cancelBackgroundJobs()
        return contentGen.streamItemExplanation(en = en, gu = gu, readingLevel = settings.value.readingLevel)
    }

    /** Called by the screens after a game finishes / on exit so the next entry
     *  has a fresh batch waiting (no extra wait for the kid). */
    fun regenerateQuizContent() = ensureQuizContent(force = true)
    fun regenerateMatchContent() = ensureMatchContent(force = true)
    fun regenerateAskSuggestions() = ensureAskSuggestions(force = true)
    fun regenerateDailyTip() = ensureDailyTip(force = true)

    /** Run the LiteRT benchmark and stream samples into a state flow for the UI. */
    private val _bench = MutableStateFlow<List<GemmaInferenceEngine.BenchSample>>(emptyList())
    val bench: StateFlow<List<GemmaInferenceEngine.BenchSample>> = _bench.asStateFlow()
    private val _benchRunning = MutableStateFlow(false)
    val benchRunning: StateFlow<Boolean> = _benchRunning.asStateFlow()

    fun runBenchmark() {
        if (_benchRunning.value) return
        _benchRunning.value = true
        _bench.value = emptyList()
        viewModelScope.launch {
            try {
                gemma.benchmark().collect { sample ->
                    _bench.value = _bench.value + sample
                }
            } finally {
                _benchRunning.value = false
            }
        }
    }

    fun prepareEngine() {
        if (_engine.value is EngineState.Ready) return
        viewModelScope.launch {
            try {
                if (!gemma.isModelDownloaded()) {
                    _engine.value = EngineState.Downloading(0f)
                    gemma.downloadModel().collect { p ->
                        _engine.value = EngineState.Downloading(p)
                    }
                }
                _engine.value = EngineState.WarmingUp
                gemma.warmUp()
                _engine.value = EngineState.Ready
            } catch (t: Throwable) {
                _engine.value = EngineState.Error(t.message ?: t::class.simpleName ?: "unknown")
            }
        }
    }

    /** Ask a text question. Runs a multi-turn agent loop:
     *    1. Stream a response from Gemma 4 with the tool list in scope.
     *    2. If the model emits a `<<TOOL>>{…}<<END>>` marker, run the tool.
     *    3. Inject `<<RESULT>>…<<END>>` and continue generation.
     *    4. Repeat up to [MAX_TOOL_ITER] times, then accept whatever's emitted.
     *
     *  Also performs a quick RAG keyword lookup as a hint the model can latch
     *  onto if the model decides not to call a tool itself. */
    fun askText(question: String) {
        if (_engine.value !is EngineState.Ready) return
        cancelBackgroundJobs()
        generationJob?.cancel()
        _ask.value = AskResponse(question = question)
        generationJob = viewModelScope.launch {
            val hit = runCatching { rag.lookup(question) }.getOrNull()
            _ask.value = _ask.value?.copy(ragHit = hit)

            val steps = mutableListOf<ToolStep>()
            val convo = StringBuilder()
            convo.append("User: ").append(question).append('\n')
            if (hit != null) {
                convo.append("(Hint from local DB: ").append(hit.en).append(" = ")
                    .append(hit.gu).append(")\n")
            }
            convo.append("GyanSetu:")

            try {
                val readingSuffix = readingLevelSuffix(settings.value.readingLevel)
                val langSuffix = appLangSuffix(settings.value.appLang)
                runAgentLoop(convo, steps, ToolRegistry.SYSTEM_PROMPT + readingSuffix + langSuffix)
                _ask.value = _ask.value?.copy(done = true)
            } catch (t: Throwable) {
                _ask.value = _ask.value?.copy(
                    partial = "Sorry, something went wrong: ${t.message}",
                    done = true,
                )
            }
        }
    }

    /** The agentic loop. Modifies [convo] in place by appending tool calls and
     *  results so each iteration sees the full transcript so far. */
    private suspend fun runAgentLoop(
        convo: StringBuilder,
        steps: MutableList<ToolStep>,
        systemPrompt: String,
    ) {
        var iter = 0
        while (iter < MAX_TOOL_ITER) {
            iter++
            var lastPartial = ""
            gemma.generateStream(
                prompt = convo.toString(),
                systemPrompt = systemPrompt,
            ).collect { partial ->
                lastPartial = partial
                // Show the model's natural-language portion live; hide tool markers.
                val visible = ToolRegistry.stripToolMarkers(partial)
                _ask.value = _ask.value?.copy(partial = visible, toolSteps = steps.toList())
            }

            val call = ToolRegistry.parseToolCall(lastPartial)
            if (call == null) {
                // No tool needed — final answer reached.
                return
            }

            val result = runCatching { tools.run(call) }.getOrElse { "tool error: ${it.message}" }
            val step = ToolStep(call.name, call.argsRepr(), result)
            steps += step
            _ask.value = _ask.value?.copy(toolSteps = steps.toList())

            // Build the next turn: keep what the model said, then inject the result
            // and prompt the model to continue.
            convo.append(' ').append(lastPartial)
            convo.append("\n<<RESULT>>").append(result).append("<<END>>\nGyanSetu:")
        }
    }

    /** Suffix appended to the system prompt based on the chosen reading level —
     *  drives the LLM to use age-appropriate vocabulary on the fly. */
    private fun readingLevelSuffix(level: String): String = when (level) {
        "1-2" -> "\n\nReading-level: Std 1-2 (ages 6-7). Use ONE-syllable English words " +
            "wherever possible. Two-sentence answers max. Tone: very simple."
        "5"   -> "\n\nReading-level: Std 5 (ages 10-11). You can use slightly longer " +
            "vocabulary and three-sentence answers, but stay clear and concrete."
        else  -> "\n\nReading-level: Std 3-4 (ages 8-9). Standard vocabulary, two to " +
            "three short sentences."
    }

    /** Suffix that names the user's preferred mother-tongue. The model already
     *  knows 140 languages — this just biases output toward the right one. */
    private fun appLangSuffix(lang: String): String = when (lang) {
        "hi" -> "\n\nUser's mother tongue: Hindi (हिन्दी). Use Hindi as the primary " +
            "non-English language in your replies."
        "mr" -> "\n\nUser's mother tongue: Marathi (मराठी). Use Marathi as the primary " +
            "non-English language in your replies."
        "ta" -> "\n\nUser's mother tongue: Tamil (தமிழ்). Use Tamil as the primary " +
            "non-English language in your replies."
        "en" -> "\n\nUser prefers English-only replies. Skip the Gujarati translation."
        else -> "\n\nUser's mother tongue: Gujarati (ગુજરાતી). Use Gujarati as the primary " +
            "non-English language in your replies."
    }

    private companion object {
        const val MAX_TOOL_ITER = 3
        const val QUIZ_BATCH = 5
        const val QUIZ_MIN_VALID = 3   // accept the model's output if at least 3 questions parsed
        const val MATCH_BATCH = 5
        const val MATCH_MIN_VALID = 5  // match needs all pairs unique to be playable
        const val ASK_SUGGESTIONS_BATCH = 3
        const val ASK_SUGGESTIONS_MIN_VALID = 2
    }

    /** Capture mic audio, hand to Gemma 4. Caller must already have RECORD_AUDIO. */
    fun listenAndAsk() {
        if (_engine.value !is EngineState.Ready || _isRecording.value) return
        cancelBackgroundJobs()
        _ask.value = AskResponse(question = "🎙 …")
        _isRecording.value = true
        generationJob = viewModelScope.launch {
            try {
                val cap = AudioCapture().also { capture = it }
                val pcm = cap.startAndCollect()
                _isRecording.value = false
                _ask.value = _ask.value?.copy(question = "🎙 (you spoke)")
                gemma.generateStream(
                    prompt = "User just spoke. Transcribe in your head and answer.\nGyanSetu:",
                    audio = pcm,
                ).collect { partial -> _ask.value = _ask.value?.copy(partial = partial) }
                _ask.value = _ask.value?.copy(done = true)
            } catch (t: Throwable) {
                _isRecording.value = false
                _ask.value = _ask.value?.copy(
                    partial = "Couldn't process audio: ${t.message}",
                    done = true,
                )
            } finally {
                capture = null
            }
        }
    }

    fun stopRecording() { capture?.stop() }
    fun clearAsk() { _ask.value = null; generationJob?.cancel() }

    /** Hand a captured camera frame to Gemma 4 for bilingual recognition. */
    fun analyzeBitmap(bitmap: Bitmap) {
        if (_engine.value !is EngineState.Ready) return
        cancelBackgroundJobs()
        generationJob?.cancel()
        _scan.value = ScanState.Scanning
        generationJob = viewModelScope.launch {
            try {
                val raw = gemma.analyzeImage(bitmap)
                val parsed = parseScanJson(raw)
                _scan.value = if (parsed != null) ScanState.Result(raw, parsed)
                              else ScanState.Error("Couldn't parse model output:\n$raw")
            } catch (t: Throwable) {
                _scan.value = ScanState.Error(t.message ?: "scan failed")
            }
        }
    }

    fun resetScan() { _scan.value = ScanState.Idle }

    private fun parseScanJson(raw: String): ScanResult? {
        // Gemma sometimes wraps JSON in ```json … ``` or surrounds it with prose;
        // pull the first { … } block out and parse leniently.
        val start = raw.indexOf('{'); val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching {
            val o = JSONObject(raw.substring(start, end + 1))
            ScanResult(
                en = o.optString("en", "?"),
                gu = o.optString("gu", "?"),
                phonetic = o.optString("phonetic", ""),
                sentenceEn = o.optString("sentence_en", ""),
                sentenceGu = o.optString("sentence_gu", ""),
            )
        }.getOrNull()
    }

    suspend fun topicItems(topic: String): List<SyllabusEntity> = db.dao().byTopic(topic)

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
        capture?.stop()
    }
}
