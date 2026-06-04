package com.ranjanvipul.relayguard.domain.usecase

import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.MessageKind
import com.ranjanvipul.relayguard.domain.model.Recipient
import com.ranjanvipul.relayguard.domain.model.RecipientKind
import com.ranjanvipul.relayguard.domain.model.ReplacementRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RenderTemplateUseCaseTest {
    @Test
    fun appliesTokensAndReplacementRules() {
        val filter = ForwardFilter(
            id = "f1",
            name = "Template",
            enabled = true,
            messageKinds = setOf(MessageKind.IncomingSms),
            conditions = emptyList(),
            recipients = listOf(Recipient("r1", RecipientKind.Email, "Inbox", "ops@example.test")),
            template = "{{sender}} sent {{message}}",
            replacements = listOf(ReplacementRule("secret", "[hidden]"))
        )

        val rendered = RenderTemplateUseCase().render(
            filter,
            MessageEvent("m1", MessageKind.IncomingSms, "A", "secret code", 1L)
        )

        assertTrue(rendered.contains("[hidden]"))
        assertFalse(rendered.contains("secret"))
    }
}
