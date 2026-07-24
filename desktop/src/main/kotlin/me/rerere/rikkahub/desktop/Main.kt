package me.rerere.rikkahub.desktop

import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.composables.icons.lucide.Bot
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.Maximize2
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.ExternalLink
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
import com.composables.icons.lucide.Trash2
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.Base64
import java.io.File
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.StringSelection
import java.net.URI

private val SakuraLightColors = lightColorScheme(
    primary = Color(0xFF8E4955),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DD),
    onPrimaryContainer = Color(0xFF72333E),
    secondary = Color(0xFF76565A),
    secondaryContainer = Color(0xFFFFD9DD),
    tertiary = Color(0xFF785831),
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF22191A),
    surface = Color(0xFFFFF8F7),
    onSurface = Color(0xFF22191A),
    surfaceVariant = Color(0xFFF3DDDF),
    onSurfaceVariant = Color(0xFF524345),
    outline = Color(0xFF847374),
    outlineVariant = Color(0xFFD7C1C3),
    surfaceContainerLow = Color(0xFFFFF0F1),
    surfaceContainer = Color(0xFFFBEAEB),
    surfaceContainerHigh = Color(0xFFF6E4E5),
    surfaceContainerHighest = Color(0xFFF0DEDF)
)

private val SakuraDarkColors = darkColorScheme(
    primary = Color(0xFFFFB2BC),
    onPrimary = Color(0xFF561D28),
    primaryContainer = Color(0xFF72333E),
    onPrimaryContainer = Color(0xFFFFD9DD),
    secondary = Color(0xFFE5BDC1),
    onSecondary = Color(0xFF43292D),
    secondaryContainer = Color(0xFF5C3F43),
    onSecondaryContainer = Color(0xFFFFD9DD),
    tertiary = Color(0xFFEABF8F),
    background = Color(0xFF1A1112),
    onBackground = Color(0xFFF0DEDF),
    surface = Color(0xFF1A1112),
    onSurface = Color(0xFFF0DEDF),
    surfaceVariant = Color(0xFF524345),
    onSurfaceVariant = Color(0xFFD7C1C3),
    outline = Color(0xFF9F8C8E),
    outlineVariant = Color(0xFF524345),
    surfaceContainerLow = Color(0xFF22191A),
    surfaceContainer = Color(0xFF261D1E),
    surfaceContainerHigh = Color(0xFF312828),
    surfaceContainerHighest = Color(0xFF3D3233)
)

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

private val MessageTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    .withZone(ZoneId.systemDefault())

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

private fun showSaveFileDialog(owner: Frame, title: String, suggestedName: String): File? {
    val dialog = FileDialog(owner, title, FileDialog.SAVE).apply {
        file = suggestedName
        isVisible = true
    }
    val name = dialog.file ?: return null
    return File(dialog.directory, name)
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "RikkaHub",
        icon = painterResource("icon.png"),
        state = WindowState(size = DpSize(1280.dp, 820.dp))
    ) {
        RikkaHubDesktop(dialogOwner = window)
    }
}

@Composable
private fun RikkaHubDesktop(
    dialogOwner: Frame,
    store: DesktopStore = remember { DesktopStore() },
    client: OpenAiClient = remember { OpenAiClient() }
) {
    var data by remember { mutableStateOf(store.load()) }
    var prompt by remember { mutableStateOf("") }
    var pendingAttachments by remember { mutableStateOf<List<DesktopAttachment>>(emptyList()) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsSection by remember { mutableStateOf(DesktopSettingsSection.GENERAL) }
    var showSidebar by remember { mutableStateOf(true) }
    var editTarget by remember { mutableStateOf<MessageEditTarget?>(null) }
    var renameTarget by remember { mutableStateOf<ConversationRenameTarget?>(null) }
    var conversationPromptTarget by remember { mutableStateOf<ConversationPromptTarget?>(null) }
    var attachmentPickerOpen by remember { mutableStateOf(false) }
    val generationJobs = remember { mutableStateMapOf<String, Job>() }
    val generationErrors = remember { mutableStateMapOf<String, String>() }
    val scope = rememberCoroutineScope()
    var saveJob by remember { mutableStateOf<Job?>(null) }
    val latestData by rememberUpdatedState(data)

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
        if (data.assistants.any { it.id == assistantId }) {
            update(data.copy(
                selectedAssistantId = assistantId,
                conversations = data.conversations.map {
                    if (it.id == conversationId) it.copy(assistantId = assistantId) else it
                }
            ))
        }
    }

    fun selectAssistantModel(assistantId: String, providerId: String, model: String) {
        val assistant = data.assistants.firstOrNull { it.id == assistantId } ?: return
        update(data.saveAssistantProfile(assistant.copy(providerId = providerId, model = model)))
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
        val selected = showSaveFileDialog(dialogOwner, "导出 RikkaHub 备份", "rikkahub-desktop-backup.json") ?: return null
        val destination = if (selected.extension.equals("json", ignoreCase = true)) {
            selected.toPath()
        } else {
            File(selected.parentFile, "${selected.name}.json").toPath()
        }
        store.exportData(destination, data)
        return "备份已导出到 $destination"
    }

    fun exportConversation(conversation: DesktopConversation): String? {
        val selected = showSaveFileDialog(
            dialogOwner,
            "将对话导出为 Markdown",
            "${conversation.title.ifBlank { "conversation" }.take(64)}.md"
        ) ?: return null
        val destination = if (selected.extension.equals("md", ignoreCase = true)) {
            selected.toPath()
        } else {
            File(selected.parentFile, "${selected.name}.md").toPath()
        }
        destination.toFile().writeText(exportConversationMarkdown(conversation, data.configForConversation(conversation).systemPrompt))
        return "对话已导出到 $destination"
    }

    fun importBackup(): String? {
        val source = showOpenFileDialog(dialogOwner, "导入 RikkaHub 备份", multiple = false)?.firstOrNull() ?: return null
        val imported = store.importData(source.toPath())
        generationJobs.values.forEach { it.cancel() }
        generationJobs.clear()
        generationErrors.clear()
        prompt = ""
        pendingAttachments = emptyList()
        update(imported)
        return "已从 ${source.toPath()} 导入备份"
    }

    fun resetDesktopData() {
        generationJobs.values.forEach { it.cancel() }
        generationJobs.clear()
        generationErrors.clear()
        prompt = ""
        pendingAttachments = emptyList()
        store.clearSecrets(data)
        update(DesktopData())
    }

    fun chooseAttachments(): List<DesktopAttachment>? {
        return showOpenFileDialog(dialogOwner, "添加图片或文本文件", multiple = true)?.map(::loadDesktopAttachment)
    }

    fun startGeneration(
        conversationId: String,
        requestMessages: List<ChatMessage>,
        title: String? = null,
        alternativeTarget: ChatMessage? = null,
        forkName: String? = null
    ) {
        if (generationJobs.containsKey(conversationId)) return
        val generationConversation = data.conversations.firstOrNull { it.id == conversationId } ?: return
        val generationAssistant = data.assistantFor(generationConversation)
        val generationConfig = data.configForConversation(generationConversation)
        val generationMessages = runCatching {
            generationAssistant.renderMessageTemplate(
                generationAssistant.transformRequestMessages(
                    generationAssistant.limitContext(requestMessages)
                )
            )
        }.getOrElse { error ->
            generationErrors[conversationId] = error.message ?: "无效的消息模板"
            return
        }
        generationErrors.remove(conversationId)
        updateConversation(conversationId) { conversation ->
            val nextMessages = requestMessages + (alternativeTarget?.beginAlternative()
                ?: ChatMessage(role = "assistant", content = ""))
            val updated = conversation.copy(
                title = title ?: conversation.title,
                messages = nextMessages,
                updatedAt = System.currentTimeMillis()
            )
            if (forkName != null && conversation.messages != requestMessages) {
                updated.copy(
                    branches = conversation.branches + DesktopConversationBranch(
                        name = forkName,
                        messages = conversation.messages
                    )
                )
            } else {
                updated
            }
        }

        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                var request = generationMessages
                var toolRounds = 0
                while (true) {
                    client.stream(generationConfig, request).collect { delta ->
                        updateConversation(conversationId) { conversation ->
                            val messages = conversation.messages.toMutableList()
                            val last = messages.lastOrNull()
                            if (last?.role == "assistant") {
                                messages[messages.lastIndex] = last.copy(
                                    content = last.content + delta.content,
                                    reasoning = last.reasoning + delta.reasoning,
                                    promptTokens = delta.promptTokens ?: last.promptTokens,
                                    completionTokens = delta.completionTokens ?: last.completionTokens,
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
                    check(toolRounds++ < 8) { "工具调用次数超过上限" }
                    val results = client.executeToolCalls(generationConfig, toolCalls)
                    updateConversation(conversationId) { conversation ->
                        conversation.copy(
                            messages = conversation.messages + results,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    val current = data.conversations.firstOrNull { it.id == conversationId } ?: break
                    request = generationAssistant.renderMessageTemplate(
                        generationAssistant.transformRequestMessages(
                            generationAssistant.limitContext(current.messages)
                        )
                    )
                    updateConversation(conversationId) { conversation ->
                        conversation.copy(
                            messages = conversation.messages + ChatMessage(role = "assistant", content = ""),
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }
                updateConversation(conversationId) { conversation ->
                    val messages = conversation.messages.toMutableList()
                    val last = messages.lastOrNull()
                    if (last?.role == "assistant") {
                        messages[messages.lastIndex] = generationAssistant.transformGeneratedMessage(last)
                            .completeAlternative()
                    }
                    conversation.copy(messages = messages, updatedAt = System.currentTimeMillis())
                }
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
                                generationAssistant.transformGeneratedMessage(last).completeAlternative()
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
                                generationAssistant.transformGeneratedMessage(last).completeAlternative()
                        )
                    } else {
                        conversation
                    }
                }
            } finally {
                generationJobs.remove(conversationId)
            }
        }
        generationJobs[conversationId] = job
        job.start()
    }

    val selected = data.conversations.firstOrNull { it.id == data.selectedConversationId }
        ?: data.conversations.first()
    val selectedAssistant = data.assistantFor(selected)
    val effectiveConfig = data.configForAssistant(selectedAssistant)
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (data.preferences.colorMode) {
        DesktopColorMode.SYSTEM -> systemDark
        DesktopColorMode.LIGHT -> false
        DesktopColorMode.DARK -> true
    }

    MaterialTheme(colorScheme = if (useDarkTheme) SakuraDarkColors else SakuraLightColors) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
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
                        val remaining = data.conversations.filterNot { it.id == id }
                            .ifEmpty { listOf(DesktopConversation()) }
                        update(data.copy(conversations = remaining, selectedConversationId = remaining.first().id))
                    },
                    onPin = { id ->
                        updateConversation(id) { it.copy(isPinned = !it.isPinned, updatedAt = System.currentTimeMillis()) }
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
                        webSearchSettings = data.webSearchSettings,
                        client = client,
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
                        onPreferencesChange = { update(data.copy(preferences = it)) }
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
                        selectedProviderId = selectedAssistant.providerId.ifBlank { data.activeProvider().id },
                        webSearchEnabled = selected.webSearchEnabled ?: selectedAssistant.enableWebSearch,
                        showMenu = compact,
                        onMenu = { showSidebar = true },
                        onNew = ::newConversation,
                        onSettings = {
                            settingsSection = DesktopSettingsSection.PROVIDERS
                            showSettings = true
                        },
                        onProviderModelSelect = { providerId, selectedModel ->
                            selectAssistantModel(selectedAssistant.id, providerId, selectedModel)
                        },
                        onAssistantSelect = { assistantId ->
                            selectConversationAssistant(selected.id, assistantId)
                        },
                        onToggleWebSearch = {
                            updateConversation(selected.id) { conversation ->
                                val current = conversation.webSearchEnabled ?: selectedAssistant.enableWebSearch
                                conversation.copy(webSearchEnabled = !current, updatedAt = System.currentTimeMillis())
                            }
                        },
                        onPromptChange = { prompt = it },
                        pendingAttachments = pendingAttachments,
                        onAddAttachments = { attachmentPickerOpen = true },
                        onRemoveAttachment = { attachment ->
                            pendingAttachments = pendingAttachments.filterNot { it == attachment }
                        },
                        onDismissError = { generationErrors.remove(selected.id) },
                        onCancel = { generationJobs[selected.id]?.cancel() },
                        onRename = { renameTarget = ConversationRenameTarget(selected.id, selected.title) },
                        onExportConversation = {
                            runCatching { exportConversation(selected) }.onFailure { error ->
                                generationErrors[selected.id] = "导出失败：${error.message}"
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
                                updateConversation(selected.id) { conversation ->
                                    conversation.copy(
                                        messages = conversation.messages.filterIndexed { messageIndex, _ ->
                                            messageIndex != index
                                        },
                                        updatedAt = System.currentTimeMillis()
                                    )
                                }
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
                                    alternativeTarget = target.takeIf { it.role == "assistant" },
                                    forkName = "从消息 ${index + 1} 重新生成"
                                )
                            }
                        },
                        onSelectMessageVariant = { messageIndex, variantIndex ->
                            if (!generationJobs.containsKey(selected.id)) {
                                updateConversation(selected.id) { conversation ->
                                    conversation.copy(
                                        messages = conversation.messages.mapIndexed { index, message ->
                                            if (index == messageIndex) message.selectVariant(variantIndex) else message
                                        },
                                        updatedAt = System.currentTimeMillis()
                                    )
                                }
                            }
                        },
                        onSend = {
                            val text = prompt.trim()
                            if ((text.isNotEmpty() || pendingAttachments.isNotEmpty()) &&
                                !generationJobs.containsKey(selected.id)
                            ) {
                                prompt = ""
                                val attachments = pendingAttachments
                                pendingAttachments = emptyList()
                                val userMessage = ChatMessage(
                                    role = "user",
                                    content = text,
                                    attachments = attachments
                                )
                                val requestMessages = selected.messages + userMessage
                                val titleText = text.ifBlank { attachments.firstOrNull()?.name.orEmpty() }
                                val title = if (selected.title == "新对话") {
                                    titleText.take(48)
                                } else selected.title
                                startGeneration(selected.id, requestMessages, title)
                            }
                        },
                        onAddWithoutResponse = {
                            val text = prompt.trim()
                            if ((text.isNotEmpty() || pendingAttachments.isNotEmpty()) &&
                                !generationJobs.containsKey(selected.id)
                            ) {
                                prompt = ""
                                val attachments = pendingAttachments
                                pendingAttachments = emptyList()
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
                    val requestMessages = conversation.messages.take(target.messageIndex) + message.copy(content = content.trim())
                    editTarget = null
                    startGeneration(conversation.id, requestMessages, forkName = "编辑消息后重新生成")
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
    if (attachmentPickerOpen) {
        DesktopAttachmentPickerDialog(
            onDismiss = { attachmentPickerOpen = false },
            onSelect = { files ->
                attachmentPickerOpen = false
                runCatching { files.map(::loadDesktopAttachment) }.fold(
                    onSuccess = { attachments ->
                        pendingAttachments = (pendingAttachments + attachments).distinctBy { it.name to it.data }
                    },
                    onFailure = { error -> generationErrors[selected.id] = error.message ?: "无法添加文件" }
                )
            }
        )
    }
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
private fun ConversationSidebar(
    data: DesktopData,
    settingsSelected: Boolean,
    generatingConversationIds: Set<String>,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    onDelete: (String) -> Unit,
    onPin: (String) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var assistantFilterId by remember { mutableStateOf<String?>(null) }
    var assistantFilterOpen by remember { mutableStateOf(false) }
    val conversations = data.filteredConversations(query, assistantFilterId)

    Surface(modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Lucide.Sparkles, null, Modifier.padding(9.dp).size(19.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text("RikkaHub", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("欢迎回来", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            DrawerAction(Lucide.Plus, "新建对话", onNew)
            DrawerAction(Lucide.Search, "搜索对话") { searching = !searching }
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
                                assistantFilterOpen = false
                            }
                        )
                    }
                }
            }
            Text(
                "对话",
                modifier = Modifier.padding(start = 10.dp, top = 22.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                items(conversations, key = { it.id }) { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        selected = !settingsSelected && conversation.id == data.selectedConversationId,
                        generating = conversation.id in generatingConversationIds,
                        onClick = { onSelect(conversation.id) },
                        onPin = { onPin(conversation.id) },
                        onDelete = { onDelete(conversation.id) }
                    )
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
                    Text(data.config.model.ifBlank { "未选择模型" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
    selected: Boolean,
    generating: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val color = if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(color).clickable(onClick = onClick)
            .padding(start = 12.dp, end = 3.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
    selectedProviderId: String,
    webSearchEnabled: Boolean,
    showMenu: Boolean,
    onMenu: () -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit,
    onProviderModelSelect: (String, String) -> Unit,
    onAssistantSelect: (String) -> Unit,
    onToggleWebSearch: () -> Unit,
    onRename: () -> Unit,
    onExportConversation: () -> Unit,
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
    onRegenerateMessage: (Int) -> Unit,
    onSelectMessageVariant: (Int, Int) -> Unit,
    onSend: () -> Unit,
    onAddWithoutResponse: () -> Unit
) {
    var conversationMenuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val providerName = providers.firstOrNull { it.id == assistant.providerId }?.name
        ?: providers.firstOrNull { it.id == selectedProviderId }?.name
        ?: "OpenAI"
    val lastContent = conversation.messages.lastOrNull()?.content
    val lastReasoning = conversation.messages.lastOrNull()?.reasoning
    LaunchedEffect(conversation.messages.size, lastContent, lastReasoning, isGenerating) {
        if (preferences.enableAutoScroll && conversation.messages.isNotEmpty()) {
            val targetIndex = conversation.messages.size + if (isGenerating) 1 else 0
            listState.animateScrollToItem(targetIndex)
        }
    }

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
                    if (conversation.branches.isNotEmpty()) {
                        HorizontalDivider()
                        conversation.branches.forEachIndexed { index, branch ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("恢复分支 ${index + 1}: ${branch.name}")
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
                                        Icon(Lucide.Trash2, "删除分支", Modifier.size(15.dp))
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
            IconButton(onClick = onNew) { Icon(Lucide.Plus, "新建对话") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            if (conversation.messages.isEmpty()) {
                EmptyConversation(model, assistant.quickMessages, onPromptChange)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxHeight().widthIn(max = 920.dp).padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item { Spacer(Modifier.height(22.dp)) }
                    itemsIndexed(conversation.messages, key = { _, message -> message.id }) { index, message ->
                        SoftMessageReveal(message.id) {
                            MessageBlock(
                                message = message,
                                model = model,
                                providerName = providerName,
                                assistant = assistant,
                                preferences = preferences,
                                generating = isGenerating && index == conversation.messages.lastIndex,
                                actionsEnabled = !isGenerating,
                                onEdit = { onEditMessage(index, message.content) },
                                onDelete = { onDeleteMessage(index) },
                                onRegenerate = { onRegenerateMessage(index) },
                                onSelectVariant = { variantIndex -> onSelectMessageVariant(index, variantIndex) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(18.dp)) }
                }
            }
        }

        errorMessage?.let {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(it, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                    TextButton(onClick = onDismissError) { Text("关闭") }
                }
            }
        }
        Composer(
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
            sendOnEnter = preferences.sendOnEnter,
            providers = providers,
            selectedProviderId = selectedProviderId,
            webSearchEnabled = webSearchEnabled,
            onProviderModelSelect = onProviderModelSelect,
            assistant = assistant,
            assistants = assistants,
            onAssistantSelect = onAssistantSelect,
            onToggleWebSearch = onToggleWebSearch,
            onQuickMessageSelect = onPromptChange
        )
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
    generating: Boolean,
    actionsEnabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit,
    onSelectVariant: (Int) -> Unit
) {
    val isUser = message.role == "user"
    val isTool = message.role == "tool"
    val displayContent = assistant.applyRegexRules(message.content, message.role, visualOnly = true)
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    val markdownOptions = MarkdownRenderOptions(
        fontScale = preferences.fontScale,
        codeBlockAutoWrap = preferences.codeBlockAutoWrap
    )
    Column(
        Modifier.fillMaxWidth().animateContentSize(tween(180, easing = FastOutSlowInEasing)),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (isTool) {
            Text("工具结果", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        } else if (isUser || preferences.showModelIcon || preferences.showModelName || preferences.showMessageTimestamp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isUser) {
                    if (preferences.showModelIcon) {
                        DesktopProviderIcon(providerName)
                    }
                    if (preferences.showModelName) {
                        Text(
                            model,
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
                    Text("你", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    if (preferences.showUserAvatar) {
                        Surface(
                            modifier = Modifier.padding(start = 8.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                "Y",
                                Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
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
            if (message.toolCalls.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    message.toolCalls.forEach { toolCall ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                Text("调用工具：${toolCall.name}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                if (toolCall.arguments.isNotBlank() && toolCall.arguments != "{}") {
                                    Text(
                                        toolCall.arguments,
                                        Modifier.padding(top = 3.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (preferences.showReasoning && message.reasoning.isNotBlank()) {
                ReasoningBlock(
                    messageId = message.id,
                    reasoning = message.reasoning,
                    generating = generating,
                    autoCollapse = preferences.autoCollapseReasoning,
                    markdownOptions = markdownOptions
                )
            }
            if (
                message.content.isEmpty() &&
                (message.reasoning.isEmpty() || !preferences.showReasoning) &&
                generating
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                    Text("思考中...", Modifier.padding(start = 9.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (displayContent.isNotBlank()) {
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
                                if (Desktop.isDesktopSupported()) {
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
        if (!generating && !isUser && (message.promptTokens != null || message.completionTokens != null)) {
            val total = (message.promptTokens ?: 0) + (message.completionTokens ?: 0)
            Text(
                buildString {
                    message.promptTokens?.let { append("输入 $it") }
                    if (message.promptTokens != null && message.completionTokens != null) append(" · ")
                    message.completionTokens?.let { append("输出 $it") }
                    append(" · 共 $total tokens")
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val variants = message.availableVariants()
            if (!isUser && variants.size > 1) {
                MessageAction(
                    Lucide.ChevronLeft,
                    "上一条回复",
                    enabled = actionsEnabled && message.selectedVariantIndex > 0
                ) { onSelectVariant(message.selectedVariantIndex - 1) }
                Text(
                    "${message.selectedVariantIndex + 1}/${variants.size}",
                    Modifier.padding(horizontal = 3.dp).align(Alignment.CenterVertically),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                MessageAction(
                    Lucide.ChevronRight,
                    "下一条回复",
                    enabled = actionsEnabled && message.selectedVariantIndex < variants.lastIndex
                ) { onSelectVariant(message.selectedVariantIndex + 1) }
            }
            MessageAction(Lucide.Copy, "复制", enabled = displayContent.isNotEmpty()) {
                clipboardScope.launch { clipboard.setClipEntry(ClipEntry(StringSelection(displayContent))) }
            }
            if (!isTool) {
                MessageAction(Lucide.RotateCcw, "重新生成", enabled = actionsEnabled, onClick = onRegenerate)
            }
            if (isUser) {
                MessageAction(Lucide.Pencil, "编辑", enabled = actionsEnabled, onClick = onEdit)
            }
            MessageAction(Lucide.Trash2, "删除", enabled = actionsEnabled, onClick = onDelete)
        }
    }
}

@Composable
private fun AttachmentPreview(attachment: DesktopAttachment) {
    val bitmap = remember(attachment.data, attachment.isImage) {
        if (!attachment.isImage) null else runCatching {
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
                Text(attachment.name, Modifier.padding(start = 6.dp), fontSize = 11.sp)
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
private fun ReasoningBlock(
    messageId: String,
    reasoning: String,
    generating: Boolean,
    autoCollapse: Boolean,
    markdownOptions: MarkdownRenderOptions
) {
    var expanded by remember(messageId) { mutableStateOf(generating || !autoCollapse) }
    LaunchedEffect(generating, autoCollapse) {
        expanded = generating || !autoCollapse
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (expanded) Lucide.ChevronDown else Lucide.ChevronRight,
                    if (expanded) "收起思考过程" else "展开思考过程",
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (generating) "思考中..." else "思考过程",
                    Modifier.padding(start = 7.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (generating) {
                    CircularProgressIndicator(
                        Modifier.padding(start = 9.dp).size(12.dp),
                        strokeWidth = 1.5.dp
                    )
                }
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                MarkdownContent(
                    reasoning,
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
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
        Icon(icon, description, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Composer(
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
    sendOnEnter: Boolean,
    providers: List<DesktopProviderProfile>,
    selectedProviderId: String,
    webSearchEnabled: Boolean,
    onProviderModelSelect: (String, String) -> Unit,
    assistant: DesktopAssistantProfile,
    assistants: List<DesktopAssistantProfile>,
    onAssistantSelect: (String) -> Unit,
    onToggleWebSearch: () -> Unit,
    onQuickMessageSelect: (String) -> Unit
) {
    var modelMenuOpen by remember { mutableStateOf(false) }
    var assistantMenuOpen by remember { mutableStateOf(false) }
    var quickMessageMenuOpen by remember { mutableStateOf(false) }
    var fullScreenEditorOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 920.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
        ) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
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
                                    Text(attachment.name, Modifier.padding(start = 5.dp), fontSize = 11.sp, maxLines = 1)
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp, max = 150.dp)
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
                        DropdownMenu(modelMenuOpen, onDismissRequest = { modelMenuOpen = false }) {
                            providers.forEach { provider ->
                                val models = (provider.discoveredModels + provider.config.model)
                                    .filter { it.isNotBlank() }
                                    .distinct()
                                models.forEach { availableModel ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(availableModel, fontSize = 13.sp)
                                                Text(
                                                    provider.name,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            if (
                                                provider.id == selectedProviderId &&
                                                availableModel == model
                                            ) {
                                                Icon(Lucide.Sparkles, null, Modifier.size(17.dp))
                                            }
                                        },
                                        onClick = {
                                            modelMenuOpen = false
                                            onProviderModelSelect(provider.id, availableModel)
                                        }
                                    )
                                }
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("管理服务商") },
                                leadingIcon = { Icon(Lucide.Settings, null, Modifier.size(17.dp)) },
                                onClick = {
                                    modelMenuOpen = false
                                    onSettings()
                                }
                            )
                        }
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
