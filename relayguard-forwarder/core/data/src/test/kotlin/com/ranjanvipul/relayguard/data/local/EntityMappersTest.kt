package com.ranjanvipul.relayguard.data.local

import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MessageKind
import com.ranjanvipul.relayguard.domain.model.Recipient
import com.ranjanvipul.relayguard.domain.model.RecipientKind
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappersTest {
    @Test
    fun filterRoundTripPreservesCoreFields() {
        val filter = ForwardFilter(
            id = "id",
            name = "Alerts",
            enabled = true,
            messageKinds = setOf(MessageKind.IncomingSms, MessageKind.AppNotification),
            conditions = emptyList(),
            recipients = listOf(Recipient("r", RecipientKind.Webhook, "Ops", "https://example.test")),
            template = "{{message}}"
        )

        val mapped = EntityMappers().entityToFilter(EntityMappers().filterToEntity(filter))

        assertEquals(filter.id, mapped.id)
        assertEquals(filter.messageKinds, mapped.messageKinds)
        assertEquals(filter.recipients.single().kind, mapped.recipients.single().kind)
    }
}
