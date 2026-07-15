package com.example.myapplication.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Unwraps the context to find the underlying [Activity].
 * This is necessary when using [ContextWrapper] for localization or other purposes.
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
