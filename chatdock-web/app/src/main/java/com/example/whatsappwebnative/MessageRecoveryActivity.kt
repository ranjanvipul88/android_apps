package com.example.whatsappwebnative

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MessageRecoveryActivity : AppCompatActivity() {

    private lateinit var recoveredMessagesView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_recovery)

        recoveredMessagesView = findViewById(R.id.recoveredMessagesView)
        findViewById<Button>(R.id.clearRecoveredMessagesButton).setOnClickListener {
            MessageRecoveryStore.clear(this)
            refresh()
        }
        refresh()
    }

    private fun refresh() {
        recoveredMessagesView.text = MessageRecoveryStore.formattedItems(this)
    }
}
