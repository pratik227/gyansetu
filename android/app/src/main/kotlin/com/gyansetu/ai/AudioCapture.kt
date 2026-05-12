package com.gyansetu.ai

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Captures microphone audio as 16-kHz mono PCM 16-bit, the format Gemma 4's
 * audio modality expects. Returns the recorded bytes when [stop] is called or
 * the maxDuration elapses.
 *
 * Caller must hold RECORD_AUDIO permission before [start].
 */
class AudioCapture(
    private val sampleRate: Int = 16_000,
    private val maxDurationMs: Long = 8_000,
) {
    private var recorder: AudioRecord? = null
    @Volatile private var running = false
    private val buffer = ByteArrayOutputStream()

    @SuppressLint("MissingPermission")
    suspend fun startAndCollect(): ByteArray = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        check(minBuf > 0) { "AudioRecord.getMinBufferSize failed: $minBuf" }

        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 2,
        )
        recorder = rec
        buffer.reset()
        rec.startRecording()
        running = true

        val chunk = ByteArray(minBuf)
        val deadline = System.currentTimeMillis() + maxDurationMs
        while (running && System.currentTimeMillis() < deadline) {
            val read = rec.read(chunk, 0, chunk.size)
            if (read > 0) buffer.write(chunk, 0, read)
        }
        runCatching { rec.stop() }
        runCatching { rec.release() }
        recorder = null
        running = false
        buffer.toByteArray()
    }

    fun stop() { running = false }
}
