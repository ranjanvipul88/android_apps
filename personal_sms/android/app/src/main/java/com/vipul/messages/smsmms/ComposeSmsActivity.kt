package com.vipul.messages.smsmms

import android.app.Activity
import android.os.Bundle
import android.util.Log

/**
 * Empty entry activity required by Android OS to list the app as a Default SMS Handler target.
 * In production, it handles external share/send Intents seamlessly.
 */
class ComposeSmsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ComposeSmsActivity", "System compose intent triggered.")
        finish() // Instantly hand off to main activity in production
    }
}
