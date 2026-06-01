package com.example.whatsappwebnative

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindAccountButtons()
        bindFeatureButtons()
    }

    override fun onResume() {
        super.onResume()
        enforceScreenLock()
        bindAccountButtons()
    }

    private fun bindAccountButtons() {
        findViewById<TextView>(R.id.multiAccountNote).text =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getString(R.string.account_isolation_ready)
            } else {
                getString(R.string.account_isolation_limited)
            }

        AccountSlot.all.forEach { slot ->
            findViewById<Button>(slot.buttonId).apply {
                text = getString(R.string.open_named_account_button, AccountNameStore.getName(this@MainActivity, slot))
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, slot.activityClass))
                }
                setOnLongClickListener {
                    showRenameDialog(slot)
                    true
                }
            }
        }
    }

    private fun bindFeatureButtons() {
        findViewById<Button>(R.id.statusSaverButton).setOnClickListener {
            startActivity(Intent(this, StatusSaverActivity::class.java))
        }
        findViewById<Button>(R.id.messageRecoveryButton).setOnClickListener {
            startActivity(Intent(this, MessageRecoveryActivity::class.java))
        }
        findViewById<Button>(R.id.screenLockButton).setOnClickListener {
            startActivity(Intent(this, ScreenLockSettingsActivity::class.java))
        }
    }

    private fun showRenameDialog(slot: AccountSlot) {
        val input = EditText(this).apply {
            setSingleLine(true)
            setText(AccountNameStore.getName(this@MainActivity, slot))
            selectAll()
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.rename_account_title, slot.number))
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                AccountNameStore.setName(this, slot, input.text.toString())
                bindAccountButtons()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun enforceScreenLock() {
        if (ScreenLockStore.isEnabled(this) && !ScreenLockStore.sessionUnlocked) {
            startActivity(Intent(this, LockActivity::class.java))
        }
    }
}
