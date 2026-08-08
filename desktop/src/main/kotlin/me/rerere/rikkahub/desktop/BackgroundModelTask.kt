package me.rerere.rikkahub.desktop

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** 后台模型任务的前置结果：运行 / 静默跳过 / 被阻止（写入错误定位）。 */
internal sealed interface TaskPrecondition {
    data object Run : TaskPrecondition
    data object Skip : TaskPrecondition
    data class Blocked(val message: String) : TaskPrecondition
}

/** 一次流输出的校验结果：通过 / 换 attempt 重试 / 失败（错误消息）。 */
internal sealed interface TaskValidation {
    data object Pass : TaskValidation
    data object Retry : TaskValidation
    data class Fail(val message: String) : TaskValidation
}

internal data class BackgroundModelRequest(val config: DesktopConfig, val messages: List<ChatMessage>)

/**
 * 后台模型任务（标题生成、回复建议、历史压缩、消息翻译）的声明式描述。
 * 钩子均为纯函数：生命周期（防重入、取消、错误定位、完成清理）由 [BackgroundModelTaskRunner] 统一吸收。
 */
internal interface BackgroundModelTask {
    val conversationId: String

    /** 重试次数上限；返回 [TaskValidation.Retry] 的次数不能达到该上限（最后一次必须 Pass/Fail）。 */
    val maxAttempts: Int get() = 1

    fun canRun(data: DesktopData): TaskPrecondition

    /** 第 [attempt] 次请求（0 起）；重试时调用方会用新的 attempt 重新构造请求。 */
    fun request(data: DesktopData, attempt: Int): BackgroundModelRequest

    fun validate(output: String, attempt: Int): TaskValidation

    /** 应用最终输出；仅当 validate 返回 [TaskValidation.Pass] 时调用。 */
    fun apply(data: DesktopData, output: String): DesktopData
}

internal class BackgroundModelTaskRunner(
    private val model: ConversationModelStreamAdapter,
    private val state: ConversationExecutionState,
    private val registry: DesktopGenerationRegistry,
    private val reportError: (String, String) -> Unit,
    private val scope: CoroutineScope,
) {
    fun submit(task: BackgroundModelTask, registry: DesktopGenerationRegistry = this.registry) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val precondition = task.canRun(state.current())
                when (precondition) {
                    TaskPrecondition.Run -> runTask(task)
                    TaskPrecondition.Skip -> Unit
                    is TaskPrecondition.Blocked -> reportError(task.conversationId, precondition.message)
                }
            } catch (_: CancellationException) {
                // 取消保留原状态（与主执行语义一致）。
            } catch (error: Throwable) {
                reportError(task.conversationId, error.userFacingMessage())
            } finally {
                registry.finish(task.conversationId)
            }
        }
        if (!registry.begin(task.conversationId, job)) return
        job.start()
    }

    private suspend fun runTask(task: BackgroundModelTask) {
        for (attempt in 0 until task.maxAttempts) {
            val request = task.request(state.current(), attempt)
            val output = collectOutput(request.config, request.messages)
            val validation = task.validate(output, attempt)
            when (validation) {
                TaskValidation.Pass -> {
                    state.update { task.apply(it, output) }
                    return
                }
                is TaskValidation.Fail -> {
                    reportError(task.conversationId, validation.message)
                    return
                }
                TaskValidation.Retry -> Unit
            }
        }
        // 契约保证最后一次 attempt 返回 Pass 或 Fail，不会走到这里。
    }

    private suspend fun collectOutput(config: DesktopConfig, messages: List<ChatMessage>): String {
        val output = StringBuilder()
        model.stream(config, messages).collect { delta -> output.append(delta.content) }
        return output.toString()
    }
}

internal class DesktopTitleGenerationTask(
    override val conversationId: String,
    private val force: Boolean,
    private val language: DesktopLanguage,
    private val enableChineseTypography: Boolean,
) : BackgroundModelTask {
    override fun canRun(data: DesktopData): TaskPrecondition {
        val conversation = data.conversations.firstOrNull { it.id == conversationId }
            ?: return TaskPrecondition.Skip
        if (conversation.messages.isEmpty()) return TaskPrecondition.Skip
        if (!force && conversation.title != "新对话" && conversation.title.isNotBlank()) return TaskPrecondition.Skip
        return TaskPrecondition.Run
    }

    override fun request(data: DesktopData, attempt: Int): BackgroundModelRequest {
        val conversation = data.conversations.first { it.id == conversationId }
        val config = data.titleGenerationConfig(conversation)
        val content = conversation.messages.takeLast(4).joinToString("\n\n") { message ->
            "${message.role.uppercase()}: ${message.content.take(500)}"
        }
        return BackgroundModelRequest(config, listOf(ChatMessage(role = "user", content = config.titleRequest(content))))
    }

    override fun validate(output: String, attempt: Int): TaskValidation =
        if (normalizeGeneratedTitle(output, enableChineseTypography).isNotBlank()) TaskValidation.Pass
        else TaskValidation.Fail(desktopText(language, "runtime.title_empty"))

    override fun apply(data: DesktopData, output: String): DesktopData {
        val title = normalizeGeneratedTitle(output, enableChineseTypography)
        return data.copy(conversations = data.conversations.map { conversation ->
            if (conversation.id == conversationId) {
                conversation.copy(title = title, updatedAt = System.currentTimeMillis())
            } else {
                conversation
            }
        })
    }
}

internal class DesktopSuggestionGenerationTask(
    override val conversationId: String,
    private val language: DesktopLanguage,
    private val enableChineseTypography: Boolean,
) : BackgroundModelTask {
    override fun canRun(data: DesktopData): TaskPrecondition {
        val conversation = data.conversations.firstOrNull { it.id == conversationId }
            ?: return TaskPrecondition.Skip
        if (conversation.messages.isEmpty()) return TaskPrecondition.Skip
        return TaskPrecondition.Run
    }

    override fun request(data: DesktopData, attempt: Int): BackgroundModelRequest {
        val conversation = data.conversations.first { it.id == conversationId }
        val config = data.suggestionGenerationConfig(conversation)
        val content = conversation.messages.takeLast(6).joinToString("\n\n") { message ->
            "${message.role.uppercase()}: ${message.content.take(700)}"
        }
        val request = """
            <conversation>
            $content
            </conversation>

            The next turn in this conversation belongs to the human user.
            Write 3 suggestions for what the user would naturally type next, from the user's own
            first-person perspective, one suggestion per line.
        """.trimIndent()
        return BackgroundModelRequest(config, listOf(ChatMessage(role = "user", content = request)))
    }

    override fun validate(output: String, attempt: Int): TaskValidation =
        if (parseChatSuggestions(output, enableChineseTypography).isNotEmpty()) TaskValidation.Pass
        else TaskValidation.Fail(desktopText(language, "runtime.suggestions_empty"))

    override fun apply(data: DesktopData, output: String): DesktopData {
        val suggestions = parseChatSuggestions(output, enableChineseTypography)
        return data.copy(conversations = data.conversations.map { conversation ->
            if (conversation.id == conversationId) {
                conversation.copy(suggestions = suggestions, updatedAt = System.currentTimeMillis())
            } else {
                conversation
            }
        })
    }
}

internal class DesktopCompressionTask(
    override val conversationId: String,
    private val targetTokens: Int,
    private val keepRecentMessages: Int,
    private val additionalPrompt: String,
    private val language: DesktopLanguage,
) : BackgroundModelTask {
    override fun canRun(data: DesktopData): TaskPrecondition {
        val conversation = data.conversations.firstOrNull { it.id == conversationId }
            ?: return TaskPrecondition.Skip
        if (targetTokens <= 0 || keepRecentMessages < 0 || conversation.messages.size <= keepRecentMessages) {
            return TaskPrecondition.Blocked(desktopText(language, "runtime.not_enough_messages"))
        }
        return TaskPrecondition.Run
    }

    override fun request(data: DesktopData, attempt: Int): BackgroundModelRequest {
        val conversation = data.conversations.first { it.id == conversationId }
        val recentMessages = conversation.messages.takeRecentMessagesPreservingToolCalls(keepRecentMessages)
        val messagesToCompress = conversation.messages.dropLast(recentMessages.size)
        val config = data.configForConversation(conversation).compressionRequestConfig(maxTokens = targetTokens)
        val request = buildString {
            appendLine("You are a conversation compression assistant. Summarize the conversation below for a future assistant.")
            appendLine("Preserve facts, decisions, user preferences, unresolved work, and important details.")
            appendLine("Keep the same language where practical. Target approximately $targetTokens tokens.")
            appendLine("Output only the reusable summary, with no meta-commentary.")
            if (additionalPrompt.isNotBlank()) appendLine("Additional instructions: ${additionalPrompt.trim()}")
            appendLine("<conversation>")
            append(messagesToCompress.compressionTranscript())
            appendLine()
            append("</conversation>")
        }
        return BackgroundModelRequest(config, listOf(ChatMessage(role = "user", content = request)))
    }

    override fun validate(output: String, attempt: Int): TaskValidation =
        if (output.isNotBlank()) TaskValidation.Pass
        else TaskValidation.Fail(desktopText(language, "runtime.compression_empty"))

    override fun apply(data: DesktopData, output: String): DesktopData =
        data.copy(conversations = data.conversations.map { conversation ->
            if (conversation.id == conversationId) {
                conversation.replaceHistoryWithSummary(output, keepRecentMessages)
            } else {
                conversation
            }
        })
}

internal class DesktopTranslationTask(
    override val conversationId: String,
    private val messageIndex: Int,
    private val targetLanguage: String,
    private val language: DesktopLanguage,
) : BackgroundModelTask {
    override val maxAttempts: Int = TranslationAttemptLimit

    private var sourceText: String = ""

    override fun canRun(data: DesktopData): TaskPrecondition {
        val conversation = data.conversations.firstOrNull { it.id == conversationId }
            ?: return TaskPrecondition.Skip
        if (conversation.messages.getOrNull(messageIndex) == null) return TaskPrecondition.Skip
        return TaskPrecondition.Run
    }

    override fun request(data: DesktopData, attempt: Int): BackgroundModelRequest {
        val conversation = data.conversations.first { it.id == conversationId }
        sourceText = conversation.messages[messageIndex].content
        val config = data.configForConversation(conversation).translationRequestConfig()
        val request = buildMessageTranslationRequest(
            sourceText,
            targetLanguage,
            unchangedAttemptCount = attempt,
        )
        return BackgroundModelRequest(config, listOf(ChatMessage(role = "user", content = request)))
    }

    override fun validate(output: String, attempt: Int): TaskValidation {
        val trimmed = output.trim()
        val lastAttempt = attempt >= maxAttempts - 1
        return when {
            trimmed.isNotBlank() && !isTranslationUnchanged(sourceText, trimmed) -> TaskValidation.Pass
            lastAttempt && trimmed.isBlank() -> TaskValidation.Fail(desktopText(language, "runtime.translation_empty"))
            lastAttempt -> TaskValidation.Fail(desktopText(language, "runtime.translation_failed"))
            else -> TaskValidation.Retry
        }
    }

    override fun apply(data: DesktopData, output: String): DesktopData {
        val trimmed = output.trim()
        return data.copy(conversations = data.conversations.map { conversation ->
            if (conversation.id == conversationId) {
                conversation.copy(
                    messages = conversation.messages.mapIndexed { index, item ->
                        if (index == messageIndex) {
                            item.withTranslation(trimmed, targetLanguage.trim())
                        } else {
                            item
                        }
                    },
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                conversation
            }
        })
    }
}
