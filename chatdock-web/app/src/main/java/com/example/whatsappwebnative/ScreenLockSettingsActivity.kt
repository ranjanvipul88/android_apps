package com.example.whatsappwebnative

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ScreenLockSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_lock_settings)

        val pinInput = findViewById<EditText>(R.id.newPinInput)
        val confirmInput = findViewById<EditText>(R.id.confirmPinInput)

        findViewById<Button>(R.id.savePinButton).setOnClickListener {
            val pin = pinInput.text.toString()
            val confirm = confirmInput.text.toString()
            when {
                pin.length < 4 -> Toast.makeText(this, R.string.pin_too_short, Toast.LENGTH_SHORT).show()
                pin != confirm -> Toast.makeText(this, R.string.pin_mismatch, Toast.LENGTH_SHORT).show()
                else -> {
                    ScreenLockStore.setPin(this, pin)
                    Toast.makeText(this, R.string.screen_lock_enabled, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        findViewById<Button>(R.id.disablePinButton).setOnClickListener {
            ScreenLockStore.clear(this)
            Toast.makeText(this, R.string.screen_lock_disabled, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
