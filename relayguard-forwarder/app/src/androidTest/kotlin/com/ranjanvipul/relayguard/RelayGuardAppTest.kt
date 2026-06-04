package com.ranjanvipul.relayguard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ranjanvipul.relayguard.domain.usecase.PermissionCatalog
import com.ranjanvipul.relayguard.ui.RelayGuardApp
import com.ranjanvipul.relayguard.ui.RelayGuardUiState
import com.ranjanvipul.relayguard.ui.theme.RelayGuardTheme
import org.junit.Rule
import org.junit.Test

class RelayGuardAppTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun emptyStateIsVisible() {
        compose.setContent {
            RelayGuardTheme {
                RelayGuardApp(
                    state = RelayGuardUiState(permissions = PermissionCatalog.required),
                    onToggleFilter = { _, _ -> },
                    onCreateSampleFilter = {}
                )
            }
        }

        compose.onNodeWithText("No forwarding rules yet").assertIsDisplayed()
    }
}
