package com.akproleter.mobile.voice

import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Decodes Whisper token IDs back to a UTF-8 string.
 *
 * Whisper uses a byte-level BPE tokenizer (same as GPT-2).
 * Each token in vocab.json is a string key mapping to an integer ID.
 * We invert this map (ID -> string) and apply GPT-2's byte-to-unicode
 * reverse mapping to reconstruct the original bytes.
 */
class WhisperTokenizer(vocabFile: File) {

    companion object {
        private const val TAG = "WhisperTokenizer"

        // Whisper special token IDs (whisper-tiny multilingual)
        const val SOT_TOKEN       = 50258  // start of transcript
        const val EOT_TOKEN       = 50256  // end of transcript
        const val TRANSCRIBE_TOKEN = 50359
        const val NO_TIMESTAMPS   = 50363
        const val BLANK_TOKEN     = 220    // single space (used to detect word boundaries)

        // Language token IDs (SOT_TOKEN + 1 + language_index)
        // Languages sorted alphabetically in Whisper source
        private val LANGUAGE_OFFSETS = mapOf(
            "en" to 18,  // English
            "sr" to 80   // Serbian
        )

        fun languageToken(langCode: String): Int {
            // Strip region suffix: "en-US" -> "en", "sr-RS" -> "sr"
            val code = langCode.lowercase().substringBefore("-")
            val offset = LANGUAGE_OFFSETS[code] ?: LANGUAGE_OFFSETS["en"]!!
            return SOT_TOKEN + 1 + offset
        }

        /**
         * GPT-2 byte-to-unicode mapping (reversed for decoding).
         * Maps the unicode characters used in the vocab back to byte values.
         */
        private val unicodeToByte: Map<Char, Int> by lazy {
            val bs = mutableListOf<Int>()
            bs.addAll(('!'.code)..('~'.code))
            bs.addAll(('¡'.code)..('¬'.code))
            bs.addAll(('®'.code)..('ÿ'.code))
            val cs = bs.toMutableList()
            var n = 0
            for (b in 0 until 256) {
                if (b !in bs) { bs.add(b); cs.add(256 + n); n++ }
            }
            bs.indices.associate { i -> cs[i].toChar() to bs[i] }
        }
    }

    // id -> token string (the "raw" unicode representation from vocab.json)
    private val idToToken: Map<Int, String>

    init {
        val json  = JSONObject(vocabFile.readText())
        val inv   = mutableMapOf<Int, String>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val token = keys.next()
            inv[json.getInt(token)] = token
        }
        idToToken = inv
        Log.d(TAG, "Loaded ${idToToken.size} tokens from vocab.json")
    }

    /** Special token IDs that should be stripped from output. */
    private val specialTokens: Set<Int> = setOf(
        SOT_TOKEN, EOT_TOKEN, TRANSCRIBE_TOKEN, NO_TIMESTAMPS,
        50257,   // pad / eos alias
        50361,   // translate
        50362,   // sot_prev
        50364,   // sot_lm
    ).plus((50259..50357).toSet())  // all language tokens

    /**
     * Converts a list of token IDs to a decoded UTF-8 string.
     * Skips special tokens; converts byte-level tokens via GPT-2 mapping.
     */
    fun decode(tokenIds: List<Int>): String {
        val bytes = mutableListOf<Byte>()
        for (id in tokenIds) {
            if (id in specialTokens) continue
            val tokenStr = idToToken[id] ?: continue
            for (ch in tokenStr) {
                val byte = unicodeToByte[ch]
                if (byte != null) bytes.add(byte.toByte())
            }
        }
        return String(bytes.toByteArray(), Charsets.UTF_8).trim()
    }
}
