package com.ranjanvipul.relayguard.domain.usecase

import com.ranjanvipul.relayguard.domain.model.ConditionField
import com.ranjanvipul.relayguard.domain.model.FilterCondition
import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MatchMode
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.MessageKind
import com.ranjanvipul.relayguard.domain.model.Recipient
import com.ranjanvipul.relayguard.domain.model.RecipientKind
import org.junit.Assert.assertEquals
import org.junit.Test

class EvaluateMessageUseCaseTest {
    private val recipient = Recipient("r1", RecipientKind.Webhook, "Ops", "https://example.test/hook")

    @Test
    fun matchingFilterRendersRelay() {
        val filter = ForwardFilter(
            id = "f1",
            name = "Bank alerts",
            enabled = true,
            messageKinds = setOf(MessageKind.IncomingSms),
            conditions = listOf(FilterCondition(ConditionField.Body, MatchMode.Contains, "balance")),
            recipients = listOf(recipient),
            template = "From {{sender}}: {{message}}"
        )
        val event = MessageEvent("m1", MessageKind.IncomingSms, "+15550001000", "Balance updated", 1L)

        val relays = EvaluateMessageUseCase().evaluate(event, listOf(filter))

        assertEquals(1, relays.size)
        assertEquals("From +15550001000: Balance updated", relays.single().body)
    }

    @Test
    fun disabledFilterDoesNotRelay() {
        val filter = ForwardFilter(
            id = "f1",
            name = "Off",
            enabled = false,
            messageKinds = setOf(MessageKind.IncomingSms),
            conditions = emptyList(),
            recipients = listOf(recipient),
            template = "{{message}}"
        )

        val relays = EvaluateMessageUseCase().evaluate(
            MessageEvent("m1", MessageKind.IncomingSms, "sender", "body", 1L),
            listOf(filter)
        )

        assertEquals(emptyList<Any>(), relays)
    }
}
