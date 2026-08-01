package me.rerere.rikkahub.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopChatTimelineTest {
    @Test
    fun mergesMultipleToolRoundsBeforeTheFinalAnswer() {
        val firstCall = DesktopToolCall("call_1", "search", "{\"query\":\"one\"}")
        val secondCall = DesktopToolCall("call_2", "clock")
        val messages = listOf(
            ChatMessage("user", "help"),
            ChatMessage("assistant", "", reasoning = "Need to search", toolCalls = listOf(firstCall)),
            ChatMessage("tool", "first result", toolCallId = firstCall.id),
            ChatMessage("assistant", "", reasoning = "Need the current time", toolCalls = listOf(secondCall)),
            ChatMessage("tool", "12:00", toolCallId = secondCall.id),
            ChatMessage("assistant", "Here is the answer")
        )

        val items = buildDesktopChatDisplayItems(messages)

        assertEquals(2, items.size)
        val turn = assertIs<DesktopChatDisplayItem.AssistantTurn>(items[1])
        assertEquals(1, turn.startMessageIndex)
        assertEquals(5, turn.messageIndex)
        assertEquals("Here is the answer", turn.message.content)
        assertEquals(messages.drop(1).map { it.id }.toSet(), turn.messageIds)
        assertEquals(4, turn.steps.size)
        assertEquals("Need to search", assertIs<DesktopExecutionStep.Reasoning>(turn.steps[0]).message.reasoning)
        assertEquals("first result", assertIs<DesktopExecutionStep.ToolCall>(turn.steps[1]).result?.content)
        assertEquals("Need the current time", assertIs<DesktopExecutionStep.Reasoning>(turn.steps[2]).message.reasoning)
        assertEquals("12:00", assertIs<DesktopExecutionStep.ToolCall>(turn.steps[3]).result?.content)
    }

    @Test
    fun keepsAnIncompleteToolLoopAsOneTurn() {
        val call = DesktopToolCall("call_1", "search")
        val messages = listOf(
            ChatMessage("user", "help"),
            ChatMessage("assistant", "", reasoning = "Searching", toolCalls = listOf(call))
        )

        val turn = assertIs<DesktopChatDisplayItem.AssistantTurn>(buildDesktopChatDisplayItems(messages)[1])

        assertEquals(1, turn.messageIndex)
        assertEquals(1, turn.startMessageIndex)
        assertEquals(2, turn.steps.size)
        assertNull(assertIs<DesktopExecutionStep.ToolCall>(turn.steps[1]).result)
    }

    @Test
    fun cancellingUnresolvedToolCallsDropsAnEmptyToolCallingMessage() {
        val call = DesktopToolCall("call_1", DesktopAskUserToolName, "{}")
        val conversation = DesktopConversation(
            messages = listOf(
                ChatMessage("user", "help"),
                ChatMessage("assistant", "", toolCalls = listOf(call))
            )
        )

        val cleaned = conversation.cancelUnresolvedToolCalls()

        assertEquals(listOf("help"), cleaned.messages.map { it.content })
        assertEquals(listOf("user"), cleaned.messages.map { it.role })
    }

    @Test
    fun cancellingUnresolvedToolCallsKeepsReasoningButStripsTheCalls() {
        val call = DesktopToolCall("call_1", DesktopAskUserToolName, "{}")
        val conversation = DesktopConversation(
            messages = listOf(
                ChatMessage("user", "help"),
                ChatMessage("assistant", "", reasoning = "Need to clarify", toolCalls = listOf(call))
            )
        )

        val cleaned = conversation.cancelUnresolvedToolCalls()

        assertEquals(2, cleaned.messages.size)
        val last = cleaned.messages.last()
        assertEquals("assistant", last.role)
        assertEquals("Need to clarify", last.reasoning)
        assertTrue(last.toolCalls.isEmpty())
    }

    @Test
    fun cancellingUnresolvedToolCallsKeepsTextButStripsTheCalls() {
        val call = DesktopToolCall("call_1", DesktopAskUserToolName, "{}")
        val conversation = DesktopConversation(
            messages = listOf(
                ChatMessage("user", "help"),
                ChatMessage("assistant", "Let me ask you something", toolCalls = listOf(call))
            )
        )

        val cleaned = conversation.cancelUnresolvedToolCalls()

        val last = cleaned.messages.last()
        assertEquals("Let me ask you something", last.content)
        assertTrue(last.toolCalls.isEmpty())
    }

    @Test
    fun cancellingUnresolvedToolCallsLeavesResolvedRoundsUntouched() {
        val call = DesktopToolCall("call_1", "search")
        val conversation = DesktopConversation(
            messages = listOf(
                ChatMessage("user", "help"),
                ChatMessage("assistant", "", toolCalls = listOf(call)),
                ChatMessage("tool", "result", toolCallId = call.id)
            )
        )

        val cleaned = conversation.cancelUnresolvedToolCalls()

        assertEquals(conversation.messages, cleaned.messages)
    }

    @Test
    fun textAndUserMessagesEndTheExecutionBoundary() {
        val call = DesktopToolCall("call_1", "search")
        val messages = listOf(
            ChatMessage("assistant", "First answer"),
            ChatMessage("user", "Follow up"),
            ChatMessage("assistant", "", toolCalls = listOf(call)),
            ChatMessage("tool", "result", toolCallId = call.id),
            ChatMessage("assistant", "Second answer")
        )

        val items = buildDesktopChatDisplayItems(messages)

        assertEquals(3, items.size)
        assertIs<DesktopChatDisplayItem.Message>(items[0])
        assertIs<DesktopChatDisplayItem.Message>(items[1])
        assertIs<DesktopChatDisplayItem.AssistantTurn>(items[2])
    }

    @Test
    fun preservesUnmatchedToolResults() {
        val messages = listOf(
            ChatMessage("assistant", "", reasoning = "Checking"),
            ChatMessage("tool", "legacy result", toolCallId = "missing")
        )

        val turn = assertIs<DesktopChatDisplayItem.AssistantTurn>(buildDesktopChatDisplayItems(messages).single())

        assertEquals("legacy result", assertIs<DesktopExecutionStep.ToolResult>(turn.steps.last()).message.content)
    }

    @Test
    fun putsToolTimelineAfterTextWhenTheToolCallingMessageContainsText() {
        val shell = DesktopToolCall("call_shell", "shell")
        val messages = listOf(
            ChatMessage("assistant", "Running the command", toolCalls = listOf(shell)),
            ChatMessage("tool", "command output", toolCallId = shell.id),
            ChatMessage("assistant", "Done")
        )

        val items = buildDesktopChatDisplayItems(messages)
        val turn = assertIs<DesktopChatDisplayItem.AssistantTurn>(items[0])

        assertEquals("Running the command", turn.message.content)
        assertEquals(true, turn.timelineAfterContent)
        assertEquals("command output", assertIs<DesktopExecutionStep.ToolCall>(turn.steps[0]).result?.content)
        assertEquals("Done", assertIs<DesktopChatDisplayItem.Message>(items[1]).message.content)
    }

    @Test
    fun buildsSearchableNavigationItemsFromVisibleMessages() {
        val call = DesktopToolCall("call", "search")
        val messages = listOf(
            ChatMessage("user", "Find Kotlin docs", createdAt = 10),
            ChatMessage("assistant", "", toolCalls = listOf(call), createdAt = 20),
            ChatMessage("tool", "tool output", toolCallId = call.id, createdAt = 30),
            ChatMessage("assistant", "Kotlin documentation", createdAt = 40)
        )

        val navigationItems = buildDesktopMessageNavigationItems(buildDesktopChatDisplayItems(messages))

        assertEquals(2, navigationItems.size)
        assertEquals("Find Kotlin docs", navigationItems[0].summary)
        assertEquals("Kotlin documentation", navigationItems[1].summary)
        assertEquals(1, navigationItems[1].displayIndex)
        assertEquals(listOf("Kotlin documentation"), navigationItems.filterForNavigation("DOCUMENT").map { it.content })
    }

    @Test
    fun navigationSearchExcludesToolContentAndNormalizesSummaries() {
        val items = listOf(
            DesktopMessageNavigationItem(0, "user", "user", " first\n  message ", 0),
            DesktopMessageNavigationItem(1, "tool", "tool", "private result", 0)
        )

        assertEquals("first message", items.first().summary)
        assertTrue(items.filterForNavigation("private").isEmpty())
    }
}
