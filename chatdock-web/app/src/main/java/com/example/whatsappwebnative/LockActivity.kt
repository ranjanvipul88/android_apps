package com.example.whatsappwebnative

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class LockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ScreenLockStore.isEnabled(this) || ScreenLockStore.sessionUnlocked) {
            finish()
            return
        }

        setContentView(R.layout.activity_lock)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        val pinInput = findViewById<EditText>(R.id.unlockPinInput)
        findViewById<Button>(R.id.unlockButton).setOnClickListener {
            if (ScreenLockStore.verify(this, pinInput.text.toString())) {
                finish()
            } else {
                Toast.makeText(this, R.string.invalid_pin, Toast.LENGTH_SHORT).show()
                pinInput.text.clear()
            }
        }
    }
}
