package com.akproleter.mobile.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Records raw PCM audio from the microphone using [AudioRecord].
 *
 * Whisper requires 16 kHz, mono, float32 PCM.
 * This class records as 16-bit PCM (hardware native) and converts to float32.
 *
 * Usage:
 *   val capture = AudioCapture()
 *   capture.start()
 *   // ... user speaks ...
 *   val samples = capture.stop()   // FloatArray at 16 kHz
 */
class AudioCapture {

    companion object {
        private const val TAG = "AudioCapture"
        const val SAMPLE_RATE = 16_000
        private const val CHANNEL  = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        // Max 30 s buffer (Whisper's window); extras are truncated
        private const val MAX_SAMPLES = SAMPLE_RATE * 30
    }

    private var audioRecord: AudioRecord? = null
    private val buffer = ShortArray(MAX_SAMPLES)
    private var samplesRecorded = 0
    @Volatile private var recording = false

    fun start() {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
            .coerceAtLeast(4096)

        @Suppress("MissingPermission")
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL,
            ENCODING,
            minBuf
        ).also { ar ->
            if (ar.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                ar.release()
                return
            }
        }

        samplesRecorded = 0
        recording = true
        audioRecord?.startRecording()
        Log.d(TAG, "Recording started")
    }

    /**
     * Reads audio chunks in a coroutine loop.
     * Call from a launched coroutine; the loop ends when [stop] is called.
     */
    suspend fun recordLoop() = withContext(Dispatchers.IO) {
        val chunk = ShortArray(1024)
        while (isActive && recording) {
            val read = audioRecord?.read(chunk, 0, chunk.size) ?: break
            if (read > 0 && samplesRecorded + read <= MAX_SAMPLES) {
                chunk.copyInto(buffer, samplesRecorded, 0, read)
                samplesRecorded += read
            }
        }
    }

    /**
     * Stops recording and returns the captured audio as a normalized float32 array.
     * Values are in [-1.0, 1.0].
     */
    fun stop(): FloatArray {
        recording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val count = samplesRecorded
        Log.d(TAG, "Recording stopped — $count samples (${count / SAMPLE_RATE}s)")

        val float32 = FloatArray(count) { i -> buffer[i] / 32768.0f }
        return float32
    }
}
