package com.ranjanvipul.relayguard.domain.usecase

import com.ranjanvipul.relayguard.domain.model.ConditionField
import com.ranjanvipul.relayguard.domain.model.FilterCondition
import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MatchMode
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.RenderedRelay
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class EvaluateMessageUseCase(
    private val renderer: RenderTemplateUseCase = RenderTemplateUseCase(),
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun evaluate(event: MessageEvent, filters: List<ForwardFilter>): List<RenderedRelay> {
        val now = Instant.ofEpochMilli(event.receivedAtEpochMillis)
            .atZone(clock.zone.takeUnless { it == ZoneId.systemDefault() } ?: ZoneId.systemDefault())
        return filters
            .asSequence()
            .filter { it.enabled }
            .filter { event.kind in it.messageKinds }
            .filter { filter -> filter.schedule?.includes(now.dayOfWeek, now.toLocalTime()) ?: true }
            .filter { filter -> filter.conditions.all { it.matches(event) } }
            .flatMap { filter ->
                val body = renderer.render(filter, event)
                filter.recipients.map { RenderedRelay(filter.id, it, body) }
            }
            .toList()
    }
}

private fun FilterCondition.matches(event: MessageEvent): Boolean {
    val actual = when (field) {
        ConditionField.Sender -> event.sender
        ConditionField.Body -> event.body
        ConditionField.NotificationTitle -> event.notificationTitle.orEmpty()
        ConditionField.PackageName -> event.packageName.orEmpty()
        ConditionField.SimSlot -> event.simSlot.orEmpty()
    }
    val left = if (caseSensitive) actual else actual.lowercase()
    val right = if (caseSensitive) value else value.lowercase()
    return when (mode) {
        MatchMode.Contains -> left.contains(right)
        MatchMode.DoesNotContain -> !left.contains(right)
        MatchMode.Equals -> left == right
        MatchMode.Regex -> runCatching { Regex(value).containsMatchIn(actual) }.getOrDefault(false)
    }
}
