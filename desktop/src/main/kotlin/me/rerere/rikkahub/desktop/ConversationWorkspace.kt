package me.rerere.rikkahub.desktop

/**
 * 对话编辑与执行编排：生成期间拒绝编辑（门控单一实现），ask_user 答案路由
 * （执行中注入 pending 通道；执行已结束则插入 tool 消息并触发续跑）。
 */
internal class ConversationWorkspace(
    private val state: ConversationExecutionState,
    private val registry: DesktopGenerationRegistry,
    private val answerIfPending: (callId: String, answer: String) -> Boolean,
    private val resume: (conversationId: String) -> Unit,
) {
    /** 生成中拒绝编辑；返回 false = 被门控拒绝（状态未变）。 */
    fun editConversation(id: String, transform: (DesktopConversation) -> DesktopConversation): Boolean {
        if (registry.isRunning(id)) return false
        state.update { data ->
            data.copy(conversations = data.conversations.map { if (it.id == id) transform(it) else it })
        }
        return true
    }

    /** data 级编辑（如 fork 新建对话），门控规则相同。 */
    fun editData(id: String, transform: (DesktopData) -> DesktopData): Boolean {
        if (registry.isRunning(id)) return false
        state.update { transform(it) }
        return true
    }

    /**
     * ask_user 答案路由：
     * 1. 执行中且答案在 pending 通道 → 注入，不续跑；
     * 2. 执行中但不在 pending（答案迟到/已取消）→ 忽略；
     * 3. 执行已结束且该 tool 结果尚不存在 → 插入 tool 消息并触发续跑；
     * 4. 该 tool 结果已存在 → 忽略（重复提交）。
     */
    fun answer(conversationId: String, call: DesktopToolCall, answer: String) {
        if (answerIfPending(call.id, answer)) return
        if (registry.isRunning(conversationId)) return
        val data = state.current()
        val conversation = data.conversations.firstOrNull { it.id == conversationId } ?: return
        if (conversation.messages.any { it.role == "tool" && it.toolCallId == call.id }) return
        state.update { current ->
            current.copy(conversations = current.conversations.map { conversation ->
                if (conversation.id == conversationId) {
                    conversation.copy(
                        messages = conversation.messages + ChatMessage(
                            role = "tool",
                            content = answer,
                            toolCallId = call.id
                        ),
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    conversation
                }
            })
        }
        resume(conversationId)
    }
}
