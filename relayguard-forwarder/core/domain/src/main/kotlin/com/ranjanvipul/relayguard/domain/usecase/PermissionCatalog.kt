package com.ranjanvipul.relayguard.domain.usecase

import com.ranjanvipul.relayguard.domain.model.PermissionExplanation

object PermissionCatalog {
    val required = listOf(
        PermissionExplanation(
            permission = "RECEIVE_SMS",
            userFacingReason = "RelayGuard needs to detect new SMS messages you choose to forward.",
            developerNote = "Incoming SMS PDUs are processed locally, matched against enabled rules, logged in Room, and sent only to configured recipients."
        ),
        PermissionExplanation(
            permission = "SEND_SMS",
            userFacingReason = "RelayGuard can forward messages to SMS recipients and send approved remote replies.",
            developerNote = "SMS sending is initiated only by enabled rules or explicit remote-reply actions; results are retained in local delivery history."
        ),
        PermissionExplanation(
            permission = "INTERNET",
            userFacingReason = "RelayGuard can deliver selected messages to HTTPS web, email, and chat integrations.",
            developerNote = "Network requests are made with OkHttp/Retrofit over HTTPS; secrets are stored via Android Keystore-backed encrypted preferences."
        ),
        PermissionExplanation(
            permission = "POST_NOTIFICATIONS",
            userFacingReason = "RelayGuard shows delivery status and foreground processing notices.",
            developerNote = "Notifications contain operational status only and avoid displaying message content unless the user enables previews."
        )
    )

    val optional = listOf(
        PermissionExplanation(
            permission = "READ_SMS",
            userFacingReason = "RelayGuard can import recent messages when you ask it to build history.",
            developerNote = "Reads are user-initiated and stored locally; the permission can be skipped for live-only forwarding."
        ),
        PermissionExplanation(
            permission = "READ_CONTACTS",
            userFacingReason = "RelayGuard can show contact names instead of raw phone numbers.",
            developerNote = "Contact names are resolved on device and are not uploaded except where the user includes them in a forwarding template."
        ),
        PermissionExplanation(
            permission = "BIND_NOTIFICATION_LISTENER_SERVICE",
            userFacingReason = "RelayGuard can forward notifications from apps you select.",
            developerNote = "Notification forwarding is disabled by default and processes only packages selected by the user."
        ),
        PermissionExplanation(
            permission = "RECEIVE_BOOT_COMPLETED",
            userFacingReason = "RelayGuard can restart enabled background rules after device reboot.",
            developerNote = "The receiver schedules pending work only; it does not read or transmit messages during boot."
        )
    )
}
