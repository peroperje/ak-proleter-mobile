package com.akproleter.mobile.voice

import kotlin.math.*

/**
 * Computes the log-mel spectrogram expected by Whisper models.
 *
 * Parameters match OpenAI's Whisper exactly:
 *   - Sample rate : 16 000 Hz
 *   - N_FFT       : 400  (25 ms window)
 *   - Hop length  : 160  (10 ms)
 *   - N_MELS      : 80
 *   - FFT size    : 512  (next power-of-2 ≥ 400, for efficient FFT)
 *
 * Output shape   : [N_MELS × N_FRAMES] flattened in row-major order,
 *                  padded / truncated to exactly 3 000 time frames (30 s).
 */
object MelSpectrogram {

    private const val SAMPLE_RATE  = 16_000
    private const val N_FFT        = 400
    private const val HOP_LENGTH   = 160
    private const val N_MELS       = 80
    private const val FFT_SIZE     = 512   // next pow2 ≥ N_FFT
    private const val N_FRAMES     = 3_000 // 30 s of audio at 10 ms hop
    private const val N_FFT_BINS   = FFT_SIZE / 2 + 1

    // Pre-computed Hann window (length N_FFT)
    private val hannWindow = FloatArray(N_FFT) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / N_FFT))).toFloat()
    }

    // Pre-computed mel filterbank [N_MELS × N_FFT_BINS]
    private val melFilters: Array<FloatArray> by lazy { buildMelFilterbank() }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * @param pcm  Float32 PCM samples at 16 kHz in range [-1, 1].
     * @return     Flattened [N_MELS × N_FRAMES] = [80 × 3000] = 240 000 floats.
     */
    fun compute(pcm: FloatArray): FloatArray {
        // 1. Pad / truncate to exactly 30 s
        val maxSamples = SAMPLE_RATE * 30
        val audio = if (pcm.size >= maxSamples) {
            pcm.copyOf(maxSamples)
        } else {
            // Pad with zeros (silence) at the end
            FloatArray(maxSamples).also { pcm.copyInto(it) }
        }

        // 2. Compute STFT power spectrum for each frame
        val powerFrames = mutableListOf<FloatArray>()
        var offset = 0
        while (offset + N_FFT <= audio.size && powerFrames.size < N_FRAMES) {
            val frame = FloatArray(FFT_SIZE) // zero-padded to FFT_SIZE
            for (i in 0 until N_FFT) frame[i] = audio[offset + i] * hannWindow[i]
            powerFrames.add(powerSpectrum(frame))
            offset += HOP_LENGTH
        }

        // Pad time dimension if audio was shorter than 30 s
        while (powerFrames.size < N_FRAMES) {
            powerFrames.add(FloatArray(N_FFT_BINS))
        }

        // 3. Apply mel filterbank → log-mel with Whisper's normalization
        val logMel = Array(N_MELS) { m ->
            FloatArray(N_FRAMES) { t ->
                val power = powerFrames[t]
                var energy = 1e-10f
                for (k in 0 until N_FFT_BINS) energy += melFilters[m][k] * power[k]
                log10(energy)
            }
        }

        // 4. Whisper normalization: clamp to (max − 8), then scale to [−1, 1] via (x+4)/4
        var maxVal = Float.NEGATIVE_INFINITY
        for (m in 0 until N_MELS) for (t in 0 until N_FRAMES) {
            if (logMel[m][t] > maxVal) maxVal = logMel[m][t]
        }

        // 5. Flatten to contiguous [N_MELS * N_FRAMES] array
        val out = FloatArray(N_MELS * N_FRAMES)
        for (m in 0 until N_MELS) {
            for (t in 0 until N_FRAMES) {
                val v = maxOf(logMel[m][t], maxVal - 8.0f)
                out[m * N_FRAMES + t] = (v + 4.0f) / 4.0f
            }
        }
        return out
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Computes the one-sided power spectrum of a windowed frame via FFT. */
    private fun powerSpectrum(frame: FloatArray): FloatArray {
        val real = frame.copyOf()
        val imag = FloatArray(frame.size)
        fft(real, imag)
        return FloatArray(N_FFT_BINS) { k -> real[k] * real[k] + imag[k] * imag[k] }
    }

    /**
     * In-place Cooley–Tukey radix-2 DIT FFT.
     * Array size must be a power of 2.
     */
    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n ushr 1
            while (j and bit != 0) { j = j xor bit; bit = bit ushr 1 }
            j = j xor bit
            if (i < j) {
                var t = real[i]; real[i] = real[j]; real[j] = t
                t = imag[i]; imag[i] = imag[j]; imag[j] = t
            }
        }
        // Butterfly passes
        var len = 2
        while (len <= n) {
            val half  = len ushr 1
            val angle = -PI / half
            val wRe   = cos(angle).toFloat()
            val wIm   = sin(angle).toFloat()
            var pos   = 0
            while (pos < n) {
                var curRe = 1.0f; var curIm = 0.0f
                for (k in 0 until half) {
                    val uRe = real[pos + k];          val uIm = imag[pos + k]
                    val vRe = real[pos + k + half]
                    val vIm = imag[pos + k + half]
                    val tvRe = vRe * curRe - vIm * curIm
                    val tvIm = vRe * curIm + vIm * curRe
                    real[pos + k]        = uRe + tvRe;  imag[pos + k]        = uIm + tvIm
                    real[pos + k + half] = uRe - tvRe;  imag[pos + k + half] = uIm - tvIm
                    val nextRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe;  curRe = nextRe
                }
                pos += len
            }
            len = len shl 1
        }
    }

    /**
     * Triangular mel filterbank matching librosa / Whisper parameters.
     * Returns [N_MELS][N_FFT_BINS].
     */
    private fun buildMelFilterbank(): Array<FloatArray> {
        fun hzToMel(hz: Double) = 2595.0 * log10(1.0 + hz / 700.0)
        fun melToHz(mel: Double) = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)

        val fMin   = 0.0
        val fMax   = SAMPLE_RATE / 2.0
        val melMin = hzToMel(fMin)
        val melMax = hzToMel(fMax)

        // N_MELS + 2 equally-spaced mel-scale points
        val melPts = DoubleArray(N_MELS + 2) { i ->
            melMin + i * (melMax - melMin) / (N_MELS + 1)
        }
        // Convert to FFT bin indices
        val bins = IntArray(N_MELS + 2) { i ->
            ((melToHz(melPts[i]) * FFT_SIZE) / SAMPLE_RATE).toInt().coerceIn(0, N_FFT_BINS - 1)
        }

        return Array(N_MELS) { m ->
            FloatArray(N_FFT_BINS) { k ->
                when {
                    k < bins[m]     -> 0.0f
                    k < bins[m + 1] -> (k - bins[m]).toFloat() / (bins[m + 1] - bins[m]).coerceAtLeast(1)
                    k < bins[m + 2] -> (bins[m + 2] - k).toFloat() / (bins[m + 2] - bins[m + 1]).coerceAtLeast(1)
                    else            -> 0.0f
                }
            }
        }
    }
}
