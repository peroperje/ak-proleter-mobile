package com.akproleter.mobile.voice

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages downloading and caching of the Whisper Tiny Multilingual ONNX models.
 *
 * Models are downloaded once and stored in internal storage (filesDir/whisper/).
 * They survive app restarts but are wiped on app uninstall.
 *
 * Model source: Xenova/whisper-tiny (quantized ONNX) from HuggingFace.
 * Encoder:  ~20 MB
 * Decoder:  ~40 MB
 * Tokenizer: ~1 MB
 * Total:    ~61 MB  (one-time download over Wi-Fi recommended)
 */
@Singleton
class WhisperModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WhisperModelManager"

        // HuggingFace quantized ONNX models (Xenova format — standard optimum export)
        private const val HF_BASE = "https://huggingface.co/Xenova/whisper-tiny/resolve/main"

        private const val ENCODER_REMOTE = "$HF_BASE/onnx/encoder_model_quantized.onnx"
        private const val DECODER_REMOTE = "$HF_BASE/onnx/decoder_model_quantized.onnx"
        private const val VOCAB_REMOTE    = "https://huggingface.co/openai/whisper-tiny/resolve/main/vocab.json"

        const val ENCODER_FILENAME  = "encoder_model_quantized.onnx"
        const val DECODER_FILENAME  = "decoder_model_quantized.onnx"
        const val VOCAB_FILENAME    = "vocab.json"
    }

    private val modelDir: File get() = File(context.filesDir, "whisper").also { it.mkdirs() }

    val encoderFile: File get() = File(modelDir, ENCODER_FILENAME)
    val decoderFile: File get() = File(modelDir, DECODER_FILENAME)
    val vocabFile:   File get() = File(modelDir, VOCAB_FILENAME)

    val isReady: Boolean
        get() = encoderFile.exists() && decoderFile.exists() && vocabFile.exists()

    sealed class DownloadState {
        object Idle         : DownloadState()
        data class Progress(val percent: Int, val fileName: String) : DownloadState()
        object Done         : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState = _downloadState.asStateFlow()

    /**
     * Downloads all required model files if they are not already present.
     * Emits progress via [downloadState]. Safe to call multiple times.
     */
    suspend fun ensureModels() = withContext(Dispatchers.IO) {
        if (isReady) {
            Log.d(TAG, "All model files already present — skipping download")
            _downloadState.value = DownloadState.Done
            return@withContext
        }

        val filesToDownload = buildList {
            if (!encoderFile.exists()) add(ENCODER_REMOTE to encoderFile)
            if (!decoderFile.exists()) add(DECODER_REMOTE to decoderFile)
            if (!vocabFile.exists())   add(VOCAB_REMOTE   to vocabFile)
        }

        for ((url, dest) in filesToDownload) {
            val ok = downloadFile(url, dest) { pct ->
                _downloadState.value = DownloadState.Progress(pct, dest.name)
            }
            if (!ok) {
                _downloadState.value = DownloadState.Error("Failed to download ${dest.name}")
                dest.delete()
                return@withContext
            }
        }

        _downloadState.value = DownloadState.Done
        Log.d(TAG, "All model files downloaded successfully")
    }

    private fun downloadFile(
        urlString: String,
        dest: File,
        onProgress: (Int) -> Unit
    ): Boolean {
        return try {
            val url  = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout    = 60_000
            conn.connect()

            val length = conn.contentLengthLong

            conn.inputStream.use { input ->
                dest.outputStream().use { output ->
                    val buf   = ByteArray(8 * 1024)
                    var total = 0L
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        total += read
                        if (length > 0) onProgress(((total * 100) / length).toInt())
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $urlString", e)
            false
        }
    }
}
