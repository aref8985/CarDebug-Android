package com.example.myapplication.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleManager {
    fun updateLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }
}
