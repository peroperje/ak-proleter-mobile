package com.akproleter.mobile.voice

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import java.nio.FloatBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device Whisper Tiny Multilingual inference using ONNX Runtime.
 *
 * Architecture:
 *   1. Mel spectrogram  [1, 80, 3000] → Encoder → encoder_hidden_states [1, 1500, 384]
 *   2. Greedy decode    Start tokens + encoder output → Decoder → next token (repeat until EOT)
 *   3. Token decode     token IDs → UTF-8 text via [WhisperTokenizer]
 *
 * Thread safety: sessions are created once and reused. All inference runs on IO dispatcher.
 */
@Singleton
class WhisperRecognizer @Inject constructor(
    private val modelManager: WhisperModelManager
) {
    companion object {
        private const val TAG = "WhisperRecognizer"
        private const val MAX_TOKENS = 128 // enough for any athletic result phrase
    }

    private val env: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private var encoderSession: OrtSession? = null
    private var decoderSession: OrtSession? = null
    private var tokenizer: WhisperTokenizer? = null

    /** Load ONNX sessions and tokenizer (once). Safe to call repeatedly. */
    private fun ensureSessionsLoaded() {
        if (encoderSession != null) return
        check(modelManager.isReady) { "Whisper models not downloaded yet" }

        val opts = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
            setInterOpNumThreads(1)
        }
        encoderSession = env.createSession(modelManager.encoderFile.absolutePath, opts)
        decoderSession = env.createSession(modelManager.decoderFile.absolutePath, opts)
        tokenizer      = WhisperTokenizer(modelManager.vocabFile)

        Log.d(TAG, "ONNX sessions loaded")
        Log.d(TAG, "Encoder inputs : ${encoderSession!!.inputNames}")
        Log.d(TAG, "Decoder inputs : ${decoderSession!!.inputNames}")
    }

    /**
     * Transcribes raw float32 PCM samples (16 kHz mono) to text.
     *
     * @param pcm      Audio from [AudioCapture.stop]
     * @param language BCP-47 language code, e.g. "en-US" or "sr-RS"
     * @return         Transcribed text, or throws on error.
     */
    suspend fun transcribe(pcm: FloatArray, language: String): String =
        withContext(Dispatchers.Default) {
            ensureSessionsLoaded()
            val encoder = encoderSession!!
            val decoder = decoderSession!!
            val tok     = tokenizer!!

            // ── Step 1: Mel spectrogram ──────────────────────────────────────
            val mel = MelSpectrogram.compute(pcm) // [80 * 3000] = 240 000 floats

            val inputFeatures = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(mel),
                longArrayOf(1L, 80L, 3000L)
            )

            // ── Step 2: Encoder ──────────────────────────────────────────────
            val encoderOut = encoder.run(
                mapOf("input_features" to inputFeatures)
            )
            val encoderHidden = encoderOut[0].value as Array<Array<FloatArray>>
            // shape: [1, 1500, 384]
            inputFeatures.close()
            encoderOut.close()

            // ── Step 3: Greedy decode ────────────────────────────────────────
            // Initial prompt: SOT + language token + transcribe + no-timestamps
            val langToken = WhisperTokenizer.languageToken(language)
            val tokenIds  = mutableListOf(
                WhisperTokenizer.SOT_TOKEN,
                langToken,
                WhisperTokenizer.TRANSCRIBE_TOKEN,
                WhisperTokenizer.NO_TIMESTAMPS
            )

            repeat(MAX_TOKENS) {
                val inputIds = OnnxTensor.createTensor(
                    env,
                    Array(1) { LongArray(tokenIds.size) { i -> tokenIds[i].toLong() } }
                )
                val encoderTensor = OnnxTensor.createTensor(
                    env,
                    encoderHidden
                )

                val decoderOut = decoder.run(
                    mapOf(
                        "input_ids"             to inputIds,
                        "encoder_hidden_states" to encoderTensor
                    )
                )

                // logits shape: [1, seq_len, vocab_size]
                @Suppress("UNCHECKED_CAST")
                val logits = decoderOut[0].value as Array<Array<FloatArray>>
                val lastLogits = logits[0].last()   // [vocab_size]
                val nextToken  = argmax(lastLogits)

                inputIds.close(); encoderTensor.close(); decoderOut.close()

                if (nextToken == WhisperTokenizer.EOT_TOKEN) return@repeat
                tokenIds.add(nextToken)
            }

            Log.d(TAG, "Decoded token IDs: $tokenIds")
            val text = tok.decode(tokenIds)
            Log.d(TAG, "Transcription: '$text'")
            text
        }

    /** Releases ONNX sessions. Call when the recognizer is no longer needed. */
    fun close() {
        encoderSession?.close(); encoderSession = null
        decoderSession?.close(); decoderSession = null
        Log.d(TAG, "ONNX sessions released")
    }

    private fun argmax(values: FloatArray): Int {
        var best = 0
        var bestVal = values[0]
        for (i in 1 until values.size) {
            if (values[i] > bestVal) { bestVal = values[i]; best = i }
        }
        return best
    }
}
