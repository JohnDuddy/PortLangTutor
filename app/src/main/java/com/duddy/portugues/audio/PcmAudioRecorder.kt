package com.duddy.portugues.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/**
 * Captures 16 kHz, 16-bit, mono PCM audio and produces a WAV blob in memory.
 *
 * Azure Pronunciation Assessment requires exactly this format — any other
 * sample rate or channel layout needs server-side transcoding.
 *
 *  Usage:
 *    val recorder = PcmAudioRecorder(context)
 *    recorder.start()
 *    // ... user speaks ...
 *    val (wav, duration) = recorder.stop()
 *    // POST wav to backend /v1/pronunciation
 */
class PcmAudioRecorder(private val context: Context) {

    companion object {
        private const val TAG          = "PcmAudioRecorder"
        const val SAMPLE_RATE_HZ       = 16_000
        const val CHANNEL_CONFIG       = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_ENCODING       = AudioFormat.ENCODING_PCM_16BIT
        private const val MAX_SECONDS  = 30
    }

    private var recorder: AudioRecord? = null
    @Volatile private var recording = false
    private var buffer = ByteArrayOutputStream()
    private var startMillis = 0L

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * @throws SecurityException if RECORD_AUDIO not granted.
     * @throws IllegalStateException if AudioRecord fails to initialise.
     */
    fun start() {
        require(hasPermission()) { "RECORD_AUDIO permission required" }
        if (recording) return

        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_ENCODING)
        check(minBuf > 0) { "AudioRecord.getMinBufferSize failed: $minBuf" }

        val bufferSize = maxOf(minBuf, SAMPLE_RATE_HZ * 2)
        recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE_HZ,
            CHANNEL_CONFIG,
            AUDIO_ENCODING,
            bufferSize,
        ).also {
            check(it.state == AudioRecord.STATE_INITIALIZED) {
                "AudioRecord failed to initialise: state=${it.state}"
            }
        }

        buffer = ByteArrayOutputStream()
        recording = true
        startMillis = System.currentTimeMillis()
        recorder!!.startRecording()

        thread(name = "PcmRecorder", isDaemon = true) {
            val tmp = ByteArray(bufferSize)
            while (recording) {
                val read = recorder?.read(tmp, 0, tmp.size) ?: 0
                if (read > 0) buffer.write(tmp, 0, read)
                if (System.currentTimeMillis() - startMillis > MAX_SECONDS * 1000) {
                    Log.w(TAG, "max recording duration reached, auto-stopping")
                    recording = false
                }
            }
        }
    }

    data class Recording(val wavBytes: ByteArray, val durationSeconds: Double) {
        override fun equals(other: Any?): Boolean = other is Recording &&
                wavBytes.contentEquals(other.wavBytes) && durationSeconds == other.durationSeconds
        override fun hashCode(): Int = wavBytes.contentHashCode() * 31 + durationSeconds.hashCode()
    }

    /** Stop recording and return WAV blob + accurate duration. */
    fun stop(): Recording {
        if (!recording && recorder == null) return Recording(ByteArray(0), 0.0)

        recording = false
        try {
            recorder?.stop()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "stop() while not started", e)
        }
        recorder?.release()
        recorder = null

        Thread.sleep(50) // let recording thread flush

        val pcm = buffer.toByteArray()
        val durationSec = pcm.size / (SAMPLE_RATE_HZ * 2.0)
        return Recording(wrapAsWav(pcm), durationSec)
    }

    fun cancel() {
        recording = false
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        buffer = ByteArrayOutputStream()
    }

    /** Prepend a 44-byte WAV header so Azure can decode it directly. */
    private fun wrapAsWav(pcm: ByteArray): ByteArray {
        val totalDataLen = pcm.size + 36
        val byteRate     = SAMPLE_RATE_HZ * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(totalDataLen); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(SAMPLE_RATE_HZ); putInt(byteRate); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(pcm.size)
        }.array()
        return header + pcm
    }
}
