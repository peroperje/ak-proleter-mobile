package com.akproleter.mobile.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import javax.inject.Singleton
import javax.inject.Inject

@Singleton
class LanguageManager @Inject constructor() {

    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    fun getLocale(context: Context): String {
        return context.resources.configuration.locales[0].language
    }

    companion object {
        const val SR = "sr"
        const val EN = "en"
    }
}
