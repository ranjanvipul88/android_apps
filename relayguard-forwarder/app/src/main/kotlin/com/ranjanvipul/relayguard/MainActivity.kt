package com.ranjanvipul.relayguard

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ranjanvipul.relayguard.ui.RelayGuardApp
import com.ranjanvipul.relayguard.ui.theme.RelayGuardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestStartupPermissions()
        setContent {
            val state by viewModel.uiState.collectAsState()
            RelayGuardTheme {
                RelayGuardApp(
                    state = state,
                    onToggleFilter = viewModel::setFilterEnabled,
                    onCreateSmsForward = viewModel::createSmsForwardRule,
                    onCreateEmailForward = viewModel::createEmailForwardRule,
                    onCreateWhatsAppBusinessForward = viewModel::createWhatsAppBusinessForwardRule
                )
            }
        }
    }

    private fun requestStartupPermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.SEND_SMS)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        permissionLauncher.launch(permissions)
    }
}
