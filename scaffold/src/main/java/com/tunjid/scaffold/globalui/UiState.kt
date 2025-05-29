package com.tunjid.scaffold.globalui

import java.util.UUID


/**
 * Message queue for notifying the UI according to the UI events guidance in the
 * [Android architecture docs](https://developer.android.com/jetpack/guide/ui-layer/events#handle-viewmodel-events)
 *
 */
data class MessageQueue(
    val items: List<Message> = listOf(),
)

data class Message(
    val value: String,
    val id: String = UUID.randomUUID().toString(),
)

fun MessageQueue.peek(): Message? = items.firstOrNull()

fun MessageQueue.filter(predicate: (Message) -> Boolean): MessageQueue = copy(
    items = items.filter(predicate)
)

operator fun MessageQueue.plus(message: Message): MessageQueue = copy(
    items = items + message
)

operator fun MessageQueue.plus(message: String): MessageQueue = copy(
    items = items + Message(value = message)
)

operator fun MessageQueue.minus(message: Message): MessageQueue = copy(
    items = items - message
)
