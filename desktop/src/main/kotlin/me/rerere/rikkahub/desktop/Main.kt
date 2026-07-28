package me.rerere.rikkahub.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.composables.icons.lucide.Bot
import com.composables.icons.lucide.ArrowDown
import com.composables.icons.lucide.ArrowDownToLine
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.ArrowUpToLine
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Database
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.GitFork
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Maximize2
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Paperclip
import com.composables.icons.lucide.Pin
import com.composables.icons.lucide.PinOff
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.RotateCcw
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Upload
import com.composables.icons.lucide.UserRound
import com.composables.icons.lucide.Wrench
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.blur.HazeColorEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.Base64
import java.io.File
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.StringSelection
import java.net.URI

private data class MessageEditTarget(
    val conversationId: String,
    val messageIndex: Int,
    val content: String
)

private data class ConversationRenameTarget(
    val conversationId: String,
    val title: String
)

private data class ConversationPromptTarget(
    val conversationId: String,
    val systemPrompt: String
)

private data class FolderCreateTarget(
    val conversationId: String? = null,
    val assistantId: String
)

private data class CompressionTarget(val conversationId: String)

private data class TranslationTarget(
    val conversationId: String,
    val messageIndex: Int,
    val content: String
)

private data class RenderedChatItem(
    val messageIndex: Int,
    val message: ChatMessage,
    val executionSteps: List<DesktopExecutionStep>,
    val timelineAfterContent: Boolean,
    val highlighted: Boolean,
)

private val MessageTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    .withZone(ZoneId.systemDefault())

private val FilledStar = ImageVector.Builder(
    name = "filled_star",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 2f)
        lineTo(15.09f, 8.26f)
        lineTo(22f, 9.27f)
        lineTo(17f, 14.14f)
        lineTo(18.18f, 21.02f)
        lineTo(12f, 17.77f)
        lineTo(5.82f, 21.02f)
        lineTo(7f, 14.14f)
        lineTo(2f, 9.27f)
        lineTo(8.91f, 8.26f)
        close()
    }
}.build()

private fun showOpenFileDialog(owner: Frame, title: String, multiple: Boolean): List<File>? {
    val dialog = FileDialog(owner, title, FileDialog.LOAD).apply {
        isMultipleMode = multiple
        isVisible = true
    }
    val selected = dialog.files.toList().ifEmpty {
        dialog.file?.let { listOf(File(dialog.directory, it)) }.orEmpty()
    }
    return selected.ifEmpty { null }
}

fun main() = application {
    val appIcon = rememberDesktopResourcePainter("icon.png")
    Window(
        onCloseRequest = ::exitApplication,
        title = "RikkaHub",
        icon = appIcon,
        state = WindowState(size = DpSize(1280.dp, 820.dp))
    ) {
        RikkaHubDesktop(dialogOwner = window)
    }
}

@Composable
private fun RikkaHubDesktop(
    dialogOwner: Frame,
    store: DesktopStore = remember { DesktopStore() },
    client: OpenAiClient = remember { OpenAiClient() },
    mcpClient: DesktopMcpClient = remember { DesktopMcpClient() }
) {
    var data by remember { mutableStateOf(store.load()) }
    var prompt by remember { mutableStateOf("") }
    var pendingAttachments by remember { mutableStateOf<List<DesktopAttachment>>(emptyList()) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsSection by remember { mutableStateOf(DesktopSettingsSection.GENERAL) }
    var showSidebar by remember { mutableStateOf(true) }
    var jumpToMessageId by remember { mutableStateOf<String?>(null) }
    var editTarget by remember { mutableStateOf<MessageEditTarget?>(null) }
    var renameTarget by remember { mutableStateOf<ConversationRenameTarget?>(null) }
    var conversationPromptTarget by remember { mutableStateOf<ConversationPromptTarget?>(null) }
    var folderCreateTarget by remember { mutableStateOf<FolderCreateTarget?>(null) }
    var compressionTarget by remember { mutableStateOf<CompressionTarget?>(null) }
    var showConversationStats by remember { mutableStateOf(false) }
    var translationTarget by remember { mutableStateOf<TranslationTarget?>(null) }
    var attachmentPickerOpen by remember { mutableStateOf(false) }
    var markdownExportTarget by remember { mutableStateOf<DesktopConversation?>(null) }
    var backupExportRequested by remember { mutableStateOf(false) }
    val conversationScrollPositions = remember { mutableMapOf<String, Pair<Int, Int>>() }
    val pendingAskUserAnswers = remember { mutableStateMapOf<String, CompletableDeferred<String>>() }
    var pendingAgentApproval by remember { mutableStateOf<PendingDesktopAgentApproval?>(null) }
    val rememberedAgentApprovals = remember { mutableStateMapOf<String, Set<DesktopAgentApprovalGrant>>() }
    val agentRuntime = remember { DesktopAgentRuntime() }
    val generationJobs = remember { mutableStateMapOf<String, Job>() }
    val suggestionJobs = remember { mutableStateMapOf<String, Job>() }
    val generationErrors = remember { mutableStateMapOf<String, String>() }
    var saveJob by remember { mutableStateOf<Job?>(null) }
    val latestData by rememberUpdatedState(data)
    val scope = rememberCoroutineScope()

    DisposableEffect(store) {
        onDispose {
            saveJob?.cancel()
            runCatching { store.save(latestData) }
        }
    }

    fun update(next: DesktopData) {
        data = next
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(750)
            withContext(Dispatchers.IO) { runCatching { store.save(next) } }
                .onFailure { error ->
                    generationErrors[next.selectedConversationId] = "保存失败：${error.message ?: "未知错误"}"
                }
        }
    }

    fun updateConversation(id: String, transform: (DesktopConversation) -> DesktopConversation) {
        update(data.copy(conversations = data.conversations.map { if (it.id == id) transform(it) else it }))
    }

    fun selectProvider(providerId: String, model: String? = null) {
        update(data.selectProviderConfig(providerId, model))
    }

    fun saveProvider(profile: DesktopProviderProfile) {
        update(data.saveProviderProfile(profile))
    }

    fun addProvider() {
        val profile = DesktopProviderProfile(
            name = "新服务商",
            config = DesktopConfig(model = "", systemPrompt = data.config.systemPrompt)
        )
        val currentProviders = data.providers.ifEmpty { listOf(data.activeProvider()) }
        update(
            data.copy(
                config = profile.config,
                providers = currentProviders + profile,
                selectedProviderId = profile.id
            )
        )
    }

    fun deleteProvider(providerId: String) {
        if (data.providers.size > 1 && data.providers.any { it.id == providerId }) {
            store.deleteProviderSecret(providerId)
        }
        update(data.deleteProviderProfile(providerId))
    }

    fun selectAssistantProfile(assistantId: String) {
        if (data.assistants.any { it.id == assistantId }) {
            update(data.copy(selectedAssistantId = assistantId))
        }
    }

    fun saveAssistant(profile: DesktopAssistantProfile) {
        update(data.saveAssistantProfile(profile))
    }

    fun addAssistant() {
        val assistant = DesktopAssistantProfile(name = "新助手")
        update(data.copy(assistants = data.assistants + assistant, selectedAssistantId = assistant.id))
    }

    fun copyAssistant(assistantId: String) {
        val source = data.assistants.firstOrNull { it.id == assistantId } ?: return
        val copy = source.copy(id = UUID.randomUUID().toString(), name = "${source.name} copy")
        update(data.copy(assistants = data.assistants + copy, selectedAssistantId = copy.id))
    }

    fun deleteAssistant(assistantId: String) {
        update(data.deleteAssistantProfile(assistantId))
    }

    fun selectConversationAssistant(conversationId: String, assistantId: String) {
        update(data.assignAssistantToConversation(conversationId, assistantId))
    }

    fun selectAssistantModel(assistantId: String, providerId: String, model: String) {
        val assistant = data.assistants.firstOrNull { it.id == assistantId } ?: return
        update(data.saveAssistantProfile(assistant.copy(providerId = providerId, model = model)))
    }

    fun updateAssistantReasoningEffort(assistantId: String, effort: String) {
        val assistant = data.assistants.firstOrNull { it.id == assistantId } ?: return
        update(data.saveAssistantProfile(assistant.copy(reasoningEffort = effort)))
    }

    fun newConversation() {
        val conversation = data.activeAssistant().newConversation()
        update(
            data.copy(
                conversations = listOf(conversation) + data.conversations,
                selectedConversationId = conversation.id
            )
        )
    }

    fun exportBackup(): String? {
        backupExportRequested = true
        return null
    }

    fun requestConversationExport(conversation: DesktopConversation) {
        markdownExportTarget = conversation
    }

    fun importBackup(): String? {
        val source = showOpenFileDialog(dialogOwner, "导入 RikkaHub 备份", multiple = false)?.firstOrNull() ?: return null
        val imported = store.importData(source.toPath())
        generationJobs.values.forEach { it.cancel() }
        generationJobs.clear()
        suggestionJobs.values.forEach { it.cancel() }
        suggestionJobs.clear()
        generationErrors.clear()
        prompt = ""
        pendingAttachments = emptyList()
        update(imported)
        return "已从 ${source.toPath()} 导入备份"
    }

    fun resetDesktopData() {
        generationJobs.values.forEach { it.cancel() }
        generationJobs.clear()
        suggestionJobs.values.forEach { it.cancel() }
        suggestionJobs.clear()
        generationErrors.clear()
        prompt = ""
        pendingAttachments = emptyList()
        store.clearSecrets(data)
        update(DesktopData())
    }

    fun chooseAttachments(): List<DesktopAttachment>? {
        return showOpenFileDialog(dialogOwner, "添加图片、音频、文本或文档", multiple = true)?.map(::loadDesktopAttachment)
    }

    fun memoryToolHandler(assistantId: String) = DesktopMemoryToolHandler(
        create = { content ->
            val memory = DesktopMemory(content = content.trim())
            update(data.updateMemories(assistantId) { it + memory })
            memory
        },
        edit = { id, content ->
            var updated: DesktopMemory? = null
            update(data.updateMemories(assistantId) { memories ->
                memories.map { memory ->
                    if (memory.id == id) memory.copy(content = content.trim()).also { updated = it } else memory
                }
            })
            updated ?: error("Memory record #$id not found")
        },
        delete = { id ->
            var found = false
            update(data.updateMemories(assistantId) { memories ->
                memories.filter { memory ->
                    (memory.id != id).also { if (!it) found = true }
                }
            })
            check(found) { "Memory record #$id not found" }
        }
    )

    fun startTitleGeneration(conversationId: String, force: Boolean = true) {
        if (generationJobs.containsKey(conversationId)) return
        val conversation = data.conversations.firstOrNull { it.id == conversationId } ?: return
        if (conversation.messages.isEmpty()) return
        if (!force && conversation.title != "新对话" && conversation.title.isNotBlank()) return
        val config = data.titleGenerationConfig(conversation)
        val content = conversation.messages.takeLast(4).joinToString("\n\n") { message ->
            "${message.role.uppercase()}: ${message.content.take(500)}"
        }
        val request = config.titleRequest(content)
        generationErrors.remove(conversationId)
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                var result = ""
                client.stream(config, listOf(ChatMessage(role = "user", content = request))).collect { delta ->
                    result += delta.content
                }
                val title = normalizeGeneratedTitle(result, data.preferences.enableChineseTypography)
                check(title.isNotBlank()) { "标题模型没有返回内容" }
                updateConversation(conversationId) { current ->
                    current.copy(title = title, updatedAt = System.currentTimeMillis())
                }
            } catch (_: CancellationException) {
                // Keep the existing title when the request is cancelled.
            } catch (error: Throwable) {
                generationErrors[conversationId] = error.message ?: "生成标题失败"
            } finally {
                generationJobs.remove(conversationId)
            }
        }
        generationJobs[conversationId] = job
        job.start()
    }

    fun startSuggestionGeneration(conversationId: String) {
        if (suggestionJobs.containsKey(conversationId)) return
        val conversation = data.conversations.firstOrNull { it.id == conversationId } ?: return
        if (conversation.messages.isEmpty()) return
        val config = data.configForConversation(conversation).backgroundRequestConfig(maxTokens = 256)
        val content = conversation.messages.takeLast(6).joinToString("\n\n") { message ->
            "${message.role.uppercase()}: ${message.content.take(700)}"
        }
        val request = """
            Suggest 3 concise, useful next messages the user could send to continue this conversation.
            Use the user's primary language. Reply with one suggestion per line and no introduction.

            <conversation>
            $content
            </conversation>
        """.trimIndent()
        generationErrors.remove(conversationId)
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                var result = ""
                client.stream(config, listOf(ChatMessage(role = "user", content = request))).collect { delta ->
                    result += delta.content
                }
                val suggestions = parseChatSuggestions(result, data.preferences.enableChineseTypography)
                check(suggestions.isNotEmpty()) { "建议模型没有返回内容" }
                updateConversation(conversationId) { current ->
                    current.copy(suggestions = suggestions, updatedAt = System.currentTimeMillis())
                }
            } catch (_: CancellationException) {
                // Cancellation leaves existing suggestions unchanged.
            } catch (error: Throwable) {
                generationErrors[conversationId] = error.message ?: "生成回复建议失败"
            } finally {
                suggestionJobs.remove(conversationId)
            }
        }
        suggestionJobs[conversationId] = job
        job.start()
    }

    fun startGeneration(
        conversationId: String,
        requestMessages: List<ChatMessage>,
        title: String? = null,
        alternativeTarget: ChatMessage? = null
    ) {
        if (generationJobs.containsKey(conversationId)) return
        val generationConversation = data.conversations.firstOrNull { it.id == conversationId } ?: return
        val generationAssistant = data.assistantFor(generationConversation)
        val selectedMcpServerIds = generationAssistant.mcpServerIds
        val selectedMcpServers = data.mcpServers.filter { server ->
            server.enabled && server.id in selectedMcpServerIds
        }
        val missingMcpServerIds = selectedMcpServerIds - data.mcpServers.map { it.id }.toSet()
        if (missingMcpServerIds.isNotEmpty()) {
            generationErrors[conversationId] = "MCP 配置已失效，请在助手设置中重新选择服务器"
            return
        }
        val serversNeedingSync = selectedMcpServers.filter { it.tools.isEmpty() }
        if (serversNeedingSync.isNotEmpty()) {
            generationErrors.remove(conversationId)
            val syncJob = scope.launch {
                var handedOffToGeneration = false
                try {
                    val toolsByServerId = serversNeedingSync.map { server ->
                        server.id to mcpClient.syncTools(server).also { tools ->
                            check(tools.isNotEmpty()) { "MCP 服务器 ${server.name} 未提供可用工具" }
                        }
                    }.toMap()
                    update(data.copy(mcpServers = data.mcpServers.map { server ->
                        toolsByServerId[server.id]?.let { tools -> server.copy(tools = tools) } ?: server
                    }))
                    generationJobs.remove(conversationId)
                    handedOffToGeneration = true
                    startGeneration(conversationId, requestMessages, title, alternativeTarget)
                } catch (error: Throwable) {
                    generationErrors[conversationId] = "MCP 工具同步失败：${error.message ?: "未知错误"}"
                } finally {
                    if (!handedOffToGeneration) generationJobs.remove(conversationId)
                }
            }
            generationJobs[conversationId] = syncJob
            return
        }
        val requestAssistant = if (generationConversation.usesPromptInjections(generationAssistant)) {
            generationAssistant
        } else {
            generationAssistant.copy(promptInjections = emptyList())
        }
        val baseGenerationConfig = data.configForConversation(generationConversation)
        val injected = requestAssistant.injectPromptMessages(
            requestAssistant.limitContext(requestMessages)
        )
        val generationConfig = baseGenerationConfig.copy(
            systemPrompt = (injected.systemPrefix + baseGenerationConfig.systemPrompt + injected.systemSuffix)
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
        )
        val generationMessages = runCatching {
            requestAssistant.renderMessageTemplate(
                requestAssistant.transformRequestMessages(
                    injected.messages
                )
            )
        }.getOrElse { error ->
            generationErrors[conversationId] = error.message ?: "无效的消息模板"
            return
        }
        generationErrors.remove(conversationId)
        updateConversation(conversationId) { conversation ->
            conversation.prepareGeneration(
                requestMessages = requestMessages,
                alternativeTarget = alternativeTarget,
                title = title,
                modelId = generationConfig.model
            )
        }

        val job = scope.launch(start = CoroutineStart.LAZY) {
            var completed = false
            try {
                var request = generationMessages
                var toolRounds = 0
                while (true) {
                    client.stream(generationConfig, request).collect { delta ->
                        updateConversation(conversationId) { conversation ->
                            val messages = conversation.messages.toMutableList()
                            val last = messages.lastOrNull()
                            if (last?.role == "assistant") {
                                val receivedAt = System.currentTimeMillis()
                                val reasoningStartedAt = last.reasoningStartedAt
                                    ?: delta.reasoning.takeIf { it.isNotBlank() }?.let { receivedAt }
                                messages[messages.lastIndex] = last.copy(
                                    content = last.content + delta.content,
                                    reasoning = last.reasoning + delta.reasoning,
                                    reasoningStartedAt = reasoningStartedAt,
                                    reasoningDurationMillis = if (delta.reasoning.isNotBlank() && reasoningStartedAt != null) {
                                        (receivedAt - reasoningStartedAt).coerceAtLeast(0)
                                    } else {
                                        last.reasoningDurationMillis
                                    },
                                    modelId = delta.modelId ?: last.modelId,
                                    promptTokens = delta.promptTokens ?: last.promptTokens,
                                    completionTokens = delta.completionTokens ?: last.completionTokens,
                                    cachedTokens = delta.cachedTokens ?: last.cachedTokens,
                                    citations = (last.citations + delta.citations).distinctBy { it.url },
                                    toolCalls = last.toolCalls.merge(delta.toolCallDeltas)
                                )
                            }
                            conversation.copy(messages = messages, updatedAt = System.currentTimeMillis())
                        }
                    }
                    val toolCalls = data.conversations.firstOrNull { it.id == conversationId }
                        ?.messages?.lastOrNull()?.toolCalls.orEmpty()
                    if (toolCalls.isEmpty()) break
                    // The model has finished this message. Freeze reasoning before any approval or tool wait.
                    updateConversation(conversationId) { conversation ->
                        val messages = conversation.messages.toMutableList()
                        val last = messages.lastOrNull()
                        if (last?.role == "assistant") {
                            messages[messages.lastIndex] = last.completeReasoningDuration()
                        }
                        conversation.copy(messages = messages, updatedAt = System.currentTimeMillis())
                    }
                    if (generationAssistant.maxToolRounds > 0 && toolRounds >= generationAssistant.maxToolRounds) {
                        val limitMessage = "本次回复已达到 ${generationAssistant.maxToolRounds} 轮工具调用上限。请继续发送消息以开始新的处理。"
                        updateConversation(conversationId) { conversation ->
                            conversation.copy(
                                messages = conversation.messages + toolCalls.map { call ->
                                    ChatMessage(role = "tool", content = limitMessage, toolCallId = call.id)
                                } + ChatMessage(role = "assistant", content = limitMessage),
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                        break
                    }
                    toolRounds++
                    val results = client.executeToolCalls(
                        generationConfig,
                        toolCalls,
                        memoryToolHandler(generationAssistant.id),
                        mcpClient,
                        askUserHandler = { call ->
                            val answer = CompletableDeferred<String>()
                            pendingAskUserAnswers[call.id] = answer
                            try {
                                answer.await()
                            } finally {
                                pendingAskUserAnswers.remove(call.id, answer)
                            }
                        },
                        agentRuntime = agentRuntime,
                        approvalHandler = { call, request ->
                            withContext(Dispatchers.Main) {
                                if (rememberedAgentApprovals[conversationId].orEmpty().approves(request)) return@withContext true
                                val answer = CompletableDeferred<DesktopAgentApprovalDecision>()
                                pendingAgentApproval = PendingDesktopAgentApproval(call, request, answer)
                                try {
                                    answer.await().also { decision ->
                                        if (decision.approved && decision.autoApprove) {
                                            request.rememberedGrant()?.let { grant ->
                                                rememberedAgentApprovals[conversationId] =
                                                    rememberedAgentApprovals[conversationId].orEmpty() + grant
                                            }
                                        }
                                    }.approved
                                } finally {
                                    if (pendingAgentApproval?.answer === answer) pendingAgentApproval = null
                                }
                            }
                        }
                    )
                    updateConversation(conversationId) { conversation ->
                        conversation.copy(
                            messages = conversation.messages + results,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    if (results.any { it.content == DesktopAgentApprovalDeniedResult }) break
                    val current = data.conversations.firstOrNull { it.id == conversationId } ?: break
                    request = requestAssistant.renderMessageTemplate(
                        requestAssistant.transformRequestMessages(
                            requestAssistant.injectPromptMessages(
                                requestAssistant.limitContext(current.messages)
                            ).messages
                        )
                    )
                    updateConversation(conversationId) { conversation ->
                        conversation.copy(
                            messages = conversation.messages + ChatMessage(
                                role = "assistant",
                                content = "",
                                modelId = generationConfig.model
                            ),
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }
                updateConversation(conversationId) { conversation ->
                    val messages = conversation.messages.toMutableList()
                    val last = messages.lastOrNull()
                    if (last?.role == "assistant") {
                        messages[messages.lastIndex] = generationAssistant.transformGeneratedMessage(last.completeReasoningDuration())
                            .completeAlternative()
                    }
                    conversation.copy(messages = messages, updatedAt = System.currentTimeMillis())
                }
                completed = true
            } catch (_: CancellationException) {
                updateConversation(conversationId) { conversation ->
                    val last = conversation.messages.lastOrNull()
                    if (last?.role == "assistant" && last.content.isEmpty() && last.reasoning.isEmpty()) {
                        conversation.copy(
                            messages = if (alternativeTarget == null) {
                                conversation.messages.dropLast(1)
                            } else {
                                conversation.messages.dropLast(1) + alternativeTarget
                            }
                        )
                    } else if (last?.role == "assistant") {
                        conversation.copy(
                            messages = conversation.messages.dropLast(1) +
                                generationAssistant.transformGeneratedMessage(last.completeReasoningDuration()).completeAlternative()
                        )
                    } else {
                        conversation
                    }
                }
            } catch (error: Throwable) {
                generationErrors[conversationId] = error.message ?: "请求失败"
                updateConversation(conversationId) { conversation ->
                    val last = conversation.messages.lastOrNull()
                    if (last?.role == "assistant" && last.content.isEmpty() && last.reasoning.isEmpty()) {
                        conversation.copy(
                            messages = if (alternativeTarget == null) {
                                conversation.messages.dropLast(1)
                            } else {
                                conversation.messages.dropLast(1) + alternativeTarget
                            }
                        )
                    } else if (last?.role == "assistant") {
                        conversation.copy(
                            messages = conversation.messages.dropLast(1) +
                                generationAssistant.transformGeneratedMessage(last.completeReasoningDuration()).completeAlternative()
                        )
                    } else {
                        conversation
                    }
                }
            } finally {
                generationJobs.remove(conversationId)
                if (completed) {
                    val latest = data.conversations.firstOrNull { it.id == conversationId }
                    if (latest?.title == "新对话" && latest.messages.any { it.role == "assistant" }) {
                        startTitleGeneration(conversationId, force = false)
                    }
                    startSuggestionGeneration(conversationId)
                }
            }
        }
        generationJobs[conversationId] = job
        job.start()
    }

    fun submitAskUserAnswer(conversationId: String, toolCall: DesktopToolCall, answer: String) {
        if (pendingAskUserAnswers.remove(toolCall.id)?.complete(answer) == true) return
        if (generationJobs.containsKey(conversationId)) return
        val conversation = data.conversations.firstOrNull { it.id == conversationId } ?: return
        if (conversation.messages.any { it.role == "tool" && it.toolCallId == toolCall.id }) return
        updateConversation(conversationId) {
            it.copy(
                messages = it.messages + ChatMessage(role = "tool", content = answer, toolCallId = toolCall.id),
                updatedAt = System.currentTimeMillis()
            )
        }
        startGeneration(conversationId, data.conversations.first { it.id == conversationId }.messages)
    }

    fun startCompression(conversationId: String, targetTokens: Int, keepRecentMessages: Int, additionalPrompt: String) {
        if (generationJobs.containsKey(conversationId)) return
        val conversation = data.conversations.firstOrNull { it.id == conversationId } ?: return
        if (targetTokens <= 0 || keepRecentMessages < 0 || conversation.messages.size <= keepRecentMessages) {
            generationErrors[conversationId] = "没有足够的消息可压缩"
            return
        }
        val messagesToCompress = conversation.messages.dropLast(keepRecentMessages)
        val config = data.configForConversation(conversation).backgroundRequestConfig(maxTokens = targetTokens)
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
        generationErrors.remove(conversationId)
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                var summary = ""
                client.stream(config, listOf(ChatMessage(role = "user", content = request))).collect { delta ->
                    summary += delta.content
                }
                check(summary.isNotBlank()) { "压缩模型没有返回摘要" }
                updateConversation(conversationId) { current ->
                    current.replaceHistoryWithSummary(summary, keepRecentMessages)
                }
            } catch (_: CancellationException) {
                // Cancellation leaves the original conversation untouched.
            } catch (error: Throwable) {
                generationErrors[conversationId] = error.message ?: "压缩对话失败"
            } finally {
                generationJobs.remove(conversationId)
            }
        }
        generationJobs[conversationId] = job
        job.start()
    }

    fun startTranslation(target: TranslationTarget, language: String) {
        if (generationJobs.containsKey(target.conversationId)) return
        val conversation = data.conversations.firstOrNull { it.id == target.conversationId } ?: return
        val message = conversation.messages.getOrNull(target.messageIndex) ?: return
        val config = data.configForConversation(conversation).backgroundRequestConfig()
        val request = """
            Translate the text in <source_text> into ${language.trim()}.
            Return only the translation, without explanations.

            <source_text>
            ${message.content}
            </source_text>
        """.trimIndent()
        generationErrors.remove(target.conversationId)
        val job = scope.launch(start = CoroutineStart.LAZY) {
            var translatedMessageId: String? = null
            try {
                var result = ""
                client.stream(config, listOf(ChatMessage(role = "user", content = request))).collect { delta ->
                    result += delta.content
                }
                check(result.isNotBlank()) { "翻译模型没有返回内容" }
                updateConversation(target.conversationId) { current ->
                    current.copy(
                        messages = current.messages.mapIndexed { index, item ->
                            if (index == target.messageIndex) {
                                item.withTranslation(result.trim(), language.trim())
                            } else item
                        },
                        updatedAt = System.currentTimeMillis()
                    )
                }
                translatedMessageId = message.id
            } catch (_: CancellationException) {
                // Cancellation leaves the original message untouched.
            } catch (error: Throwable) {
                generationErrors[target.conversationId] = error.message ?: "翻译失败"
            } finally {
                generationJobs.remove(target.conversationId)
                translatedMessageId?.let { messageId -> jumpToMessageId = messageId }
            }
        }
        generationJobs[target.conversationId] = job
        job.start()
    }

    val selected = data.conversations.firstOrNull { it.id == data.selectedConversationId }
        ?: data.conversations.first()
    LaunchedEffect(selected.id, selected.draft, selected.draftAttachments) {
        prompt = selected.draft
        pendingAttachments = selected.draftAttachments
    }
    val selectedAssistant = data.assistantFor(selected)
    val effectiveConfig = data.configForAssistant(selectedAssistant)
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (data.preferences.colorMode) {
        DesktopColorMode.SYSTEM -> systemDark
        DesktopColorMode.LIGHT -> false
        DesktopColorMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = desktopColorScheme(data.preferences.themeColor, useDarkTheme),
        typography = desktopTypography(data.preferences.fontFamily)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(
            Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.N) {
                    newConversation()
                    showSettings = false
                    true
                } else {
                    false
                }
            }
        ) {
        val compact = maxWidth < 850.dp
        val sidebarWidth = if (compact) maxWidth else 292.dp
        Row(Modifier.fillMaxSize()) {
            if (!compact || showSidebar) {
                ConversationSidebar(
                    data = data,
                    settingsSelected = showSettings,
                    generatingConversationIds = generationJobs.keys,
                    onSelect = {
                        update(data.copy(selectedConversationId = it))
                        jumpToMessageId = null
                        showSettings = false
                        if (compact) showSidebar = false
                    },
                    onSelectFavorite = { conversationId, messageId ->
                        update(data.copy(selectedConversationId = conversationId))
                        jumpToMessageId = messageId
                        showSettings = false
                        if (compact) showSidebar = false
                    },
                    onNew = {
                        newConversation()
                        showSettings = false
                        if (compact) showSidebar = false
                    },
                    onDelete = { id ->
                        generationJobs.remove(id)?.cancel()
                        suggestionJobs.remove(id)?.cancel()
                        generationErrors.remove(id)
                        update(data.deleteConversation(id))
                    },
                    onPin = { id ->
                        updateConversation(id) { it.copy(isPinned = !it.isPinned, updatedAt = System.currentTimeMillis()) }
                    },
                    onMoveToFolder = { conversationId, folderId ->
                        update(data.moveConversationToFolder(conversationId, folderId))
                    },
                    onRenameFolder = { folderId, name ->
                        update(data.renameFolder(folderId, name))
                    },
                    onDeleteFolder = { folderId ->
                        update(data.deleteFolder(folderId))
                    },
                    onCreateFolder = { assistantId ->
                        folderCreateTarget = FolderCreateTarget(assistantId = assistantId)
                    },
                    onConversationSortChange = { sort ->
                        update(data.copy(preferences = data.preferences.copy(conversationSort = sort)))
                    },
                    onSettings = {
                        settingsSection = DesktopSettingsSection.GENERAL
                        showSettings = true
                        if (compact) showSidebar = false
                    },
                    modifier = Modifier.width(sidebarWidth)
                )
            }
            if (!compact || !showSidebar) {
                if (showSettings) {
                    DesktopSettingsPane(
                        providers = data.providers.ifEmpty { listOf(data.activeProvider()) },
                        selectedProviderId = data.activeProvider().id,
                        assistants = data.assistants.ifEmpty { listOf(data.activeAssistant()) },
                        selectedAssistantId = data.activeAssistant().id,
                        preferences = data.preferences,
                        globalMemories = data.globalMemories,
                        webSearchSettings = data.webSearchSettings,
                        client = client,
                        mcpServers = data.mcpServers,
                        mcpClient = mcpClient,
                        initialSection = settingsSection,
                        showMenu = compact,
                        onMenu = { showSidebar = true },
                        onBack = { showSettings = false },
                        onProviderSelect = ::selectProvider,
                        onProviderSave = ::saveProvider,
                        onProviderAdd = ::addProvider,
                        onProviderDelete = ::deleteProvider,
                        onAssistantSelect = ::selectAssistantProfile,
                        onAssistantSave = ::saveAssistant,
                        onAssistantAdd = ::addAssistant,
                        onAssistantCopy = ::copyAssistant,
                        onAssistantDelete = ::deleteAssistant,
                        onExportData = {
                            runCatching { exportBackup() }.getOrElse { "导出失败：${it.message}" }
                        },
                        onImportData = {
                            runCatching { importBackup() }.getOrElse { "导入失败：${it.message}" }
                        },
                        onResetData = ::resetDesktopData,
                        onWebSearchSettingsChange = { update(data.copy(webSearchSettings = it)) },
                        onMcpServersChange = { update(data.copy(mcpServers = it)) },
                        onPreferencesChange = { update(data.copy(preferences = it)) },
                        onGlobalMemoriesChange = { update(data.copy(globalMemories = it.filter { memory -> memory.content.isNotBlank() })) }
                    )
                } else {
                    ChatPane(
                        conversation = selected,
                        prompt = prompt,
                        isGenerating = generationJobs.containsKey(selected.id),
                        errorMessage = generationErrors[selected.id],
                        model = effectiveConfig.model,
                        assistant = selectedAssistant,
                        assistants = data.assistants.ifEmpty { listOf(data.activeAssistant()) },
                        preferences = data.preferences,
                        providers = data.providers.ifEmpty { listOf(data.activeProvider()) },
                        folders = data.folders.filter { it.assistantId == selectedAssistant.id },
                        selectedProviderId = selectedAssistant.providerId.ifBlank { data.activeProvider().id },
                        webSearchEnabled = selected.webSearchEnabled ?: selectedAssistant.enableWebSearch,
                        jumpToMessageId = jumpToMessageId,
                        conversationScrollPositions = conversationScrollPositions,
                        onAskUserAnswer = ::submitAskUserAnswer,
                        showMenu = compact,
                        onMenu = { showSidebar = true },
                        onNew = ::newConversation,
                        onSettings = {
                            settingsSection = DesktopSettingsSection.PROVIDERS
                            showSettings = true
                        },
                        onAssistantSettings = {
                            settingsSection = DesktopSettingsSection.ASSISTANTS
                            showSettings = true
                        },
                        onProviderModelSelect = { providerId, selectedModel ->
                            selectAssistantModel(selectedAssistant.id, providerId, selectedModel)
                        },
                        onReasoningEffortChange = { effort -> updateAssistantReasoningEffort(selectedAssistant.id, effort) },
                        client = client,
                        onAssistantSelect = { assistantId ->
                            selectConversationAssistant(selected.id, assistantId)
                        },
                        onToggleWebSearch = {
                            updateConversation(selected.id) { conversation ->
                                val current = conversation.webSearchEnabled ?: selectedAssistant.enableWebSearch
                                conversation.copy(webSearchEnabled = !current, updatedAt = System.currentTimeMillis())
                            }
                        },
                        onPromptChange = { value ->
                            prompt = value
                            updateConversation(selected.id) { conversation ->
                                conversation.copy(draft = value, updatedAt = System.currentTimeMillis())
                            }
                        },
                        pendingAttachments = pendingAttachments,
                        onAddAttachments = { attachmentPickerOpen = true },
                        onRemoveAttachment = { attachment ->
                            pendingAttachments = pendingAttachments.filterNot { it == attachment }
                            updateConversation(selected.id) { conversation ->
                                conversation.copy(draftAttachments = pendingAttachments, updatedAt = System.currentTimeMillis())
                            }
                        },
                        onDismissError = { generationErrors.remove(selected.id) },
                        onCancel = { generationJobs[selected.id]?.cancel() },
                        onRename = { renameTarget = ConversationRenameTarget(selected.id, selected.title) },
                        onExportConversation = { requestConversationExport(selected) },
                        onMoveToFolder = { folderId ->
                            update(data.moveConversationToFolder(selected.id, folderId))
                        },
                        onCreateFolder = {
                            folderCreateTarget = FolderCreateTarget(selected.id, selectedAssistant.id)
                        },
                        onCompress = { compressionTarget = CompressionTarget(selected.id) },
                        onGenerateTitle = { startTitleGeneration(selected.id) },
                        onShowStats = { showConversationStats = true },
                        onGenerateSuggestions = { startSuggestionGeneration(selected.id) },
                        onTogglePromptInjections = {
                            updateConversation(selected.id) { conversation ->
                                conversation.copy(
                                    promptInjectionsEnabled = !conversation.usesPromptInjections(selectedAssistant),
                                    updatedAt = System.currentTimeMillis()
                                )
                            }
                        },
                        onTranslateMessage = { index ->
                            selected.messages.getOrNull(index)?.let { message ->
                                translationTarget = TranslationTarget(selected.id, index, message.content)
                            }
                        },
                        onRestoreBranch = { branchId ->
                            if (!generationJobs.containsKey(selected.id)) {
                                updateConversation(selected.id) { it.restoreBranch(branchId) }
                            }
                        },
                        onDeleteBranch = { branchId ->
                            if (!generationJobs.containsKey(selected.id)) {
                                updateConversation(selected.id) { it.deleteBranch(branchId) }
                            }
                        },
                        onEditSystemPrompt = {
                            conversationPromptTarget = ConversationPromptTarget(selected.id, selected.systemPrompt)
                        },
                        onEditMessage = { index, content ->
                            editTarget = MessageEditTarget(selected.id, index, content)
                        },
                        onDeleteMessage = { index ->
                            if (!generationJobs.containsKey(selected.id)) {
                                updateConversation(selected.id) { conversation -> conversation.deleteMessageAt(index) }
                            }
                        },
                        onToggleMessageFavorite = { index ->
                            if (!generationJobs.containsKey(selected.id)) {
                                updateConversation(selected.id) { conversation ->
                                    conversation.copy(
                                        messages = conversation.messages.mapIndexed { messageIndex, message ->
                                            if (messageIndex == index) message.copy(isFavorite = !message.isFavorite) else message
                                        },
                                        updatedAt = System.currentTimeMillis()
                                    )
                                }
                            }
                        },
                        onForkAtMessage = { index ->
                            if (!generationJobs.containsKey(selected.id)) {
                                val fork = selected.forkAtMessage(index)
                                update(
                                    data.copy(
                                        conversations = listOf(fork) + data.conversations,
                                        selectedConversationId = fork.id
                                    )
                                )
                            }
                        },
                        onRegenerateMessage = { index ->
                            if (!generationJobs.containsKey(selected.id)) {
                                val target = selected.messages[index]
                                val requestMessages = if (target.role == "assistant") {
                                    selected.messages.take(index)
                                } else {
                                    selected.messages.take(index + 1)
                                }
                                startGeneration(
                                    selected.id,
                                    requestMessages,
                                    alternativeTarget = target.takeIf { it.role == "assistant" }
                                )
                            }
                        },
                        onSelectMessageVariant = { messageIndex, variantIndex ->
                            if (!generationJobs.containsKey(selected.id)) {
                                updateConversation(selected.id) { conversation ->
                                    conversation.selectMessageVariantAt(messageIndex, variantIndex)
                                }
                            }
                        },
                        onSend = {
                            val text = prompt.trim()
                            if ((text.isNotEmpty() || pendingAttachments.isNotEmpty()) &&
                                !generationJobs.containsKey(selected.id)
                            ) {
                                val attachments = pendingAttachments
                                val userMessage = ChatMessage(
                                    role = "user",
                                    content = text,
                                    attachments = attachments
                                )
                                val requestMessages = selected.messages + userMessage
                                startGeneration(selected.id, requestMessages)
                            }
                        },
                        onAddWithoutResponse = {
                            val text = prompt.trim()
                            if ((text.isNotEmpty() || pendingAttachments.isNotEmpty()) &&
                                !generationJobs.containsKey(selected.id)
                            ) {
                                val attachments = pendingAttachments
                                val userMessage = ChatMessage(
                                    role = "user",
                                    content = text,
                                    attachments = attachments
                                )
                                val titleText = text.ifBlank { attachments.firstOrNull()?.name.orEmpty() }
                                updateConversation(selected.id) { conversation ->
                                    conversation.copy(
                                        title = if (conversation.title == "新对话") {
                                            titleText.take(48)
                                        } else conversation.title,
                                        messages = conversation.messages + userMessage,
                                        draft = "",
                                        draftAttachments = emptyList(),
                                        suggestions = emptyList(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
        }

    editTarget?.let { target ->
        TextEditDialog(
            title = "编辑消息",
            initialValue = target.content,
            onDismiss = { editTarget = null },
            onSave = { content ->
                val conversation = data.conversations.firstOrNull { it.id == target.conversationId }
                val message = conversation?.messages?.getOrNull(target.messageIndex)
                if (conversation != null && message != null && content.isNotBlank()) {
                    updateConversation(target.conversationId) { current -> current.editMessageAt(target.messageIndex, content) }
                    if (message.role == "user") {
                        val requestMessages = conversation.messages.take(target.messageIndex) + message.addVariant(content)
                        startGeneration(target.conversationId, requestMessages)
                    }
                    editTarget = null
                }
            }
        )
    }

    renameTarget?.let { target ->
        TextEditDialog(
            title = "重命名对话",
            initialValue = target.title,
            singleLine = true,
            onDismiss = { renameTarget = null },
            onSave = { title ->
                if (title.isNotBlank()) {
                    updateConversation(target.conversationId) {
                        it.copy(title = title.trim(), updatedAt = System.currentTimeMillis())
                    }
                    renameTarget = null
                }
            }
        )
    }

    conversationPromptTarget?.let { target ->
        TextEditDialog(
            title = "对话系统提示词",
            initialValue = target.systemPrompt,
            allowBlank = true,
            onDismiss = { conversationPromptTarget = null },
            onSave = { systemPrompt ->
                updateConversation(target.conversationId) {
                    it.copy(systemPrompt = systemPrompt.trim(), updatedAt = System.currentTimeMillis())
                }
                conversationPromptTarget = null
            }
        )
    }
    folderCreateTarget?.let { target ->
        TextEditDialog(
            title = "新建文件夹",
            initialValue = "",
            singleLine = true,
            onDismiss = { folderCreateTarget = null },
            onSave = { name ->
                if (name.isNotBlank()) {
                    val folder = DesktopFolder(assistantId = target.assistantId, name = name.trim())
                    update(data.createFolder(folder, target.conversationId))
                    folderCreateTarget = null
                }
            }
        )
    }
    compressionTarget?.let { target ->
        val conversation = data.conversations.firstOrNull { it.id == target.conversationId }
        if (conversation == null) {
            compressionTarget = null
        } else {
            CompressionDialog(
                messageCount = conversation.messages.size,
                onDismiss = { compressionTarget = null },
                onConfirm = { targetTokens, keepRecentMessages, additionalPrompt ->
                    compressionTarget = null
                    startCompression(target.conversationId, targetTokens, keepRecentMessages, additionalPrompt)
                }
            )
        }
    }
    if (showConversationStats) {
        ConversationStatsDialog(conversation = selected, onDismiss = { showConversationStats = false })
    }
    translationTarget?.let { target ->
        TranslationDialog(
            onDismiss = { translationTarget = null },
            onConfirm = { language ->
                translationTarget = null
                startTranslation(target, language)
            }
        )
    }
    if (attachmentPickerOpen) {
        DesktopAttachmentPickerDialog(
            onDismiss = { attachmentPickerOpen = false },
            onSelect = { files ->
                attachmentPickerOpen = false
                runCatching { files.map(::loadDesktopAttachment) }.fold(
                    onSuccess = { attachments ->
                        pendingAttachments = (pendingAttachments + attachments).distinctBy { it.name to it.data }
                        updateConversation(selected.id) { conversation ->
                            conversation.copy(draftAttachments = pendingAttachments, updatedAt = System.currentTimeMillis())
                        }
                    },
                    onFailure = { error -> generationErrors[selected.id] = error.message ?: "无法添加文件" }
                )
            }
        )
    }
    markdownExportTarget?.let { conversation ->
        DesktopSaveFileDialog(
            title = "将对话导出为 Markdown",
            suggestedName = "${conversation.title.ifBlank { "conversation" }.take(64)}.md",
            requiredExtension = "md",
            onDismiss = { markdownExportTarget = null },
            onSave = { destination ->
                markdownExportTarget = null
                runCatching {
                    destination.writeText(
                        exportConversationMarkdown(conversation, data.configForConversation(conversation).systemPrompt)
                    )
                }.onFailure { error ->
                    generationErrors[conversation.id] = "导出失败：${error.message ?: "未知错误"}"
                }
            }
        )
    }
    if (backupExportRequested) {
        DesktopSaveFileDialog(
            title = "导出 RikkaHub 备份",
            suggestedName = "rikkahub-desktop-backup.json",
            requiredExtension = "json",
            onDismiss = { backupExportRequested = false },
            onSave = { destination ->
                backupExportRequested = false
                runCatching { store.exportData(destination.toPath(), data) }.onFailure { error ->
                    generationErrors[data.selectedConversationId] = "导出失败：${error.message ?: "未知错误"}"
                }
            }
        )
    }
    pendingAgentApproval?.let { pending ->
        DesktopAgentApprovalDialog(
            request = pending.request,
            onApprove = { autoApprove -> pending.answer.complete(DesktopAgentApprovalDecision(true, autoApprove)) },
            onDeny = { pending.answer.complete(DesktopAgentApprovalDecision(false, false)) }
        )
    }
    }
}

private data class PendingDesktopAgentApproval(
    val call: DesktopToolCall,
    val request: DesktopAgentApprovalRequest,
    val answer: CompletableDeferred<DesktopAgentApprovalDecision>
)

private data class DesktopAgentApprovalDecision(val approved: Boolean, val autoApprove: Boolean)

@Composable
private fun DesktopAgentApprovalDialog(
    request: DesktopAgentApprovalRequest,
    onApprove: (Boolean) -> Unit,
    onDeny: () -> Unit
) {
    var autoApprove by remember(request) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text(request.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Agent 请求此操作，是否允许？")
                Text(request.detail, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text(
                    when (request.kind) {
                        DesktopAgentApprovalKind.SHELL -> when (request.backend) {
                            DesktopAgentBackend.DOCKER -> if (request.network) {
                                "将在受限 Docker 容器中执行，并临时使用宿主网络；完成后恢复到无外网的隔离容器。"
                            } else {
                                "将在受限 Docker 容器中执行；容器仅挂载选定工作区。"
                            }
                            DesktopAgentBackend.LOCAL -> "本机 Shell 以当前用户权限运行；请确认命令及其影响。"
                            null -> "请确认命令及其影响。"
                        }
                        DesktopAgentApprovalKind.IMAGE_PULL -> "将从镜像仓库下载内容到本机 Docker 缓存。"
                        DesktopAgentApprovalKind.SKILL -> "Skill 指令可能影响后续 Agent 行为。"
                        DesktopAgentApprovalKind.WRITE -> "文件修改限制在已选定的工作区目录内。"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                if (request.canRemember) {
                    FilterChip(
                        selected = autoApprove,
                        onClick = { autoApprove = !autoApprove },
                        label = {
                            Text(
                                when (request.rememberedGrant()?.scope) {
                                    DesktopAgentApprovalScope.DOCKER_SHELL -> "本对话内自动批准此工作区的 Docker 命令"
                                    DesktopAgentApprovalScope.DOCKER_NETWORK -> "本对话内自动批准此工作区的 Docker 联网命令"
                                    DesktopAgentApprovalScope.IMAGE_PULL -> "本对话内自动批准此工作区的此镜像下载"
                                    else -> "本对话内自动批准此类操作"
                                }
                            )
                        }
                    )
                }
            }
        },
        confirmButton = { Button(onClick = { onApprove(autoApprove) }) { Text("允许") } },
        dismissButton = { TextButton(onClick = onDeny) { Text("拒绝") } }
    )
}

@Composable
private fun CompressionDialog(
    messageCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (targetTokens: Int, keepRecentMessages: Int, additionalPrompt: String) -> Unit
) {
    var targetTokens by remember { mutableStateOf("1500") }
    var keepRecentMessages by remember { mutableStateOf(minOf(8, messageCount - 1).toString()) }
    var additionalPrompt by remember { mutableStateOf("") }
    val target = targetTokens.toIntOrNull()
    val keep = keepRecentMessages.toIntOrNull()
    val valid = target != null && target > 0 && keep != null && keep >= 0 && keep < messageCount
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("压缩对话历史") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "将较早消息生成摘要，并保留最近消息。压缩前的完整历史会保存为可恢复快照。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = targetTokens,
                    onValueChange = { value -> if (value.all(Char::isDigit)) targetTokens = value },
                    label = { Text("目标 Token 数") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = keepRecentMessages,
                    onValueChange = { value -> if (value.all(Char::isDigit)) keepRecentMessages = value },
                    label = { Text("保留最近消息数（共 $messageCount 条）") },
                    isError = keep != null && (keep < 0 || keep >= messageCount),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = additionalPrompt,
                    onValueChange = { additionalPrompt = it },
                    label = { Text("附加说明（可选）") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onConfirm(target!!, keep!!, additionalPrompt) }
            ) { Text("压缩") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ConversationStatsDialog(conversation: DesktopConversation, onDismiss: () -> Unit) {
    val stats = conversation.stats()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("对话统计") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ConversationStatRow("消息", "${stats.messageCount} 条（用户 ${stats.userMessageCount} / 助手 ${stats.assistantMessageCount}）")
                ConversationStatRow("附件", "${stats.attachmentCount} 个")
                ConversationStatRow("文本与思维链字符", stats.characterCount.toString())
                ConversationStatRow("输入 Token", stats.promptTokens.toString())
                ConversationStatRow("输出 Token", stats.completionTokens.toString())
                ConversationStatRow("缓存 Token", stats.cachedTokens.toString())
                ConversationStatRow("创建时间", MessageTimeFormatter.format(Instant.ofEpochMilli(conversation.createdAt)))
                ConversationStatRow("更新时间", MessageTimeFormatter.format(Instant.ofEpochMilli(conversation.updatedAt)))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun TranslationDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var language by remember { mutableStateOf("中文") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("翻译消息") },
        text = {
            OutlinedTextField(
                value = language,
                onValueChange = { language = it },
                label = { Text("目标语言") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(language.trim()) }, enabled = language.isNotBlank()) { Text("翻译") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ConversationStatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@Composable
private fun DesktopAttachmentPickerDialog(onDismiss: () -> Unit, onSelect: (List<File>) -> Unit) {
    var directory by remember { mutableStateOf(File(System.getProperty("user.home"))) }
    var selectedPaths by remember { mutableStateOf(emptySet<String>()) }
    val entries = remember(directory) {
        directory.listFiles().orEmpty()
            .filter { it.isDirectory || isDesktopAttachmentSupported(it) }
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加附件") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            directory.parentFile?.let {
                                directory = it
                                selectedPaths = emptySet()
                            }
                        },
                        enabled = directory.parentFile != null
                    ) { Icon(Lucide.ChevronLeft, "上级目录") }
                    Text(directory.path, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    items(entries, key = { it.absolutePath }) { entry ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (entry.isDirectory) {
                                    directory = entry
                                    selectedPaths = emptySet()
                                } else {
                                    selectedPaths = if (entry.path in selectedPaths) {
                                        selectedPaths - entry.path
                                    } else {
                                        selectedPaths + entry.path
                                    }
                                }
                            }.padding(horizontal = 8.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (entry.isDirectory) Lucide.ChevronRight else Lucide.Paperclip, null, Modifier.size(17.dp))
                            Text(entry.name, Modifier.padding(start = 9.dp).weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (!entry.isDirectory && entry.path in selectedPaths) {
                                Icon(Lucide.Sparkles, "已选择", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(selectedPaths.map(::File)) }, enabled = selectedPaths.isNotEmpty()) {
                Text("添加（${selectedPaths.size}）")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun DesktopSaveFileDialog(
    title: String,
    suggestedName: String,
    requiredExtension: String,
    onDismiss: () -> Unit,
    onSave: (File) -> Unit
) {
    var directory by remember { mutableStateOf(File(System.getProperty("user.home"))) }
    var fileName by remember(suggestedName) { mutableStateOf(suggestedName) }
    val entries = remember(directory) {
        directory.listFiles().orEmpty()
            .filter { it.isDirectory || it.isFile }
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
    }
    val normalizedName = fileName.trim().let { name ->
        if (File(name).extension.equals(requiredExtension, ignoreCase = true)) name else "$name.$requiredExtension"
    }
    val validName = fileName.trim().isNotBlank() && File(normalizedName).name == normalizedName
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { directory.parentFile?.let { directory = it } },
                        enabled = directory.parentFile != null
                    ) { Icon(Lucide.ChevronLeft, "上级目录") }
                    Text(
                        directory.path,
                        Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                    items(entries, key = { it.absolutePath }) { entry ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (entry.isDirectory) directory = entry else fileName = entry.name
                            }.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (entry.isDirectory) Lucide.ChevronRight else Lucide.Paperclip, null, Modifier.size(17.dp))
                            Text(entry.name, Modifier.padding(start = 9.dp).weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("文件名") },
                    isError = fileName.isNotBlank() && !validName,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(File(directory, normalizedName)) }, enabled = validName) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ConversationSidebar(
    data: DesktopData,
    settingsSelected: Boolean,
    generatingConversationIds: Set<String>,
    onSelect: (String) -> Unit,
    onSelectFavorite: (String, String) -> Unit,
    onNew: () -> Unit,
    onDelete: (String) -> Unit,
    onPin: (String) -> Unit,
    onMoveToFolder: (String, String?) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onConversationSortChange: (DesktopConversationSort) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appIcon = rememberDesktopResourcePainter("icon.png")
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var assistantFilterId by remember { mutableStateOf<String?>(null) }
    var assistantFilterOpen by remember { mutableStateOf(false) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var tagFilterOpen by remember { mutableStateOf(false) }
    var folderFilterId by remember { mutableStateOf<String?>(null) }
    var renameFolder by remember { mutableStateOf<DesktopFolder?>(null) }
    var showFavorites by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val availableFolders = data.folders.filter { folder ->
        assistantFilterId == null || folder.assistantId == assistantFilterId
    }
    val conversations = data.filteredConversations(query, assistantFilterId).filter {
        (folderFilterId == null || it.folderId == folderFilterId) &&
            (tagFilter == null || data.assistantFor(it).tags.any { tag -> tag.equals(tagFilter, ignoreCase = true) })
    }.let { filtered ->
        if (folderFilterId == null) filtered.sortedByDescending { it.updatedAt } else filtered
    }
    val conversationRows = conversations.asConversationTree()
    val favorites = data.favoriteMessages(assistantFilterId).filter { (conversation, _) ->
        tagFilter == null || data.assistantFor(conversation).tags.any { tag ->
            tag.equals(tagFilter, ignoreCase = true)
        }
    }

    Surface(modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (appIcon != null) {
                    Image(appIcon, "RikkaHub", Modifier.size(38.dp))
                } else {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Icon(Lucide.Sparkles, null, Modifier.padding(9.dp).size(19.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text("RikkaHub", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("欢迎回来", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            DrawerAction(Lucide.Plus, "新建对话", onNew)
            DrawerAction(Lucide.Search, "搜索对话") { searching = !searching }
            DrawerAction(Lucide.Star, if (showFavorites) "返回对话" else "收藏消息") {
                showFavorites = !showFavorites
            }
            if (searching) {
                OutlinedTextField(
                    query,
                    { query = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    placeholder = { Text("搜索") },
                    leadingIcon = { Icon(Lucide.Search, null, Modifier.size(17.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            Box {
                val filterAssistant = data.assistants.firstOrNull { it.id == assistantFilterId }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .clickable { assistantFilterOpen = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Lucide.Bot, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        filterAssistant?.name ?: "全部助手",
                        Modifier.padding(start = 9.dp).weight(1f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Lucide.ChevronDown, null, Modifier.size(15.dp))
                }
                DropdownMenu(assistantFilterOpen, onDismissRequest = { assistantFilterOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("全部助手") },
                        onClick = {
                            assistantFilterId = null
                            assistantFilterOpen = false
                        }
                    )
                    data.assistants.forEach { assistant ->
                        DropdownMenuItem(
                            text = { Text(assistant.name) },
                            leadingIcon = {
                                if (assistant.id == assistantFilterId) {
                                    Icon(Lucide.Sparkles, null, Modifier.size(16.dp))
                                }
                            },
                            onClick = {
                                assistantFilterId = assistant.id
                                folderFilterId = data.folderFilterForAssistant(folderFilterId, assistant.id)
                                assistantFilterOpen = false
                            }
                        )
                    }
                }
            }
            Box {
                val tags = data.assistants.flatMap { it.tags }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { tagFilterOpen = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Lucide.Sparkles, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(tagFilter ?: "全部标签", Modifier.padding(start = 9.dp).weight(1f), fontSize = 13.sp, maxLines = 1)
                    Icon(Lucide.ChevronDown, null, Modifier.size(15.dp))
                }
                DropdownMenu(tagFilterOpen, onDismissRequest = { tagFilterOpen = false }) {
                    DropdownMenuItem(text = { Text("全部标签") }, onClick = { tagFilter = null; tagFilterOpen = false })
                    tags.forEach { tag ->
                        DropdownMenuItem(text = { Text(tag) }, onClick = { tagFilter = tag; tagFilterOpen = false })
                    }
                }
            }
            if (!showFavorites) {
                FolderCapsuleBar(
                    folders = availableFolders,
                    selectedFolderId = folderFilterId,
                    onSelect = { folderFilterId = it },
                    onCreate = { onCreateFolder(assistantFilterId ?: data.activeAssistant().id) },
                    onRename = { renameFolder = it },
                    onDelete = { folder ->
                        onDeleteFolder(folder.id)
                        if (folderFilterId == folder.id) folderFilterId = null
                    }
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 10.dp, top = 18.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (showFavorites) "收藏消息" else "对话",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!showFavorites && folderFilterId != null) {
                    Box {
                        TextButton(onClick = { sortMenuOpen = true }) {
                            Text(
                                if (data.preferences.conversationSort == DesktopConversationSort.RECENT) {
                                    "最近"
                                } else {
                                    "最常用"
                                },
                                fontSize = 12.sp
                            )
                            Icon(Lucide.ChevronDown, null, Modifier.padding(start = 3.dp).size(14.dp))
                        }
                        DropdownMenu(sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                            DesktopConversationSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (sort == DesktopConversationSort.RECENT) "按最近时间" else "按使用频率"
                                        )
                                    },
                                    onClick = {
                                        onConversationSortChange(sort)
                                        sortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                if (showFavorites) {
                    items(favorites, key = { (conversation, message) -> "${conversation.id}:${message.id}" }) {
                            (conversation, message) ->
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectFavorite(conversation.id, message.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(conversation.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(
                                message.content.ifBlank { message.reasoning }.ifBlank { "工具调用" },
                                Modifier.padding(top = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    var previousTimelineLabel: String? = null
                    conversationRows.forEach { item ->
                        val conversation = item.conversation
                        if (folderFilterId == null && item.branchDepth == 0) {
                            val timelineLabel = conversationTimelineLabel(conversation.updatedAt)
                            if (timelineLabel != previousTimelineLabel) {
                                item(key = "timeline:$timelineLabel") {
                                    ConversationTimelineHeader(timelineLabel)
                                }
                                previousTimelineLabel = timelineLabel
                            }
                        }
                        item(key = conversation.id) {
                        ConversationRow(
                            conversation = conversation,
                            branchDepth = item.branchDepth,
                            selected = !settingsSelected && conversation.id == data.selectedConversationId,
                            generating = conversation.id in generatingConversationIds,
                            onClick = { onSelect(conversation.id) },
                            onPin = { onPin(conversation.id) },
                            onDelete = { onDelete(conversation.id) },
                            folders = data.folders.filter { it.assistantId == data.assistantFor(conversation).id },
                            onMoveToFolder = { folderId -> onMoveToFolder(conversation.id, folderId) }
                        )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(
                        if (settingsSelected) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable(onClick = onSettings)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.Settings, "设置", Modifier.size(19.dp))
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text("设置", fontSize = 14.sp)
                }
            }
        }
    }
    renameFolder?.let { folder ->
        TextEditDialog(
            title = "重命名文件夹",
            initialValue = folder.name,
            singleLine = true,
            onDismiss = { renameFolder = null },
            onSave = { name ->
                if (name.isNotBlank()) onRenameFolder(folder.id, name)
                renameFolder = null
            }
        )
    }
}

@Composable
private fun FolderCapsuleBar(
    folders: List<DesktopFolder>,
    selectedFolderId: String?,
    onSelect: (String?) -> Unit,
    onCreate: () -> Unit,
    onRename: (DesktopFolder) -> Unit,
    onDelete: (DesktopFolder) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FolderCapsule(
                label = "聊天",
                selected = selectedFolderId == null,
                onClick = { onSelect(null) }
            )
        }
        items(folders, key = { it.id }) { folder ->
            var menuOpen by remember(folder.id) { mutableStateOf(false) }
            Box {
                FolderCapsule(
                    label = folder.name,
                    selected = selectedFolderId == folder.id,
                    icon = Lucide.Folder,
                    onClick = { onSelect(folder.id) },
                    onLongClick = { menuOpen = true }
                )
                DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        leadingIcon = { Icon(Lucide.Pencil, null, Modifier.size(17.dp)) },
                        onClick = {
                            menuOpen = false
                            onRename(folder)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { Icon(Lucide.Trash2, null, Modifier.size(17.dp)) },
                        onClick = {
                            menuOpen = false
                            onDelete(folder)
                        }
                    )
                }
            }
        }
        item {
            FolderCapsule(
                label = "新建",
                selected = false,
                icon = Lucide.Plus,
                onClick = onCreate
            )
        }
    }
}

@Composable
private fun FolderCapsule(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.clip(CircleShape).combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick ?: {}
        )
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let { Icon(it, null, Modifier.size(14.dp)) }
            Text(
                label,
                Modifier.padding(start = if (icon == null) 0.dp else 5.dp).widthIn(max = 120.dp),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConversationTimelineHeader(label: String) {
    Text(
        label,
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 12.dp, bottom = 3.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
    )
}

private fun conversationTimelineLabel(updatedAt: Long, today: LocalDate = LocalDate.now()): String {
    val date = Instant.ofEpochMilli(updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(date, today)
    return when (daysAgo) {
        0L -> "今天"
        1L -> "昨天"
        2L -> "前天"
        in 3L..30L -> "$daysAgo 天前"
        else -> "${date.monthValue} 月 ${date.dayOfMonth} 日"
    }
}

@Composable
private fun DrawerAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, Modifier.size(19.dp))
        Text(label, Modifier.padding(start = 11.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ConversationRow(
    conversation: DesktopConversation,
    branchDepth: Int,
    selected: Boolean,
    generating: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    folders: List<DesktopFolder>,
    onMoveToFolder: (String?) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val color = if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(color).clickable(onClick = onClick)
            .padding(start = 12.dp + (16.dp * branchDepth), end = 3.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (branchDepth > 0) {
            Icon(
                Lucide.GitFork,
                "对话分支",
                Modifier.padding(end = 7.dp).size(15.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(conversation.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
        if (generating) {
            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
        } else if (conversation.isPinned) {
            Icon(Lucide.Pin, "已置顶", Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(34.dp)) {
            Icon(Lucide.Ellipsis, "对话选项", Modifier.size(17.dp))
            }
            DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (conversation.isPinned) "取消置顶" else "置顶") },
                    leadingIcon = {
                        Icon(if (conversation.isPinned) Lucide.PinOff else Lucide.Pin, null, Modifier.size(18.dp))
                    },
                    onClick = {
                        menuOpen = false
                        onPin()
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("移至聊天") },
                    leadingIcon = { Icon(Lucide.Folder, null, Modifier.size(18.dp)) },
                    onClick = {
                        menuOpen = false
                        onMoveToFolder(null)
                    }
                )
                folders.forEach { folder ->
                    DropdownMenuItem(
                        text = { Text("移至 ${folder.name}") },
                        leadingIcon = { Icon(Lucide.Folder, null, Modifier.size(18.dp)) },
                        onClick = {
                            menuOpen = false
                            onMoveToFolder(folder.id)
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("删除") },
                    leadingIcon = { Icon(Lucide.Trash2, null, Modifier.size(18.dp)) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    }
                )
            }
        }
    }

}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun ChatPane(
    conversation: DesktopConversation,
    prompt: String,
    pendingAttachments: List<DesktopAttachment>,
    isGenerating: Boolean,
    errorMessage: String?,
    model: String,
    assistant: DesktopAssistantProfile,
    assistants: List<DesktopAssistantProfile>,
    preferences: DesktopPreferences,
    providers: List<DesktopProviderProfile>,
    folders: List<DesktopFolder>,
    selectedProviderId: String,
    webSearchEnabled: Boolean,
    jumpToMessageId: String?,
    conversationScrollPositions: MutableMap<String, Pair<Int, Int>>,
    onAskUserAnswer: (String, DesktopToolCall, String) -> Unit,
    showMenu: Boolean,
    onMenu: () -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit,
    onAssistantSettings: () -> Unit,
    onProviderModelSelect: (String, String) -> Unit,
    onReasoningEffortChange: (String) -> Unit,
    client: OpenAiClient,
    onAssistantSelect: (String) -> Unit,
    onToggleWebSearch: () -> Unit,
    onRename: () -> Unit,
    onExportConversation: () -> Unit,
    onMoveToFolder: (String?) -> Unit,
    onCreateFolder: () -> Unit,
    onCompress: () -> Unit,
    onGenerateTitle: () -> Unit,
    onShowStats: () -> Unit,
    onGenerateSuggestions: () -> Unit,
    onTogglePromptInjections: () -> Unit,
    onTranslateMessage: (Int) -> Unit,
    onRestoreBranch: (String) -> Unit,
    onDeleteBranch: (String) -> Unit,
    onEditSystemPrompt: () -> Unit,
    onPromptChange: (String) -> Unit,
    onAddAttachments: () -> Unit,
    onRemoveAttachment: (DesktopAttachment) -> Unit,
    onDismissError: () -> Unit,
    onCancel: () -> Unit,
    onEditMessage: (Int, String) -> Unit,
    onDeleteMessage: (Int) -> Unit,
    onToggleMessageFavorite: (Int) -> Unit,
    onForkAtMessage: (Int) -> Unit,
    onRegenerateMessage: (Int) -> Unit,
    onSelectMessageVariant: (Int, Int) -> Unit,
    onSend: () -> Unit,
    onAddWithoutResponse: () -> Unit
) {
    var conversationMenuOpen by remember { mutableStateOf(false) }
    var folderMenuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val hazeState = rememberHazeState()
    var composerHeightPx by remember { mutableStateOf(164) }
    val messageBottomPadding = with(LocalDensity.current) { composerHeightPx.toDp() + 16.dp }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    val providerName = providers.firstOrNull { it.id == assistant.providerId }?.name
        ?: providers.firstOrNull { it.id == selectedProviderId }?.name
        ?: "OpenAI"
    val lastContent = conversation.messages.lastOrNull()?.content
    val lastReasoning = conversation.messages.lastOrNull()?.reasoning
    val displayItems = remember(conversation.messages) {
        buildDesktopChatDisplayItems(conversation.messages)
    }
    var showMessageJumper by remember(conversation.id) { mutableStateOf(false) }
    var pointerOverMessageJumper by remember(conversation.id) { mutableStateOf(false) }
    var pointerOverMessageJumperEdge by remember(conversation.id) { mutableStateOf(false) }
    DisposableEffect(conversation.id) {
        onDispose {
            conversationScrollPositions[conversation.id] =
                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
    }
    LaunchedEffect(conversation.id) {
        val savedPosition = conversationScrollPositions[conversation.id] ?: (0 to 0)
        listState.scrollToItem(
            savedPosition.first.coerceIn(0, displayItems.size),
            savedPosition.second
        )
    }
    LaunchedEffect(conversation.messages.size, lastContent, lastReasoning, isGenerating) {
        if (preferences.enableAutoScroll && isGenerating && conversation.messages.isNotEmpty()) {
            val targetIndex = displayItems.size
            listState.animateScrollToItem(targetIndex)
        }
    }
    LaunchedEffect(jumpToMessageId, conversation.id) {
        val index = displayItems.indexOfFirst { item ->
            when (item) {
                is DesktopChatDisplayItem.Message -> item.message.id == jumpToMessageId
                is DesktopChatDisplayItem.AssistantTurn -> jumpToMessageId in item.messageIds
            }
        }
        if (index >= 0) {
            highlightedMessageId = jumpToMessageId
            listState.animateScrollToItem(index)
            delay(1_800)
            if (highlightedMessageId == jumpToMessageId) highlightedMessageId = null
        }
    }
    LaunchedEffect(listState.isScrollInProgress, pointerOverMessageJumper, pointerOverMessageJumperEdge) {
        if (listState.isScrollInProgress) {
            showMessageJumper = true
        } else if (showMessageJumper && !pointerOverMessageJumper && !pointerOverMessageJumperEdge) {
            delay(1_500)
            if (!pointerOverMessageJumper && !pointerOverMessageJumperEdge) {
                showMessageJumper = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showMenu) {
                IconButton(onClick = onMenu) { Icon(Lucide.Menu, "打开对话列表") }
            }
            Surface(
                onClick = onRename,
                color = Color.Transparent,
                modifier = Modifier.padding(start = if (showMenu) 2.dp else 8.dp).weight(1f)
            ) {
                Column {
                    Text(conversation.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${assistant.name} · $model",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Box {
                IconButton(onClick = { conversationMenuOpen = true }) {
                    Icon(Lucide.Ellipsis, "对话选项")
                }
                DropdownMenu(conversationMenuOpen, onDismissRequest = { conversationMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        leadingIcon = { Icon(Lucide.Pencil, null, Modifier.size(18.dp)) },
                        onClick = {
                            conversationMenuOpen = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("导出 Markdown") },
                        leadingIcon = { Icon(Lucide.Download, null, Modifier.size(18.dp)) },
                        onClick = {
                            conversationMenuOpen = false
                            onExportConversation()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("生成标题") },
                        leadingIcon = { Icon(Lucide.Sparkles, null, Modifier.size(18.dp)) },
                        enabled = !isGenerating && conversation.messages.isNotEmpty(),
                        onClick = {
                            conversationMenuOpen = false
                            onGenerateTitle()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("对话统计") },
                        enabled = conversation.messages.isNotEmpty(),
                        onClick = {
                            conversationMenuOpen = false
                            onShowStats()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("生成回复建议") },
                        leadingIcon = { Icon(Lucide.Sparkles, null, Modifier.size(18.dp)) },
                        enabled = !isGenerating && conversation.messages.isNotEmpty(),
                        onClick = {
                            conversationMenuOpen = false
                            onGenerateSuggestions()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("压缩历史") },
                        leadingIcon = { Icon(Lucide.Sparkles, null, Modifier.size(18.dp)) },
                        enabled = !isGenerating && conversation.messages.size > 1,
                        onClick = {
                            conversationMenuOpen = false
                            onCompress()
                        }
                    )
                    if (assistant.allowConversationSystemPrompt) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (conversation.systemPrompt.isBlank()) {
                                        "设置系统提示词"
                                    } else {
                                        "编辑系统提示词"
                                    }
                                )
                            },
                            leadingIcon = { Icon(Lucide.Settings, null, Modifier.size(18.dp)) },
                            onClick = {
                                conversationMenuOpen = false
                                onEditSystemPrompt()
                            }
                        )
                    }
                    if (assistant.allowConversationPromptInjection) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (conversation.usesPromptInjections(assistant)) "禁用本对话世界书" else "启用本对话世界书"
                                )
                            },
                            leadingIcon = { Icon(Lucide.Sparkles, null, Modifier.size(18.dp)) },
                            enabled = !isGenerating,
                            onClick = {
                                conversationMenuOpen = false
                                onTogglePromptInjections()
                            }
                        )
                    }
                    if (conversation.branches.isNotEmpty()) {
                        HorizontalDivider()
                        conversation.branches.forEachIndexed { index, branch ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("恢复历史快照 ${index + 1}: ${branch.name}")
                                        Text(
                                            "${branch.messages.size} 条消息",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Lucide.RotateCcw, null, Modifier.size(18.dp)) },
                                trailingIcon = {
                                    IconButton(onClick = { onDeleteBranch(branch.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Lucide.Trash2, "删除历史快照", Modifier.size(15.dp))
                                    }
                                },
                                onClick = {
                                    conversationMenuOpen = false
                                    onRestoreBranch(branch.id)
                                }
                            )
                        }
                    }
                }
            }
            Box {
                IconButton(onClick = { folderMenuOpen = true }) {
                    Icon(Lucide.Folder, "移动到文件夹")
                }
                DropdownMenu(folderMenuOpen, onDismissRequest = { folderMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("未分类") },
                                     onClick = {
                                         folderMenuOpen = false
                                         onMoveToFolder(null)
                                     }
                    )
                    folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder.name) },
                                         onClick = {
                                             folderMenuOpen = false
                                             onMoveToFolder(folder.id)
                                         }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("新建文件夹") },
                                     leadingIcon = { Icon(Lucide.Plus, null, Modifier.size(18.dp)) },
                                     onClick = {
                                         folderMenuOpen = false
                                         onCreateFolder()
                                     }
                    )
                }
            }
            IconButton(onClick = onNew) { Icon(Lucide.Plus, "新建对话") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier.fillMaxSize()
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.TopCenter
            ) {
                if (conversation.messages.isEmpty()) {
                    EmptyConversation(model, assistant.quickMessages, onPromptChange)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 22.dp, bottom = messageBottomPadding),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        itemsIndexed(displayItems, key = { _, item -> item.key }) { _, item ->
                            val renderedItem = when (item) {
                                is DesktopChatDisplayItem.Message -> RenderedChatItem(
                                    item.messageIndex,
                                    item.message,
                                    emptyList(),
                                    false,
                                    item.message.id == highlightedMessageId,
                                )
                                is DesktopChatDisplayItem.AssistantTurn -> RenderedChatItem(
                                    item.messageIndex,
                                    item.message,
                                    item.steps,
                                    item.timelineAfterContent,
                                    highlightedMessageId in item.messageIds,
                                )
                            }
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Box(Modifier.widthIn(max = 920.dp).padding(horizontal = 28.dp)) {
                                    SoftMessageReveal(renderedItem.message.id) {
                                        MessageBlock(
                                            message = renderedItem.message,
                                            model = model,
                                            providerName = providerName,
                                            assistant = assistant,
                                            preferences = preferences,
                                            executionSteps = renderedItem.executionSteps,
                                            timelineAfterContent = renderedItem.timelineAfterContent,
                                            generating = isGenerating && renderedItem.messageIndex == conversation.messages.lastIndex,
                                            actionsEnabled = !isGenerating,
                                            onEdit = { onEditMessage(renderedItem.messageIndex, renderedItem.message.content) },
                                            onDelete = { onDeleteMessage(renderedItem.messageIndex) },
                                            onToggleFavorite = { onToggleMessageFavorite(renderedItem.messageIndex) },
                                            onFork = { onForkAtMessage(renderedItem.messageIndex) },
                                            onTranslate = { onTranslateMessage(renderedItem.messageIndex) },
                                            highlighted = renderedItem.highlighted,
                                            onRegenerate = { onRegenerateMessage(renderedItem.messageIndex) },
                                            onSelectVariant = { variantIndex ->
                                                onSelectMessageVariant(renderedItem.messageIndex, variantIndex)
                                            },
                                            onAskUserAnswer = { toolCall, answer ->
                                                onAskUserAnswer(conversation.id, toolCall, answer)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                            )
                        }
                    }
                    ChatContentEdgeFade()
                }
            }
            if (conversation.messages.isNotEmpty() && preferences.showMessageJumper) {
                Box(
                    Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(16.dp)
                        .onPointerEvent(PointerEventType.Enter) {
                            pointerOverMessageJumperEdge = true
                            showMessageJumper = true
                        }
                        .onPointerEvent(PointerEventType.Exit) { pointerOverMessageJumperEdge = false }
                )
                DesktopMessageJumper(
                    visible = showMessageJumper && !listState.isScrollInProgress,
                    onLeft = preferences.messageJumperOnLeft,
                    state = listState,
                    messageCount = displayItems.size,
                    onPointerOverChange = { pointerOverMessageJumper = it }
                )
            }
            Composer(
                modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { composerHeightPx = it.height },
                prompt = prompt,
                pendingAttachments = pendingAttachments,
                model = model,
                isGenerating = isGenerating,
                onPromptChange = onPromptChange,
                onAddAttachments = onAddAttachments,
                onRemoveAttachment = onRemoveAttachment,
                onSend = onSend,
                onAddWithoutResponse = onAddWithoutResponse,
                onCancel = onCancel,
                onSettings = onSettings,
                onAssistantSettings = onAssistantSettings,
                sendOnEnter = preferences.sendOnEnter,
                providers = providers,
                selectedProviderId = selectedProviderId,
                webSearchEnabled = webSearchEnabled,
                onProviderModelSelect = onProviderModelSelect,
                onReasoningEffortChange = onReasoningEffortChange,
                client = client,
                assistant = assistant,
                assistants = assistants,
                onAssistantSelect = onAssistantSelect,
                onToggleWebSearch = onToggleWebSearch,
                onQuickMessageSelect = onPromptChange,
                suggestions = conversation.suggestions,
                onSuggestionSelect = onPromptChange,
                hazeState = hazeState
            )
        }

        errorMessage?.let {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(it, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                    TextButton(onClick = onDismissError) { Text("关闭") }
                }
            }
            DropdownMenu(folderMenuOpen, onDismissRequest = { folderMenuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("聊天") },
                    onClick = {
                        folderMenuOpen = false
                        onMoveToFolder(null)
                    }
                )
                folders.forEach { folder ->
                    DropdownMenuItem(
                        text = { Text(folder.name) },
                        onClick = {
                            folderMenuOpen = false
                            onMoveToFolder(folder.id)
                        }
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun BoxScope.ChatContentEdgeFade() {
    val background = MaterialTheme.colorScheme.background
    Box(
        Modifier.align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(36.dp)
            .background(Brush.verticalGradient(listOf(background, background.copy(alpha = 0f))))
    )
    Box(
        Modifier.align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(56.dp)
            .background(Brush.verticalGradient(listOf(background.copy(alpha = 0f), background)))
    )
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun BoxScope.DesktopMessageJumper(
    visible: Boolean,
    onLeft: Boolean,
    state: LazyListState,
    messageCount: Int,
    onPointerOverChange: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val alignment = if (onLeft) Alignment.CenterStart else Alignment.CenterEnd
    val color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
    val horizontalOffset: (Int) -> Int = { width -> if (onLeft) -width else width }
    val currentMessage = state.firstVisibleItemIndex.coerceIn(0, messageCount.coerceAtLeast(1) - 1) + 1

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(alignment)
            .padding(horizontal = 12.dp)
            .onPointerEvent(PointerEventType.Enter) { onPointerOverChange(true) }
            .onPointerEvent(PointerEventType.Exit) { onPointerOverChange(false) },
        enter = fadeIn(tween(180)) + slideInHorizontally(tween(180), initialOffsetX = horizontalOffset),
        exit = fadeOut(tween(220)) + slideOutHorizontally(tween(220), targetOffsetX = horizontalOffset)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MessageJumperButton(Lucide.ArrowUpToLine, "跳转到顶部", color) {
                scope.launch { state.animateScrollToItem(0) }
            }
            MessageJumperButton(Lucide.ArrowUp, "上一条消息", color) {
                scope.launch { state.animateScrollToItem((state.firstVisibleItemIndex - 1).coerceAtLeast(0)) }
            }
            Surface(
                modifier = Modifier.size(width = 40.dp, height = 28.dp),
                shape = RoundedCornerShape(6.dp),
                color = color,
                tonalElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$currentMessage/$messageCount", fontSize = 10.sp)
                }
            }
            MessageJumperButton(Lucide.ArrowDown, "下一条消息", color) {
                scope.launch {
                    state.animateScrollToItem((state.firstVisibleItemIndex + 1).coerceAtMost(messageCount))
                }
            }
            MessageJumperButton(Lucide.ArrowDownToLine, "跳转到底部", color) {
                scope.launch { state.animateScrollToItem(messageCount) }
            }
        }
    }
}

@Composable
private fun MessageJumperButton(icon: ImageVector, description: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = color,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, description, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SoftMessageReveal(messageId: String, content: @Composable () -> Unit) {
    var revealed by remember(messageId) { mutableStateOf(false) }
    LaunchedEffect(messageId) { revealed = true }
    val progress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "messageReveal"
    )
    Box(
        Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 10f
        }.blur(
            radius = ((1f - progress) * 7f).dp,
            edgeTreatment = BlurredEdgeTreatment.Unbounded
        )
    ) {
        content()
    }
}

@Composable
private fun EmptyConversation(
    model: String,
    quickMessages: List<DesktopQuickMessage>,
    onQuickMessageSelect: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(bottom = 80.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(Lucide.Sparkles, null, Modifier.padding(16.dp).size(28.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text("有什么可以帮你的？", fontSize = 24.sp, fontWeight = FontWeight.Medium)
        Text(model, Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        if (quickMessages.isNotEmpty()) {
            FlowRow(
                Modifier.padding(top = 20.dp).widthIn(max = 620.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickMessages.forEach { message ->
                    OutlinedButton(onClick = { onQuickMessageSelect(message.content) }) {
                        Text(message.title.ifBlank { message.content.take(32) }, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun MessageBlock(
    message: ChatMessage,
    model: String,
    providerName: String,
    assistant: DesktopAssistantProfile,
    preferences: DesktopPreferences,
    executionSteps: List<DesktopExecutionStep>,
    timelineAfterContent: Boolean,
    generating: Boolean,
    actionsEnabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onFork: () -> Unit,
    onTranslate: () -> Unit,
    highlighted: Boolean,
    onRegenerate: () -> Unit,
    onSelectVariant: (Int) -> Unit,
    onAskUserAnswer: (DesktopToolCall, String) -> Unit
) {
    val isUser = message.role == "user"
    val configuredUserNickname = preferences.userNickname.trim()
    val userNickname = configuredUserNickname.ifBlank { "你" }
    val hasVisibleExecutionSteps = executionSteps.any {
        it !is DesktopExecutionStep.Reasoning || preferences.showReasoning
    }
    val displayContent = assistant.applyRegexRules(message.content, message.role, visualOnly = true)
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    var copyVersion by remember(message.id) { mutableStateOf(0) }
    var copied by remember(message.id) { mutableStateOf(false) }
    LaunchedEffect(copyVersion) {
        if (copyVersion > 0) {
            copied = true
            delay(1_000)
            copied = false
        }
    }
    val markdownOptions = MarkdownRenderOptions(
        fontScale = preferences.fontScale,
        codeBlockAutoWrap = preferences.codeBlockAutoWrap,
        enableChineseTypography = preferences.enableChineseTypography
    )
    Column(
        Modifier.fillMaxWidth()
            .background(
                if (highlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(if (highlighted) 8.dp else 0.dp)
            .animateContentSize(tween(180, easing = FastOutSlowInEasing)),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (isUser || preferences.showModelIcon || preferences.showModelName || preferences.showMessageTimestamp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isUser) {
                    if (preferences.showModelIcon) {
                        DesktopProviderIcon(providerName)
                    }
                    if (preferences.showModelName) {
                        Text(
                            message.modelId ?: model,
                            Modifier.padding(start = if (preferences.showModelIcon) 9.dp else 0.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (preferences.showMessageTimestamp) {
                        MessageTimestamp(message.createdAt)
                    }
                } else {
                    if (preferences.showMessageTimestamp) {
                        MessageTimestamp(message.createdAt)
                    }
                    Text(userNickname, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    if (preferences.showUserAvatar) {
                        Surface(
                            modifier = Modifier.padding(start = 8.dp).size(28.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (configuredUserNickname.isBlank()) {
                                    Icon(Lucide.UserRound, "用户头像", Modifier.size(16.dp))
                                } else {
                                    Text(
                                        userNickname.take(1),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (isUser) {
            Card(
                modifier = Modifier.widthIn(max = 650.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                MarkdownContent(
                    displayContent,
                    Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    markdownOptions
                )
            }
        } else {
            val assistantContent: @Composable () -> Unit = {
                if (preferences.showAssistantBubble) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        MarkdownContent(
                            displayContent,
                            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            markdownOptions
                        )
                    }
                } else {
                    MarkdownContent(
                        displayContent,
                        modifier = Modifier.fillMaxWidth(),
                        options = markdownOptions
                    )
                }
            }
            if (timelineAfterContent && displayContent.isNotBlank()) {
                assistantContent()
            }
            if (hasVisibleExecutionSteps) {
                DesktopExecutionTimeline(
                    steps = executionSteps,
                    generating = generating,
                    autoCollapse = preferences.autoCollapseReasoning,
                    showReasoning = preferences.showReasoning,
                    markdownOptions = markdownOptions,
                    onAskUserAnswer = onAskUserAnswer,
                )
            }
            if (
                message.content.isEmpty() &&
                !hasVisibleExecutionSteps &&
                generating
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                    Text("思考中...", Modifier.padding(start = 9.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (!timelineAfterContent && displayContent.isNotBlank()) {
                assistantContent()
            }
        }
        if (message.attachments.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.attachments.forEach { attachment ->
                    AttachmentPreview(attachment)
                }
            }
        }
        if (message.translation.isNotBlank()) {
            TranslationBlock(
                messageId = message.id,
                translation = message.translation,
                targetLanguage = message.translationTargetLanguage,
                markdownOptions = markdownOptions
            )
        }
        if (!isUser && message.citations.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("来源", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    message.citations.forEachIndexed { index, citation ->
                        Surface(
                            onClick = {
                                if (isSafeExternalUrl(citation.url) && Desktop.isDesktopSupported()) {
                                    runCatching { Desktop.getDesktop().browse(URI(citation.url)) }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    citation.title.ifBlank { "来源 ${index + 1}" },
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 220.dp)
                                )
                                Icon(Lucide.ExternalLink, null, Modifier.padding(start = 6.dp).size(13.dp))
                            }
                        }
                    }
                }
            }
        }
        if (!generating && !isUser && (
                message.promptTokens != null || message.completionTokens != null || message.cachedTokens != null
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                message.promptTokens?.let { tokens ->
                    Icon(
                        Lucide.Upload,
                        "输入 Token",
                        Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$tokens tokens",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                message.completionTokens?.let { tokens ->
                    Icon(
                        Lucide.Download,
                        "输出 Token",
                        Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$tokens tokens",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                message.cachedTokens?.let { tokens ->
                    Icon(
                        Lucide.Database,
                        "缓存 Token",
                        Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$tokens cached",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val variants = message.availableVariants()
            val currentVariantIndex = message.selectedVariantIndex.coerceIn(variants.indices)
            if (variants.size > 1) {
                MessageAction(
                    Lucide.ChevronLeft,
                    "上一个版本",
                    enabled = actionsEnabled && currentVariantIndex > 0
                ) { onSelectVariant(currentVariantIndex - 1) }
                Text(
                    "${currentVariantIndex + 1}/${variants.size}",
                    Modifier.padding(horizontal = 3.dp).align(Alignment.CenterVertically),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                MessageAction(
                    Lucide.ChevronRight,
                    "下一个版本",
                    enabled = actionsEnabled && currentVariantIndex < variants.lastIndex
                ) { onSelectVariant(currentVariantIndex + 1) }
            }
            MessageAction(if (copied) Lucide.Check else Lucide.Copy, "复制", enabled = displayContent.isNotEmpty()) {
                clipboardScope.launch {
                    clipboard.setClipEntry(ClipEntry(StringSelection(displayContent)))
                    copyVersion++
                }
            }
            MessageAction(Lucide.GitFork, "从此处分支", enabled = actionsEnabled, onClick = onFork)
            if (!isUser) {
                MessageAction(Lucide.RotateCcw, "重新生成", enabled = actionsEnabled, onClick = onRegenerate)
            }
            MessageAction(Lucide.Languages, "翻译", enabled = actionsEnabled && displayContent.isNotBlank(), onClick = onTranslate)
            MessageAction(
                if (message.isFavorite) FilledStar else Lucide.Star,
                if (message.isFavorite) "取消收藏" else "收藏",
                enabled = actionsEnabled,
                onClick = onToggleFavorite
            )
            if (isUser) {
                MessageAction(Lucide.Pencil, "编辑", enabled = actionsEnabled, onClick = onEdit)
            }
            MessageAction(Lucide.Trash2, "删除", enabled = actionsEnabled, onClick = onDelete)
        }
    }
}

@Composable
private fun AttachmentPreview(attachment: DesktopAttachment) {
    val bitmap = remember(attachment.data, attachment.kind) {
        if (attachment.kind != DesktopAttachmentKind.IMAGE) null else runCatching {
            org.jetbrains.skia.Image.makeFromEncoded(Base64.getDecoder().decode(attachment.data))
                .toComposeImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Image(
                bitmap = bitmap,
                contentDescription = attachment.name,
                modifier = Modifier.widthIn(max = 360.dp).heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            Text(attachment.name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    } else {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.Paperclip, null, Modifier.size(14.dp))
                Text(
                    if (attachment.kind == DesktopAttachmentKind.AUDIO) "音频 · ${attachment.name}" else attachment.name,
                    Modifier.padding(start = 6.dp),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun MessageTimestamp(createdAt: Long) {
    Text(
        MessageTimeFormatter.format(Instant.ofEpochMilli(createdAt)),
        Modifier.padding(horizontal = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp
    )
}

@Composable
private fun DesktopExecutionTimeline(
    steps: List<DesktopExecutionStep>,
    generating: Boolean,
    autoCollapse: Boolean,
    showReasoning: Boolean,
    markdownOptions: MarkdownRenderOptions,
    onAskUserAnswer: (DesktopToolCall, String) -> Unit,
) {
    val visibleSteps = steps.filter { it !is DesktopExecutionStep.Reasoning || showReasoning }
    if (visibleSteps.isEmpty()) return

    val hasPendingQuestion = visibleSteps.any {
        it is DesktopExecutionStep.ToolCall && it.call.name == DesktopAskUserToolName && it.result == null
    }
    var expanded by remember(steps, generating, autoCollapse, hasPendingQuestion) {
        mutableStateOf(generating || !autoCollapse || hasPendingQuestion)
    }
    val canCollapse = visibleSteps.size > 2
    val renderedSteps = if (expanded || !canCollapse) visibleSteps else visibleSteps.takeLast(2)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (canCollapse) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (expanded) Lucide.ChevronDown else Lucide.ChevronRight,
                        if (expanded) "收起执行过程" else "展开执行过程",
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (expanded) "执行过程" else "展开 ${visibleSteps.size - renderedSteps.size} 个步骤",
                        Modifier.padding(start = 7.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            renderedSteps.forEachIndexed { index, step ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                }
                DesktopExecutionTimelineStep(
                    step = step,
                    generating = generating,
                    markdownOptions = markdownOptions,
                    onAskUserAnswer = onAskUserAnswer,
                )
            }
        }
    }
}

@Composable
private fun DesktopExecutionTimelineStep(
    step: DesktopExecutionStep,
    generating: Boolean,
    markdownOptions: MarkdownRenderOptions,
    onAskUserAnswer: (DesktopToolCall, String) -> Unit,
) {
    when (step) {
        is DesktopExecutionStep.Reasoning -> {
            var expanded by remember(step.message.id) { mutableStateOf(generating) }
            val message = step.message
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.Lightbulb, "思考过程", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    message.reasoningDurationMillis?.let { "思考了 ${formatReasoningDuration(it)} 秒" } ?: "思考过程",
                    Modifier.padding(start = 7.dp).weight(1f),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(if (expanded) Lucide.ChevronDown else Lucide.ChevronRight, null, Modifier.size(15.dp))
            }
            if (expanded) {
                MarkdownContent(
                    message.reasoning,
                    Modifier.fillMaxWidth().padding(start = 23.dp, bottom = 8.dp),
                    markdownOptions
                )
            }
        }

        is DesktopExecutionStep.ToolCall -> {
            if (step.call.name == DesktopAskUserToolName && step.result == null) {
                AskUserToolStep(step.call, onAskUserAnswer)
            } else {
                DesktopToolCallTimelineStep(
                    toolCall = step.call,
                    result = step.result,
                    generating = generating && step.result == null,
                    markdownOptions = markdownOptions,
                )
            }
        }

        is DesktopExecutionStep.ToolResult -> DesktopToolResultTimelineStep(step.message.content, markdownOptions)
    }
}

@Composable
private fun DesktopToolCallTimelineStep(
    toolCall: DesktopToolCall,
    result: ChatMessage?,
    generating: Boolean,
    markdownOptions: MarkdownRenderOptions,
) {
    var expanded by remember(toolCall.id) { mutableStateOf(false) }
    val input = toolCall.arguments.takeIf { it.isNotBlank() && it != "{}" }
    val output = result?.content?.takeIf { it.isNotBlank() }
    val hasDetails = input != null || output != null
    val status = if (generating && result == null) "正在调用" else "调用完成"
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = hasDetails) { expanded = !expanded }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Lucide.Wrench, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            "${toolCall.displayName()} · $status",
            Modifier.padding(start = 7.dp).weight(1f),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (hasDetails) {
            Icon(if (expanded) Lucide.ChevronDown else Lucide.ChevronRight, null, Modifier.size(15.dp))
        }
    }
    if (expanded && hasDetails) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 23.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            input?.let { value ->
                DesktopToolDetail("输入") {
                    Text(value, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
            output?.let { value ->
                DesktopToolDetail("输出") {
                    MarkdownContent(value, Modifier.fillMaxWidth(), markdownOptions)
                }
            }
        }
    }
}

@Composable
private fun DesktopToolDetail(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Box(Modifier.fillMaxWidth().padding(9.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun DesktopToolResultTimelineStep(content: String, markdownOptions: MarkdownRenderOptions) {
    var expanded by remember(content) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Lucide.Wrench, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            "工具结果",
            Modifier.padding(start = 7.dp).weight(1f),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(if (expanded) Lucide.ChevronDown else Lucide.ChevronRight, null, Modifier.size(15.dp))
    }
    if (expanded && content.isNotBlank()) {
        Box(Modifier.fillMaxWidth().padding(start = 23.dp, bottom = 8.dp)) {
            DesktopToolDetail("输出") {
                MarkdownContent(content, Modifier.fillMaxWidth(), markdownOptions)
            }
        }
    }
}

private data class DesktopAskUserQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val selectionType: String
)

@Composable
private fun AskUserToolStep(toolCall: DesktopToolCall, onSubmit: (DesktopToolCall, String) -> Unit) {
    val questions = remember(toolCall.arguments) {
        runCatching {
            Json.parseToJsonElement(toolCall.arguments).jsonObject["questions"]?.jsonArray.orEmpty()
                .mapNotNull { element ->
                    val question = element.jsonObject
                    val id = question["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val text = question["question"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    if (id.isBlank() || text.isBlank()) return@mapNotNull null
                    DesktopAskUserQuestion(
                        id = id,
                        question = text,
                        options = question["options"]?.jsonArray.orEmpty()
                            .mapNotNull { it.jsonPrimitive.contentOrNull },
                        selectionType = question["selection_type"]?.jsonPrimitive?.contentOrNull ?: "text"
                    )
                }
        }.getOrDefault(emptyList())
    }
    val answers = remember(toolCall.id) { mutableStateMapOf<String, String>() }
    val multiAnswers = remember(toolCall.id) { mutableStateMapOf<String, Set<String>>() }

    Surface(
        modifier = Modifier.widthIn(max = 620.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("需要你的回答", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            questions.forEach { question ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(question.question, fontSize = 13.sp)
                    if (question.options.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            question.options.forEach { option ->
                                val interactionSource = remember(question.id, option) { MutableInteractionSource() }
                                val hovered by interactionSource.collectIsHoveredAsState()
                                val selected = when (question.selectionType) {
                                    "multi" -> multiAnswers[question.id]?.contains(option) == true
                                    else -> answers[question.id] == option
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (question.selectionType == "multi") {
                                            val updated = multiAnswers[question.id].orEmpty().toMutableSet()
                                            if (!updated.add(option)) updated.remove(option)
                                            multiAnswers[question.id] = updated
                                        } else {
                                            answers[question.id] = option
                                        }
                                    },
                                    interactionSource = interactionSource,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = if (hovered) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                        labelColor = if (hovered) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        selectedBorderColor = MaterialTheme.colorScheme.primary
                                    ),
                                    label = { Text(option, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                    if (question.selectionType == "text") {
                        OutlinedTextField(
                            value = answers[question.id].orEmpty(),
                            onValueChange = { answers[question.id] = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 1,
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Button(
                onClick = {
                    val answer = buildJsonObject {
                        put("answers", buildJsonObject {
                            questions.forEach { question ->
                                val value = if (question.selectionType == "multi") {
                                    multiAnswers[question.id].orEmpty().joinToString(", ")
                                } else {
                                    answers[question.id].orEmpty()
                                }
                                put(question.id, JsonPrimitive(value))
                            }
                        })
                    }
                    onSubmit(toolCall, answer.toString())
                },
                enabled = questions.isNotEmpty() && questions.all { question ->
                    if (question.selectionType == "multi") {
                        multiAnswers[question.id].orEmpty().isNotEmpty()
                    } else {
                        answers[question.id].orEmpty().isNotBlank()
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("提交")
            }
        }
    }
}

private fun formatReasoningDuration(durationMillis: Long): String =
    String.format(java.util.Locale.getDefault(), "%.1f", durationMillis.coerceAtLeast(0) / 1_000.0)

private fun DesktopToolCall.displayName(): String = when (name) {
    DesktopWebSearchToolName -> "搜索网页"
    DesktopCurrentTimeToolName -> "获取时间"
    DesktopMemoryToolName -> "管理记忆"
    DesktopAgentListFilesToolName -> "列出文件"
    DesktopAgentSearchFilesToolName -> "搜索文件"
    DesktopAgentReadFileToolName -> "读取文件"
    DesktopAgentWriteFileToolName -> "写入文件"
    DesktopAgentEditFileToolName -> "编辑文件"
    DesktopAgentShellToolName -> "执行命令"
    DesktopUseSkillToolName -> "使用技能"
    else -> name
        .substringAfterLast("__")
        .removePrefix("agent_")
        .removePrefix("tool_")
        .replace('_', ' ')
}

@Composable
private fun TranslationBlock(
    messageId: String,
    translation: String,
    targetLanguage: String,
    markdownOptions: MarkdownRenderOptions
) {
    var expanded by remember(messageId) { mutableStateOf(true) }
    val title = "翻译${targetLanguage.takeIf { it.isNotBlank() }?.let { "（$it）" }.orEmpty()}"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (expanded) Lucide.ChevronDown else Lucide.ChevronRight,
                    if (expanded) "收起$title" else "展开$title",
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    title,
                    Modifier.padding(start = 7.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f))
                MarkdownContent(
                    translation,
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    markdownOptions
                )
            }
        }
    }
}

@Composable
private fun MessageAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(32.dp)) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
            label = "messageActionIcon"
        ) { currentIcon ->
            Icon(currentIcon, description, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ModelPickerDialog(
    providers: List<DesktopProviderProfile>,
    selectedProviderId: String,
    selectedModel: String,
    reasoningEffort: String,
    client: OpenAiClient,
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit,
    onReasoningEffortChange: (String) -> Unit,
    onSettings: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var balance by remember(selectedProviderId) { mutableStateOf<String?>(null) }
    var loadingBalance by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val activeProvider = providers.firstOrNull { it.id == selectedProviderId }
    val entries = providers.flatMap { provider ->
        (provider.discoveredModels + provider.config.model).filter { it.isNotBlank() }.distinct().map { provider to it }
    }.filter { (_, model) -> model.contains(query.trim(), ignoreCase = true) }
    LaunchedEffect(activeProvider?.id, activeProvider?.config?.balanceOptions) {
        if (activeProvider?.config?.balanceOptions?.enabled == true) {
            loadingBalance = true
            balance = runCatching { "余额：${client.getCachedBalance(activeProvider.config)}" }
                .getOrElse { "余额查询失败：${it.message ?: "未知错误"}" }
            loadingBalance = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择模型") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("筛选模型") },
                    singleLine = true
                )
                if (activeProvider?.config?.balanceOptions?.enabled == true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(balance ?: "余额未查询", Modifier.weight(1f), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(
                            enabled = !loadingBalance,
                            onClick = {
                                loadingBalance = true
                                scope.launch {
                                    balance = runCatching { "余额：${client.getCachedBalance(activeProvider.config, forceRefresh = true)}" }
                                        .getOrElse { "余额查询失败：${it.message ?: "未知错误"}" }
                                    loadingBalance = false
                                }
                            }
                        ) {
                            if (loadingBalance) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Icon(Lucide.RotateCcw, "刷新余额", Modifier.size(17.dp))
                        }
                    }
                }
                Text("推理强度", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("", "low", "medium", "high").forEach { effort ->
                        FilterChip(
                            selected = reasoningEffort == effort,
                            onClick = { onReasoningEffortChange(effort) },
                            label = { Text(effort.ifBlank { "默认" }) }
                        )
                    }
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(entries, key = { "${it.first.id}:${it.second}" }) { (provider, availableModel) ->
                        val selected = provider.id == selectedProviderId && availableModel == selectedModel
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(provider.id, availableModel) },
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                        ) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(availableModel, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    if (selected) Icon(Lucide.Sparkles, "当前模型", Modifier.size(16.dp), MaterialTheme.colorScheme.primary)
                                }
                                Text(provider.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                FlowRow(
                                    Modifier.padding(top = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    modelCapabilityLabels(availableModel).forEach { label ->
                                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                            Text(label, Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSettings) { Text("管理服务商") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

private fun modelCapabilityLabels(model: String): List<String> {
    val normalized = model.lowercase()
    return buildList {
        add("文本")
        if (listOf("gpt-4o", "gpt-4.1", "gemini", "claude", "qwen-vl", "vision").any(normalized::contains)) add("视觉")
        if (listOf("o1", "o3", "r1", "reasoner", "thinking", "deepseek-r").any(normalized::contains)) add("推理")
        if (!listOf("embedding", "image", "tts", "whisper").any(normalized::contains)) add("工具")
    }
}

@Composable
private fun Composer(
    modifier: Modifier,
    prompt: String,
    pendingAttachments: List<DesktopAttachment>,
    model: String,
    isGenerating: Boolean,
    onPromptChange: (String) -> Unit,
    onAddAttachments: () -> Unit,
    onRemoveAttachment: (DesktopAttachment) -> Unit,
    onSend: () -> Unit,
    onAddWithoutResponse: () -> Unit,
    onCancel: () -> Unit,
    onSettings: () -> Unit,
    onAssistantSettings: () -> Unit,
    sendOnEnter: Boolean,
    providers: List<DesktopProviderProfile>,
    selectedProviderId: String,
    webSearchEnabled: Boolean,
    onProviderModelSelect: (String, String) -> Unit,
    onReasoningEffortChange: (String) -> Unit,
    client: OpenAiClient,
    assistant: DesktopAssistantProfile,
    assistants: List<DesktopAssistantProfile>,
    onAssistantSelect: (String) -> Unit,
    onToggleWebSearch: () -> Unit,
    onQuickMessageSelect: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionSelect: (String) -> Unit,
    hazeState: HazeState
) {
    var modelMenuOpen by remember { mutableStateOf(false) }
    var assistantMenuOpen by remember { mutableStateOf(false) }
    var quickMessageMenuOpen by remember { mutableStateOf(false) }
    var fullScreenEditorOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val inputFocusRequester = remember { FocusRequester() }
    val composerShape = RoundedCornerShape(24.dp)
    val glassSurface = MaterialTheme.colorScheme.surface
    Box(
        modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().widthIn(max = 920.dp)
                .hazeEffect(hazeState) {
                    blurEffect {
                        blurRadius = 40.dp
                        noiseFactor = 0.04f
                        backgroundColor = glassSurface.copy(alpha = 0.16f)
                        colorEffects = listOf(
                            HazeColorEffect.tint(glassSurface.copy(alpha = 0.32f))
                        )
                        fallbackTint = HazeColorEffect.tint(glassSurface.copy(alpha = 0.72f))
                    }
                }
                .clip(composerShape)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f),
                    shape = composerShape
                )
        ) {
            Box(
                Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                if (suggestions.isNotEmpty()) {
                    FlowRow(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            OutlinedButton(
                                onClick = {
                                    onSuggestionSelect(suggestion)
                                    inputFocusRequester.requestFocus()
                                },
                                modifier = Modifier.widthIn(max = 360.dp)
                            ) {
                                Text(suggestion, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (pendingAttachments.isNotEmpty()) {
                    FlowRow(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        pendingAttachments.forEach { attachment ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    Modifier.padding(start = 9.dp, end = 2.dp, top = 3.dp, bottom = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Lucide.Paperclip, null, Modifier.size(14.dp))
                                    Text(
                                        if (attachment.kind == DesktopAttachmentKind.AUDIO) {
                                            "音频 · ${attachment.name}"
                                        } else {
                                            attachment.name
                                        },
                                        Modifier.padding(start = 5.dp),
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                    IconButton(
                                        onClick = { onRemoveAttachment(attachment) },
                                        modifier = Modifier.size(28.dp)
                                    ) { Icon(Lucide.Trash2, "移除附件", Modifier.size(14.dp)) }
                                }
                            }
                        }
                    }
                }
                TextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(inputFocusRequester)
                        .heightIn(min = 58.dp, max = 150.dp)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && event.isAltPressed) {
                                onAddWithoutResponse()
                                return@onPreviewKeyEvent true
                            }
                            val enterToSend = sendOnEnter && !event.isShiftPressed
                            val shortcutToSend = !sendOnEnter && event.isCtrlPressed
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter &&
                                (enterToSend || shortcutToSend)
                            ) {
                                onSend()
                                true
                            } else false
                        },
                    placeholder = { Text("给 RikkaHub 发送消息") },
                    trailingIcon = {
                        IconButton(onClick = { fullScreenEditorOpen = true }) {
                            Icon(Lucide.Maximize2, "打开全屏编辑器", Modifier.size(18.dp))
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    minLines = 2,
                    maxLines = 6
                )
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddAttachments, modifier = Modifier.size(34.dp)) {
                        Icon(Lucide.Paperclip, "添加文件", Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleWebSearch, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Lucide.Globe,
                            if (webSearchEnabled) "关闭联网搜索" else "开启联网搜索",
                            Modifier.size(18.dp),
                            tint = if (webSearchEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if (assistant.quickMessages.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { quickMessageMenuOpen = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Lucide.Plus, "快捷消息", Modifier.size(18.dp))
                            }
                            DropdownMenu(quickMessageMenuOpen, onDismissRequest = { quickMessageMenuOpen = false }) {
                                assistant.quickMessages.forEach { message ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(message.title.ifBlank { "未命名" }, fontSize = 13.sp)
                                                Text(
                                                    message.content,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        },
                                        onClick = {
                                            quickMessageMenuOpen = false
                                            onQuickMessageSelect(message.content)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Box {
                        Row(
                            Modifier.padding(start = 6.dp).clip(RoundedCornerShape(16.dp))
                                .clickable { assistantMenuOpen = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Lucide.Sparkles, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(assistant.name, Modifier.padding(horizontal = 7.dp), fontSize = 12.sp, maxLines = 1)
                            Icon(Lucide.ChevronDown, null, Modifier.size(14.dp))
                        }
                        DropdownMenu(assistantMenuOpen, onDismissRequest = { assistantMenuOpen = false }) {
                            assistants.forEach { availableAssistant ->
                                DropdownMenuItem(
                                    text = { Text(availableAssistant.name) },
                                    leadingIcon = {
                                        if (availableAssistant.id == assistant.id) {
                                            Icon(Lucide.Sparkles, null, Modifier.size(17.dp))
                                        }
                                    },
                                    onClick = {
                                        assistantMenuOpen = false
                                        onAssistantSelect(availableAssistant.id)
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("管理助手与标签") },
                                leadingIcon = { Icon(Lucide.Settings, null, Modifier.size(17.dp)) },
                                onClick = {
                                    assistantMenuOpen = false
                                    onAssistantSettings()
                                }
                            )
                        }
                    }
                    Box {
                        Row(
                            Modifier.padding(start = 6.dp).clip(RoundedCornerShape(16.dp))
                                .clickable { modelMenuOpen = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Lucide.Bot, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(model, Modifier.padding(horizontal = 7.dp), fontSize = 12.sp, maxLines = 1)
                            Icon(Lucide.ChevronDown, null, Modifier.size(14.dp))
                        }
                        if (modelMenuOpen) ModelPickerDialog(
                            providers = providers,
                            selectedProviderId = selectedProviderId,
                            selectedModel = model,
                            reasoningEffort = assistant.reasoningEffort,
                            client = client,
                            onDismiss = { modelMenuOpen = false },
                            onSelect = { providerId, selectedModel ->
                                modelMenuOpen = false
                                onProviderModelSelect(providerId, selectedModel)
                            },
                            onReasoningEffortChange = onReasoningEffortChange,
                            onSettings = {
                                modelMenuOpen = false
                                onSettings()
                            }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    val sendEnabled = isGenerating || prompt.isNotBlank() || pendingAttachments.isNotEmpty()
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = if (sendEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
    ) {
                        Box(
                            Modifier.fillMaxSize().combinedClickable(
                                enabled = sendEnabled,
                                onClick = if (isGenerating) onCancel else onSend,
                                onLongClick = if (isGenerating) null else onAddWithoutResponse
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGenerating) {
                                Icon(
                                    Lucide.Square,
                                    "停止生成",
                                    Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    Lucide.Send,
                                    "发送；长按仅添加消息",
                                    Modifier.size(18.dp),
                                    tint = if (sendEnabled) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
    if (fullScreenEditorOpen) {
        FullScreenPromptEditor(
            initialValue = prompt,
            onDismiss = { fullScreenEditorOpen = false },
            onSave = { value ->
                onPromptChange(value)
                fullScreenEditorOpen = false
            }
        )
    }
}

@Composable
private fun FullScreenPromptEditor(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.85f).fillMaxHeight(0.85f),
        title = { Text("编辑消息") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxSize(),
                placeholder = { Text("给 RikkaHub 发送消息") }
            )
        },
        confirmButton = { Button(onClick = { onSave(value) }) { Text("完成") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun TextEditDialog(
    title: String,
    initialValue: String,
    singleLine: Boolean = false,
    allowBlank: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
                minLines = if (singleLine) 1 else 4,
                maxLines = if (singleLine) 1 else 10
            )
        },
        confirmButton = {
            Button(onClick = { onSave(value) }, enabled = allowBlank || value.isNotBlank()) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
