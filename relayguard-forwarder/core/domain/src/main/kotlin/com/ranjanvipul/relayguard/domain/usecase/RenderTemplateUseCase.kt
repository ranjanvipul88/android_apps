package com.ranjanvipul.relayguard.domain.usecase

import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RenderTemplateUseCase {
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun render(filter: ForwardFilter, event: MessageEvent): String {
        val values = mapOf(
            "sender" to event.sender,
            "message" to event.body,
            "time" to formatter.format(Instant.ofEpochMilli(event.receivedAtEpochMillis).atZone(ZoneId.systemDefault())),
            "kind" to event.kind.name,
            "sim" to event.simSlot.orEmpty(),
            "title" to event.notificationTitle.orEmpty(),
            "app" to event.packageName.orEmpty()
        )
        val templated = values.entries.fold(filter.template) { text, (key, value) ->
            text.replace("{{$key}}", value)
        }
        return filter.replacements.fold(templated) { text, rule ->
            if (rule.useRegex) text.replace(Regex(rule.find), rule.replaceWith) else text.replace(rule.find, rule.replaceWith)
        }
    }
}
