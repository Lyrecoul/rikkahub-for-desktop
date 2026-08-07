package me.rerere.rikkahub.desktop

internal fun shouldAutoScrollChat(
    enabled: Boolean,
    hasMessages: Boolean,
    isResponseGenerating: Boolean,
): Boolean = enabled && hasMessages && isResponseGenerating

internal fun isChatMessageGenerating(
    isResponseGenerating: Boolean,
    messageIndex: Int,
    lastMessageIndex: Int,
): Boolean = isResponseGenerating && messageIndex == lastMessageIndex

/**
 * Presentation-only grouping for an assistant tool loop. The persisted message
 * sequence remains unchanged because it is also used as the provider context.
 */
internal sealed interface DesktopChatDisplayItem {
    val key: String
    val messageIndex: Int

    data class Message(
        override val messageIndex: Int,
        val message: ChatMessage,
    ) : DesktopChatDisplayItem {
        override val key = message.id
    }

    data class AssistantTurn(
        override val messageIndex: Int,
        /** The first persisted message represented by this collapsed turn. */
        val startMessageIndex: Int,
        val message: ChatMessage,
        val steps: List<DesktopExecutionStep>,
        val messageIds: Set<String>,
        val timelineAfterContent: Boolean,
    ) : DesktopChatDisplayItem {
        override val key = message.id
    }
}

internal sealed interface DesktopExecutionStep {
    data class Reasoning(val message: ChatMessage) : DesktopExecutionStep

    data class ToolCall(
        val call: DesktopToolCall,
        val result: ChatMessage? = null,
    ) : DesktopExecutionStep

    /** Retains malformed or legacy tool results that cannot be matched to a call. */
    data class ToolResult(val message: ChatMessage) : DesktopExecutionStep
}

internal data class DesktopMessageNavigationItem(
    val displayIndex: Int,
    val messageId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
) {
    val summary: String = content.trim().replace(Regex("\\s+"), " ").take(160)
}

/** Builds the searchable navigation list from the same presentation items as the chat. */
internal fun buildDesktopMessageNavigationItems(
    displayItems: List<DesktopChatDisplayItem>
): List<DesktopMessageNavigationItem> = displayItems.mapIndexed { displayIndex, item ->
    val message = when (item) {
        is DesktopChatDisplayItem.Message -> item.message
        is DesktopChatDisplayItem.AssistantTurn -> item.message
    }
    DesktopMessageNavigationItem(displayIndex, message.id, message.role, message.content, message.createdAt)
}

/** Only visible user and assistant text is searchable; tool and reasoning details remain in the message view. */
internal fun List<DesktopMessageNavigationItem>.filterForNavigation(query: String): List<DesktopMessageNavigationItem> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return this
    return filter { item ->
        item.role in setOf("user", "assistant") && item.content.contains(normalizedQuery, ignoreCase = true)
    }
}

internal fun buildDesktopChatDisplayItems(messages: List<ChatMessage>): List<DesktopChatDisplayItem> {
    val items = mutableListOf<DesktopChatDisplayItem>()
    var index = 0

    while (index < messages.size) {
        val message = messages[index]
        if (message.role != "assistant") {
            if (message.role != "tool") {
                items += DesktopChatDisplayItem.Message(index, message)
            } else {
                // A tool result without a preceding assistant call is legacy data.
                items += DesktopChatDisplayItem.Message(index, message)
            }
            index++
            continue
        }

        val steps = mutableListOf<DesktopExecutionStep>()
        var cursor = index
        var anchorIndex = index
        var anchor = message
        var hasExecution = false
        var finished = false
        var timelineAfterContent = false
        val messageIds = linkedSetOf<String>()

        while (cursor < messages.size && !finished) {
            val current = messages[cursor]
            when (current.role) {
                "assistant" -> {
                    if (timelineAfterContent) {
                        finished = true
                        continue
                    }
                    messageIds += current.id
                    val hasReasoning = current.reasoning.isNotBlank()
                    val hasToolCalls = current.toolCalls.isNotEmpty()
                    if (hasReasoning) {
                        steps += DesktopExecutionStep.Reasoning(current)
                        hasExecution = true
                    }
                    if (hasToolCalls) {
                        current.toolCalls.forEach { steps += DesktopExecutionStep.ToolCall(it) }
                        hasExecution = true
                    }
                    timelineAfterContent = hasToolCalls && current.content.isNotBlank()

                    anchorIndex = cursor
                    anchor = current
                    cursor++

                    // A text part flushes the current execution group, matching the Android renderer.
                    if (current.content.isNotBlank() && !hasToolCalls) finished = true
                }

                "tool" -> {
                    messageIds += current.id
                    val matchingIndex = steps.indexOfLast {
                        it is DesktopExecutionStep.ToolCall && it.call.id == current.toolCallId
                    }
                    if (matchingIndex >= 0) {
                        val call = steps[matchingIndex] as DesktopExecutionStep.ToolCall
                        steps[matchingIndex] = call.copy(result = current)
                    } else {
                        steps += DesktopExecutionStep.ToolResult(current)
                    }
                    hasExecution = true
                    cursor++
                }

                else -> finished = true
            }
        }

        if (hasExecution) {
            items += DesktopChatDisplayItem.AssistantTurn(
                anchorIndex,
                index,
                anchor,
                steps,
                messageIds,
                timelineAfterContent,
            )
            index = cursor
        } else {
            items += DesktopChatDisplayItem.Message(index, message)
            index++
        }
    }

    return items
}
