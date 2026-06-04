package com.ranjanvipul.relayguard.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ranjanvipul.relayguard.domain.model.FilterCondition
import com.ranjanvipul.relayguard.domain.model.ForwardFilter
import com.ranjanvipul.relayguard.domain.model.MessageEvent
import com.ranjanvipul.relayguard.domain.model.MessageKind
import com.ranjanvipul.relayguard.domain.model.Recipient
import com.ranjanvipul.relayguard.domain.model.ReplacementRule
import com.ranjanvipul.relayguard.domain.model.ScheduleWindow

class EntityMappers(private val gson: Gson = Gson()) {
    private val conditionListType = object : TypeToken<List<FilterCondition>>() {}.type
    private val recipientListType = object : TypeToken<List<Recipient>>() {}.type
    private val replacementListType = object : TypeToken<List<ReplacementRule>>() {}.type

    fun filterToEntity(filter: ForwardFilter): FilterEntity = FilterEntity(
        id = filter.id,
        name = filter.name,
        enabled = filter.enabled,
        messageKinds = filter.messageKinds.joinToString(",") { it.name },
        conditionsJson = gson.toJson(filter.conditions),
        recipientsJson = gson.toJson(filter.recipients),
        template = filter.template,
        replacementsJson = gson.toJson(filter.replacements),
        scheduleJson = filter.schedule?.let(gson::toJson),
        allowSensitiveMessages = filter.allowSensitiveMessages,
        preventRapidDuplicates = filter.preventRapidDuplicates
    )

    fun entityToFilter(entity: FilterEntity): ForwardFilter = ForwardFilter(
        id = entity.id,
        name = entity.name,
        enabled = entity.enabled,
        messageKinds = entity.messageKinds.split(",").filter { it.isNotBlank() }.map { MessageKind.valueOf(it) }.toSet(),
        conditions = gson.fromJson(entity.conditionsJson, conditionListType),
        recipients = gson.fromJson(entity.recipientsJson, recipientListType),
        template = entity.template,
        replacements = gson.fromJson(entity.replacementsJson, replacementListType),
        schedule = entity.scheduleJson?.let { gson.fromJson(it, ScheduleWindow::class.java) },
        allowSensitiveMessages = entity.allowSensitiveMessages,
        preventRapidDuplicates = entity.preventRapidDuplicates
    )

    fun eventToEntity(event: MessageEvent): MessageEventEntity = MessageEventEntity(
        id = event.id,
        kind = event.kind.name,
        sender = event.sender,
        body = event.body,
        receivedAtEpochMillis = event.receivedAtEpochMillis,
        simSlot = event.simSlot,
        packageName = event.packageName,
        notificationTitle = event.notificationTitle,
        attachmentCount = event.attachmentCount
    )

    fun entityToEvent(entity: MessageEventEntity): MessageEvent = MessageEvent(
        id = entity.id,
        kind = MessageKind.valueOf(entity.kind),
        sender = entity.sender,
        body = entity.body,
        receivedAtEpochMillis = entity.receivedAtEpochMillis,
        simSlot = entity.simSlot,
        packageName = entity.packageName,
        notificationTitle = entity.notificationTitle,
        attachmentCount = entity.attachmentCount
    )
}
