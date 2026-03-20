package com.akproleter.mobile.util

import com.akproleter.mobile.BuildConfig

object Constants {
    // 💡 Tip: 10.0.2.2 is the special address for the host machine's localhost on Android emulators
   // private const val LOCALHOST_EMULATOR = "http://10.0.2.2:3000/"
    private const val LOCALHOST_EMULATOR = "http://192.168.0.20:3000"
    private const val PRODUCTION_URL = "https://ak-proleter.vercel.app/"

    val BASE_URL = if (BuildConfig.DEBUG) {
        LOCALHOST_EMULATOR
    } else {
        PRODUCTION_URL
    }

    // Endpoints
    const val LOGIN_ENDPOINT = "api/auth/login" // Verify this route exists in Next.js
    const val VOICE_PROCESS_ENDPOINT = "api/voice/process"
    const val SUBMIT_RESULT_ENDPOINT = "api/results/submit"
}

