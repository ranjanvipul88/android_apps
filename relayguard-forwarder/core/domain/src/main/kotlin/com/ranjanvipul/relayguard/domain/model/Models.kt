package com.ranjanvipul.relayguard.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

enum class MessageKind {
    IncomingSms,
    IncomingMmsNotice,
    OutgoingSms,
    AppNotification
}

enum class RecipientKind {
    SmsNumber,
    Email,
    Webhook,
    Telegram,
    Slack
}

enum class ConditionField {
    Sender,
    Body,
    NotificationTitle,
    PackageName,
    SimSlot
}

enum class MatchMode {
    Contains,
    DoesNotContain,
    Equals,
    Regex
}

data class MessageEvent(
    val id: String,
    val kind: MessageKind,
    val sender: String,
    val body: String,
    val receivedAtEpochMillis: Long,
    val simSlot: String? = null,
    val packageName: String? = null,
    val notificationTitle: String? = null,
    val attachmentCount: Int = 0
)

data class FilterCondition(
    val field: ConditionField,
    val mode: MatchMode,
    val value: String,
    val caseSensitive: Boolean = false
)

data class ReplacementRule(
    val find: String,
    val replaceWith: String,
    val useRegex: Boolean = false
)

data class ScheduleWindow(
    val days: Set<DayOfWeek>,
    val start: LocalTime,
    val end: LocalTime
) {
    fun includes(day: DayOfWeek, time: LocalTime): Boolean {
        if (day !in days) return false
        return if (start <= end) {
            !time.isBefore(start) && !time.isAfter(end)
        } else {
            !time.isBefore(start) || !time.isAfter(end)
        }
    }
}

data class Recipient(
    val id: String,
    val kind: RecipientKind,
    val label: String,
    val address: String,
    val secretAlias: String? = null,
    val isFallback: Boolean = false
)

data class ForwardFilter(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val messageKinds: Set<MessageKind>,
    val conditions: List<FilterCondition>,
    val recipients: List<Recipient>,
    val template: String,
    val replacements: List<ReplacementRule> = emptyList(),
    val schedule: ScheduleWindow? = null,
    val allowSensitiveMessages: Boolean = false,
    val preventRapidDuplicates: Boolean = true
)

data class RenderedRelay(
    val filterId: String,
    val recipient: Recipient,
    val body: String
)

data class PermissionExplanation(
    val permission: String,
    val userFacingReason: String,
    val developerNote: String
)
