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
import com.ranjanvipul.relayguard.worker.RelayWorkScheduler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestStartupPermissions()
        RelayWorkScheduler.scheduleOutgoingScans(this)
        RelayWorkScheduler.runOutgoingScanNow(this)
        setContent {
            val state by viewModel.uiState.collectAsState()
            RelayGuardTheme {
                RelayGuardApp(
                    state = state,
                    onToggleFilter = viewModel::setFilterEnabled,
                    onDeleteFilter = viewModel::deleteFilter,
                    onCreateSmsForward = viewModel::createSmsForwardRule,
                    onCreateEmailForward = viewModel::createEmailForwardRule,
                    onCreateTelegramForward = viewModel::createTelegramForwardRule,
                    onCreateSlackForward = viewModel::createSlackForwardRule,
                    onCreateWebhookForward = viewModel::createWebhookForwardRule,
                    onExportBackup = viewModel::exportBackup,
                    onRestoreBackup = viewModel::restoreBackup
                )
            }
        }
    }

    private fun requestStartupPermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.RECEIVE_MMS)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        permissionLauncher.launch(permissions)
    }
}
