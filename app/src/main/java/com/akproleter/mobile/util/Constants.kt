package com.akproleter.mobile.util

import com.akproleter.mobile.BuildConfig

object Constants {
    val BASE_URL = if (BuildConfig.DEBUG) {
        "http://localhost:3000/"
    } else {
        "https://ak-proleter.vercel.app/"
    }
    const val VOICE_PROCESS_ENDPOINT = "api/voice/process"
}
