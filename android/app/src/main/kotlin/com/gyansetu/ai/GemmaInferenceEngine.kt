package com.gyansetu.ai

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.SamplerConfig
import com.gyansetu.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * On-device Gemma 4 inference via LiteRT-LM (com.google.ai.edge.litertlm).
 *
 * Why LiteRT-LM and not MediaPipe Tasks GenAI?
 *   The Gemma 4 model files Google publishes (litert-community/gemma-4-*-litert-lm
 *   on Hugging Face) use the LiteRT-LM proprietary format (magic header
 *   "LITERTLM"), not the zip-bundle ".task" format that tasks-genai loads — the
 *   ".task" file in those repos is the MediaPipe Web JS bundle. AICore /
 *   Gemini Nano is the alternative Google-recommended path on Android, but it's
 *   Pixel-first and Android 14+, while we target rural-school Android 11/12
 *   tablets, so we ship the model file and run LiteRT-LM directly.
 *
 * Capabilities:
 *   - GPU backend with CPU fallback
 *   - Multimodal: text, image (camera scan), audio (Gemma 4 native speech-in)
 *   - Streaming token output via [generateStream] for live UI
 *
 * Lifecycle: prefer one engine per process. [warmUp] is idempotent.
 * [close] releases native handles when the app is backgrounded for a while.
 */
class GemmaInferenceEngine(private val ctx: Context) {

    @Volatile var lastBackend: String = "—"   // "GPU" or "CPU" — surfaced in DebugOverlay
    @Volatile var sessionTokens: Long = 0L     // cumulative for debug HUD

    /** Kept for the debug HUD; the engine only loads .litertlm files now. */
    val variant: String get() = "litertlm"

    private var engine: Engine? = null
    private val http by lazy { OkHttpClient() }

    // LiteRT-LM allows only ONE active conversation per Engine — a second
    // createConversation while one is alive throws FAILED_PRECONDITION. Every
    // generation path acquires this mutex so concurrent callers serialise
    // (eager quiz pre-warm vs user-initiated camera scan, etc).
    private val sessionMutex = Mutex()

    val modelFile: File
        get() = File(ctx.filesDir, BuildConfig.GEMMA_LITERTLM_FILE)

    fun isModelDownloaded(): Boolean =
        modelFile.exists() && modelFile.length() > 100L * 1024 * 1024 && hasLitertlmMagic(modelFile)

    // .litertlm files start with the ASCII bytes "LITERTLM". A file that doesn't
    // start with that header will be rejected by the engine, so treat it as
    // not-downloaded and re-fetch.
    private fun hasLitertlmMagic(f: File): Boolean = runCatching {
        f.inputStream().use { s ->
            val m = ByteArray(8); s.read(m) == 8 && String(m) == "LITERTLM"
        }
    }.getOrDefault(false)

    /** Download the Gemma model with progress reporting (0f..1f). */
    fun downloadModel(): Flow<Float> = flow {
        if (isModelDownloaded()) { emit(1f); return@flow }
        val finalFile = File(ctx.filesDir, BuildConfig.GEMMA_LITERTLM_FILE)
        // A previous attempt may have left a wrongly-sized or truncated file
        // here — clear it before retrying.
        if (finalFile.exists()) finalFile.delete()
        val tmp = File(ctx.filesDir, BuildConfig.GEMMA_LITERTLM_FILE + ".part")
        if (tmp.exists()) tmp.delete()
        val req = Request.Builder().url(BuildConfig.GEMMA_LITERTLM_URL).build()
        http.newCall(req).execute().use { resp ->
            check(resp.isSuccessful) { "Model download failed: HTTP ${resp.code}" }
            val total = resp.body?.contentLength() ?: -1L
            val source = resp.body?.byteStream() ?: error("empty body")
            tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var read: Int; var done = 0L
                while (true) {
                    read = source.read(buf); if (read <= 0) break
                    out.write(buf, 0, read); done += read
                    if (total > 0) emit((done.toFloat() / total).coerceIn(0f, 1f))
                }
            }
            if (total > 0) check(tmp.length() == total) {
                "Truncated download: got ${tmp.length()} of $total bytes — please retry on a stable connection"
            }
        }
        if (!hasLitertlmMagic(tmp)) {
            tmp.delete()
            error("Downloaded file is not a valid .litertlm archive — the URL may be wrong or auth-gated")
        }
        check(tmp.renameTo(finalFile)) { "Could not finalize model file at ${finalFile.absolutePath}" }
        emit(1f)
    }.flowOn(Dispatchers.IO)

    /** Load the model into memory. Idempotent. */
    @OptIn(ExperimentalApi::class)
    suspend fun warmUp() = withContext(Dispatchers.IO) {
        if (engine != null) return@withContext
        check(isModelDownloaded()) { "Model not present at ${modelFile.absolutePath}" }

        ExperimentalFlags.enableSpeculativeDecoding = true

        val tryGpu = hasEnoughRam()
        engine = runCatching {
            Engine(buildConfig(if (tryGpu) Backend.GPU() else Backend.CPU())).also { it.initialize() }
        }
            .onSuccess { lastBackend = if (tryGpu) "GPU" else "CPU" }
            .getOrElse {
                // GPU init failed → fall back to CPU.
                Engine(buildConfig(Backend.CPU())).also {
                    it.initialize(); lastBackend = "CPU (fallback)"
                }
            }
    }

    private fun buildConfig(text: Backend): EngineConfig = EngineConfig(
        modelPath = modelFile.absolutePath,
        backend = text,
        // Vision/audio share the text backend's compute pool — keep them on CPU
        // even when the text backend is GPU; LiteRT-LM's vision pipeline is small
        // and putting it on GPU often costs more in transfers than it saves.
        visionBackend = Backend.CPU(),
        audioBackend = Backend.CPU(),
        cacheDir = ctx.cacheDir.path,
    )

    /** Streaming text generation. Each emission is the **cumulative** response so
     *  far (token deltas accumulated), to match the contract callers already use. */
    /** Sampling presets. NORMAL is the careful default for Q&A and image/audio
     *  analysis where accuracy beats creativity. CREATIVE breaks priors so the
     *  model stops emitting the same 5 questions for every quiz. VERY_CREATIVE
     *  is for short-text generators (encouragement, tip) where structure-risk
     *  is low and we just want maximum variety. */
    enum class SamplingMode { NORMAL, CREATIVE, VERY_CREATIVE }

    private fun samplerFor(mode: SamplingMode): SamplerConfig = when (mode) {
        SamplingMode.NORMAL         -> SamplerConfig(topK = 40,  topP = 0.95, temperature = 0.7)
        SamplingMode.CREATIVE       -> SamplerConfig(topK = 80,  topP = 0.97, temperature = 1.05)
        SamplingMode.VERY_CREATIVE  -> SamplerConfig(topK = 120, topP = 0.98, temperature = 1.2)
    }

    fun generateStream(
        prompt: String,
        image: Bitmap? = null,
        audio: ByteArray? = null,
        systemPrompt: String = SYSTEM_PROMPT,
        sampling: SamplingMode = SamplingMode.NORMAL,
    ): Flow<String> = flow {
        val eng = engine ?: run { warmUp(); engine!! }
        sessionMutex.withLock {
            val convCfg = ConversationConfig(
                systemInstruction = Contents.of(systemPrompt),
                samplerConfig = samplerFor(sampling),
            )
            eng.createConversation(convCfg).use { conv ->
                val parts = buildList<Content> {
                    if (image != null) add(Content.ImageBytes(bitmapToPng(image)))
                    if (audio != null) add(Content.AudioBytes(audio))
                    add(Content.Text(prompt))
                }
                val acc = StringBuilder()
                conv.sendMessageAsync(Contents.of(*parts.toTypedArray())).collect { msg ->
                    val delta = msg.toString()
                    acc.append(delta)
                    emit(acc.toString())
                    // Approx 4 chars/token for English; updates the debug HUD.
                    sessionTokens += (delta.length / 4).coerceAtLeast(0).toLong()
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /** Convenience: collect [generateStream] to completion. The last emission is
     *  the cumulative full response. */
    suspend fun generate(
        prompt: String,
        image: Bitmap? = null,
        audio: ByteArray? = null,
        systemPrompt: String = SYSTEM_PROMPT,
        sampling: SamplingMode = SamplingMode.NORMAL,
    ): String {
        var last = ""
        generateStream(prompt, image, audio, systemPrompt, sampling).collect { last = it }
        return last
    }

    suspend fun answerQuestion(question: String): String =
        generate("User: $question\nGyanSetu:")

    suspend fun analyzeImage(bitmap: Bitmap): String =
        generate(IMAGE_PROMPT, image = bitmap)

    suspend fun analyzeAudio(pcm16khzMono: ByteArray): String =
        generate(AUDIO_PROMPT, audio = pcm16khzMono)

    private fun bitmapToPng(bmp: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }

    private fun hasEnoughRam(): Boolean {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return mi.totalMem >= 6L * 1024 * 1024 * 1024
    }

    fun close() { runCatching { engine?.close() }; engine = null }

    /** A single benchmark sample. Returned per-prompt so the UI can render a table. */
    data class BenchSample(
        val prompt: String,
        val firstTokenMs: Long,
        val totalMs: Long,
        val tokensApprox: Int,
        val tokensPerSec: Float,
        val peakRamMb: Long,
    )

    /** Synthetic LiteRT-LM benchmark — runs a fixed set of prompts and reports
     *  first-token latency, throughput, and peak heap. Drives BenchmarkScreen
     *  and the numbers we cite in DEVPOST.md. */
    fun benchmark(): Flow<BenchSample> = flow {
        warmUp()
        val rt = Runtime.getRuntime()
        val prompts = listOf(
            "Why is the sky blue?",
            "Write a 2-sentence story about a cow.",
            "What is the capital of Gujarat?",
        )
        for (p in prompts) {
            // Trigger a small GC so peak measurement starts low.
            System.gc(); kotlinx.coroutines.delay(50)
            val baseMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
            var firstAt = -1L
            var lastLen = 0
            val t0 = System.nanoTime()
            generateStream(prompt = "User: $p\nGyanSetu:").collect { partial ->
                if (firstAt < 0) firstAt = System.nanoTime()
                lastLen = partial.length
            }
            val t1 = System.nanoTime()
            val totalMs = (t1 - t0) / 1_000_000
            val firstMs = if (firstAt > 0) (firstAt - t0) / 1_000_000 else totalMs
            val tokens = (lastLen / 4).coerceAtLeast(1)
            val tps = tokens.toFloat() / (totalMs.coerceAtLeast(1) / 1000f)
            val peakMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
            sessionTokens += tokens
            emit(BenchSample(p, firstMs, totalMs, tokens, tps, peakMb - baseMb))
        }
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You are GyanSetu, a friendly AI teacher for primary school children
            (Std 1–5) in rural Gujarat. Rules:
            1. Reply in simple Gujarati and English. Give the English term first,
               then Gujarati on the next line.
            2. Maximum 2–3 short sentences per language.
            3. Use vocabulary suitable for ages 6–10.
            4. Be warm and encouraging.
        """.trimIndent()

        private val IMAGE_PROMPT = """
            Look at this image carefully. Reply as JSON with keys:
            en (object name), gu (Gujarati translation), phonetic (English IPA),
            sentence_en (one short sentence about the object),
            sentence_gu (Gujarati translation of that sentence).
        """.trimIndent()

        private val AUDIO_PROMPT = """
            The user just spoke a question (in Gujarati or English). Understand
            it and answer following the rules above.
        """.trimIndent()
    }
}
