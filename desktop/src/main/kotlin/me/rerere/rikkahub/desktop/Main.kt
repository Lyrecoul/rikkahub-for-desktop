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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.DpOffset
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
import com.composables.icons.lucide.Minimize2
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
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt

private const val SmoothScrollAnimationTimeMillis = 400L
private const val SmoothScrollFrameDelayMillis = 7L
private const val SmoothScrollAccelerationDeltaMillis = 50L
private const val SmoothScrollAccelerationMax = 3f

private fun DesktopData.settingsContentDiffersFrom(other: DesktopData): Boolean =
    config != other.config ||
        preferences != other.preferences ||
        globalMemories != other.globalMemories ||
        providers != other.providers ||
        selectedProviderId != other.selectedProviderId ||
        assistants != other.assistants ||
        selectedAssistantId != other.selectedAssistantId ||
        webSearchSettings != other.webSearchSettings ||
        mcpServers != other.mcpServers

private fun DesktopData.modifiedSettingsSectionsFrom(other: DesktopData): Set<DesktopSettingsSection> = buildSet {
    val current = preferences
    val saved = other.preferences
    if (
        current.colorMode != saved.colorMode || current.themeColor != saved.themeColor ||
        current.fontFamily != saved.fontFamily || current.language != saved.language || current.fontScale != saved.fontScale
    ) add(DesktopSettingsSection.GENERAL)
    if (
        current.showUserAvatar != saved.showUserAvatar || current.userNickname != saved.userNickname ||
        current.showModelIcon != saved.showModelIcon || current.showModelName != saved.showModelName ||
        current.showAssistantBubble != saved.showAssistantBubble || current.showMessageTimestamp != saved.showMessageTimestamp ||
        current.showReasoning != saved.showReasoning || current.autoCollapseReasoning != saved.autoCollapseReasoning ||
        current.codeBlockAutoWrap != saved.codeBlockAutoWrap || current.enableChineseTypography != saved.enableChineseTypography ||
        current.enableMermaidRendering != saved.enableMermaidRendering || current.enableMermaidCli != saved.enableMermaidCli ||
        current.mermaidCliPath != saved.mermaidCliPath || current.mermaidUseSystemBrowser != saved.mermaidUseSystemBrowser
    ) add(DesktopSettingsSection.MESSAGE_DISPLAY)
    if (
        current.sendOnEnter != saved.sendOnEnter || current.enableAutoScroll != saved.enableAutoScroll ||
        current.enableSmoothScroll != saved.enableSmoothScroll || current.showMessageJumper != saved.showMessageJumper ||
        current.messageJumperOnLeft != saved.messageJumperOnLeft
    ) add(DesktopSettingsSection.INTERACTION)
    if (globalMemories != other.globalMemories || webSearchSettings != other.webSearchSettings || mcpServers != other.mcpServers) {
        add(DesktopSettingsSection.DATA)
    }
    if (assistants != other.assistants || selectedAssistantId != other.selectedAssistantId) add(DesktopSettingsSection.ASSISTANTS)
    if (providers != other.providers || selectedProviderId != other.selectedProviderId || config != other.config) {
        add(DesktopSettingsSection.PROVIDERS)
    }
}

private data class SmoothScrollImpulse(
    val distance: Float,
    val startTimeNanos: Long,
    var appliedDistance: Float = 0f
)

private fun smoothScrollPulse(progress: Float): Float {
    fun pulse(value: Float): Float {
        var scaledValue = value * 4f
        return if (scaledValue < 1f) {
            scaledValue - (1f - exp(-scaledValue))
        } else {
            scaledValue -= 1f
            val initialValue = exp(-1f)
            initialValue + (1f - exp(-scaledValue)) * (1f - initialValue)
        }
    }

    return pulse(progress.coerceIn(0f, 1f)) / pulse(1f)
}

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
    val startMessageIndex: Int,
    val message: ChatMessage,
    val executionSteps: List<DesktopExecutionStep>,
    val timelineAfterContent: Boolean,
    val highlighted: Boolean,
)

private enum class McpAvailability {
    UNKNOWN,
    CHECKING,
    AVAILABLE,
    UNAVAILABLE
}

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
@OptIn(ExperimentalComposeUiApi::class)
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
    var settingsDraft by remember { mutableStateOf<DesktopData?>(null) }
    var settingsExitConfirmationOpen by remember { mutableStateOf(false) }
    var pendingSettingsExit by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingSettingsExitLanguage by remember { mutableStateOf<DesktopLanguage?>(null) }
    var showSidebar by remember { mutableStateOf(true) }
    var sidebarPreferredWidth by remember { mutableStateOf(292.dp) }
    var sidebarResizeHovered by remember { mutableStateOf(false) }
    var jumpToMessageId by remember { mutableStateOf<String?>(null) }
    var jumpToMessageRequest by remember { mutableStateOf(0) }
    var renameTarget by remember { mutableStateOf<ConversationRenameTarget?>(null) }
    var conversationPromptTarget by remember { mutableStateOf<ConversationPromptTarget?>(null) }
    var folderCreateTarget by remember { mutableStateOf<FolderCreateTarget?>(null) }
    var folderRenameTarget by remember { mutableStateOf<DesktopFolder?>(null) }
    var compressionTarget by remember { mutableStateOf<CompressionTarget?>(null) }
    var showConversationStats by remember { mutableStateOf(false) }
    var translationTarget by remember { mutableStateOf<TranslationTarget?>(null) }
    var attachmentPickerOpen by remember { mutableStateOf(false) }
    var markdownExportTarget by remember { mutableStateOf<DesktopConversation?>(null) }
    var backupExportRequested by remember { mutableStateOf(false) }
    var mermaidImageExportTarget by remember { mutableStateOf<MermaidRenderResult?>(null) }
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
                    generationErrors[next.selectedConversationId] = desktopText(next.preferences.language, "runtime.save_failed")
                        .replace("%s", error.message ?: desktopText(next.preferences.language, "runtime.unknown_error"))
                }
        }
    }

    fun openSettings(section: DesktopSettingsSection) {
        settingsSection = section
        settingsDraft = data
        showSettings = true
    }

    fun updateSettingsDraft(transform: (DesktopData) -> DesktopData) {
        settingsDraft = transform(settingsDraft ?: data)
    }

    fun saveSettingsDraft() {
        val draft = (settingsDraft ?: data).let { current ->
            pendingSettingsExitLanguage?.let { language ->
                current.copy(preferences = current.preferences.copy(language = language))
            } ?: current
        }
        val deletedProviderIds = data.providers.map(DesktopProviderProfile::id) - draft.providers.map(DesktopProviderProfile::id).toSet()
        deletedProviderIds.forEach(store::deleteProviderSecret)
        val deletedAssistantIds = data.assistants.map(DesktopAssistantProfile::id) - draft.assistants.map(DesktopAssistantProfile::id).toSet()
        val dataWithDeletedAssistants = deletedAssistantIds.fold(data) { current, assistantId ->
            current.deleteAssistantProfile(assistantId)
        }
        update(dataWithDeletedAssistants.copy(
            config = draft.config,
            preferences = draft.preferences,
            globalMemories = draft.globalMemories,
            providers = draft.providers,
            selectedProviderId = draft.selectedProviderId,
            assistants = draft.assistants,
            selectedAssistantId = draft.selectedAssistantId,
            webSearchSettings = draft.webSearchSettings,
            mcpServers = draft.mcpServers
        ))
    }

    fun requestSettingsExit(afterExit: () -> Unit = {}, language: DesktopLanguage? = null) {
        val draft = settingsDraft
        val changedLanguage = language?.takeIf { it != data.preferences.language }
        if ((draft != null && draft.settingsContentDiffersFrom(data)) || changedLanguage != null) {
            pendingSettingsExit = afterExit
            pendingSettingsExitLanguage = changedLanguage
            settingsExitConfirmationOpen = true
        } else {
            settingsDraft = null
            pendingSettingsExitLanguage = null
            showSettings = false
            afterExit()
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
            name = desktopText(data.preferences.language, "defaults.new_provider"),
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
        val assistant = DesktopAssistantProfile(name = desktopText(data.preferences.language, "defaults.new_assistant"))
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
        val source = showOpenFileDialog(
            dialogOwner,
            desktopText(data.preferences.language, "file.import_backup"),
            multiple = false
        )?.firstOrNull() ?: return null
        val imported = store.importData(source.toPath())
        generationJobs.values.forEach { it.cancel() }
        generationJobs.clear()
        suggestionJobs.values.forEach { it.cancel() }
        suggestionJobs.clear()
        generationErrors.clear()
        prompt = ""
        pendingAttachments = emptyList()
        update(imported)
        settingsDraft = imported
        return desktopText(data.preferences.language, "file.imported_backup").replace("%s", source.toPath().toString())
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
        val resetData = DesktopData()
        update(resetData)
        settingsDraft = resetData
    }

    fun chooseAttachments(): List<DesktopAttachment>? {
        return showOpenFileDialog(
            dialogOwner,
            desktopText(data.preferences.language, "file.choose_attachments"),
            multiple = true
        )?.map(::loadDesktopAttachment)
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
                check(title.isNotBlank()) { desktopText(data.preferences.language, "runtime.title_empty") }
                updateConversation(conversationId) { current ->
                    current.copy(title = title, updatedAt = System.currentTimeMillis())
                }
            } catch (_: CancellationException) {
                // Keep the existing title when the request is cancelled.
            } catch (error: Throwable) {
                generationErrors[conversationId] = error.message ?: desktopText(data.preferences.language, "runtime.title_failed")
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
                check(suggestions.isNotEmpty()) { desktopText(data.preferences.language, "runtime.suggestions_empty") }
                updateConversation(conversationId) { current ->
                    current.copy(suggestions = suggestions, updatedAt = System.currentTimeMillis())
                }
            } catch (_: CancellationException) {
                // Cancellation leaves existing suggestions unchanged.
            } catch (error: Throwable) {
                generationErrors[conversationId] = error.message ?: desktopText(data.preferences.language, "runtime.suggestions_failed")
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
            generationErrors[conversationId] = desktopText(data.preferences.language, "runtime.mcp_configuration_invalid")
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
                            check(tools.isNotEmpty()) {
                                desktopText(data.preferences.language, "runtime.mcp_no_tools").replace("%s", server.name)
                            }
                        }
                    }.toMap()
                    update(data.copy(mcpServers = data.mcpServers.map { server ->
                        toolsByServerId[server.id]?.let { tools -> server.copy(tools = tools) } ?: server
                    }))
                    generationJobs.remove(conversationId)
                    handedOffToGeneration = true
                    startGeneration(conversationId, requestMessages, title, alternativeTarget)
                } catch (error: Throwable) {
                    generationErrors[conversationId] = desktopText(data.preferences.language, "runtime.mcp_sync_failed")
                        .replace("%s", error.message ?: desktopText(data.preferences.language, "runtime.unknown_error"))
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
            generationErrors[conversationId] = error.message ?: desktopText(data.preferences.language, "runtime.invalid_message_template")
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
                        val limitMessage = desktopText(data.preferences.language, "runtime.tool_round_limit")
                            .replace("%d", generationAssistant.maxToolRounds.toString())
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
                generationErrors[conversationId] = error.message ?: desktopText(data.preferences.language, "runtime.request_failed")
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
            generationErrors[conversationId] = desktopText(data.preferences.language, "runtime.not_enough_messages")
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
                check(summary.isNotBlank()) { desktopText(data.preferences.language, "runtime.compression_empty") }
                updateConversation(conversationId) { current ->
                    current.replaceHistoryWithSummary(summary, keepRecentMessages)
                }
            } catch (_: CancellationException) {
                // Cancellation leaves the original conversation untouched.
            } catch (error: Throwable) {
                generationErrors[conversationId] = error.message ?: desktopText(data.preferences.language, "runtime.compression_failed")
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
                check(result.isNotBlank()) { desktopText(data.preferences.language, "runtime.translation_empty") }
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
                generationErrors[target.conversationId] = error.message ?: desktopText(data.preferences.language, "runtime.translation_failed")
            } finally {
                generationJobs.remove(target.conversationId)
                translatedMessageId?.let { messageId ->
                    jumpToMessageId = messageId
                    jumpToMessageRequest++
                }
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
                    requestSettingsExit(::newConversation)
                    true
                } else {
                    false
                }
            }
        ) {
        val compact = maxWidth < 850.dp
        val maxSidebarWidth = minOf(480.dp, maxWidth - 420.dp)
        val sidebarWidth = if (compact) maxWidth else sidebarPreferredWidth.coerceIn(240.dp, maxSidebarWidth)
        val density = LocalDensity.current
        Row(Modifier.fillMaxSize()) {
            if (!compact || showSidebar) {
                ConversationSidebar(
                    data = data,
                    settingsSelected = showSettings,
                    generatingConversationIds = generationJobs.keys,
                    onSelect = {
                        requestSettingsExit(afterExit = {
                            update(data.copy(selectedConversationId = it))
                            jumpToMessageId = null
                            if (compact) showSidebar = false
                        })
                    },
                    onSelectFavorite = { conversationId, messageId ->
                        requestSettingsExit(afterExit = {
                            update(data.copy(selectedConversationId = conversationId))
                            jumpToMessageId = messageId
                            jumpToMessageRequest++
                            if (compact) showSidebar = false
                        })
                    },
                    onNew = {
                        requestSettingsExit(afterExit = {
                            newConversation()
                            if (compact) showSidebar = false
                        })
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
                    onCreateFolder = { assistantId ->
                        folderCreateTarget = FolderCreateTarget(assistantId = assistantId)
                    },
                    onConversationSortChange = { sort ->
                        update(data.copy(preferences = data.preferences.copy(conversationSort = sort)))
                    },
                    onSettings = {
                        openSettings(DesktopSettingsSection.GENERAL)
                        if (compact) showSidebar = false
                    },
                    modifier = Modifier.width(sidebarWidth)
                )
                if (!compact) {
                    Box(
                        Modifier.fillMaxHeight().width(8.dp)
                            .onPointerEvent(PointerEventType.Enter) { sidebarResizeHovered = true }
                            .onPointerEvent(PointerEventType.Exit) { sidebarResizeHovered = false }
                            .pointerInput(maxSidebarWidth) {
                                detectDragGestures(
                                    onDragStart = { sidebarResizeHovered = true },
                                    onDragEnd = { sidebarResizeHovered = false },
                                    onDragCancel = { sidebarResizeHovered = false }
                                ) { _, dragAmount ->
                                    sidebarPreferredWidth = (sidebarPreferredWidth + with(density) { dragAmount.x.toDp() })
                                        .coerceIn(240.dp, maxSidebarWidth)
                                }
                            }
                    ) {
                        Box(
                            Modifier.align(Alignment.Center).fillMaxHeight()
                                .width(if (sidebarResizeHovered) 2.dp else 1.dp)
                                .background(
                                    MaterialTheme.colorScheme.outlineVariant.copy(
                                        alpha = if (sidebarResizeHovered) 0.9f else 0.45f
                                    )
                                )
                        )
                    }
                }
            }
            if (!compact || !showSidebar) {
                if (showSettings) {
                    val settingsData = settingsDraft ?: data
                    val modifiedProviderIds = settingsData.providers.filter { provider ->
                        data.providers.firstOrNull { it.id == provider.id } != provider
                    }.mapTo(mutableSetOf(), DesktopProviderProfile::id)
                    val modifiedAssistantIds = settingsData.assistants.filter { assistant ->
                        data.assistants.firstOrNull { it.id == assistant.id } != assistant
                    }.mapTo(mutableSetOf(), DesktopAssistantProfile::id)
                    val modifiedSections = settingsData.modifiedSettingsSectionsFrom(data)
                    DesktopSettingsPane(
                        providers = settingsData.providers.ifEmpty { listOf(settingsData.activeProvider()) },
                        selectedProviderId = settingsData.activeProvider().id,
                        assistants = settingsData.assistants.ifEmpty { listOf(settingsData.activeAssistant()) },
                        selectedAssistantId = settingsData.activeAssistant().id,
                        preferences = settingsData.preferences,
                        globalMemories = settingsData.globalMemories,
                        webSearchSettings = settingsData.webSearchSettings,
                        client = client,
                        mcpServers = settingsData.mcpServers,
                        mcpClient = mcpClient,
                        initialSection = settingsSection,
                        showMenu = compact,
                        onMenu = { showSidebar = true },
                        onBack = { language -> requestSettingsExit(language = language) },
                        modifiedProviderIds = modifiedProviderIds,
                        modifiedAssistantIds = modifiedAssistantIds,
                        modifiedSections = modifiedSections,
                        hasUnsavedChanges = settingsData.settingsContentDiffersFrom(data),
                        onSaveAll = {
                            saveSettingsDraft()
                            settingsDraft = null
                        },
                        onProviderSelect = { providerId -> updateSettingsDraft { it.selectProviderConfig(providerId) } },
                        onProviderSave = { profile -> updateSettingsDraft { it.saveProviderProfile(profile) } },
                        onProviderAdd = {
                            val profile = DesktopProviderProfile(
                                name = desktopText(settingsData.preferences.language, "defaults.new_provider"),
                                config = DesktopConfig(model = "", systemPrompt = settingsData.config.systemPrompt)
                            )
                            updateSettingsDraft {
                                it.copy(
                                    config = profile.config,
                                    providers = it.providers.ifEmpty { listOf(it.activeProvider()) } + profile,
                                    selectedProviderId = profile.id
                                )
                            }
                        },
                        onProviderDelete = { providerId -> updateSettingsDraft { it.deleteProviderProfile(providerId) } },
                        onAssistantSelect = { assistantId ->
                            updateSettingsDraft {
                                if (it.assistants.any { assistant -> assistant.id == assistantId }) {
                                    it.copy(selectedAssistantId = assistantId)
                                } else {
                                    it
                                }
                            }
                        },
                        onAssistantSave = { profile -> updateSettingsDraft { it.saveAssistantProfile(profile) } },
                        onAssistantAdd = {
                            val assistant = DesktopAssistantProfile(name = desktopText(settingsData.preferences.language, "defaults.new_assistant"))
                            updateSettingsDraft { it.copy(assistants = it.assistants + assistant, selectedAssistantId = assistant.id) }
                        },
                        onAssistantCopy = { assistantId ->
                            val source = (settingsDraft ?: data).assistants.firstOrNull { it.id == assistantId } ?: return@DesktopSettingsPane
                            val copy = source.copy(id = UUID.randomUUID().toString(), name = "${source.name} copy")
                            updateSettingsDraft { it.copy(assistants = it.assistants + copy, selectedAssistantId = copy.id) }
                        },
                        onAssistantDelete = { assistantId -> updateSettingsDraft { it.deleteAssistantProfile(assistantId) } },
                        onExportData = {
                            runCatching { exportBackup() }.getOrElse {
                                desktopText(data.preferences.language, "runtime.export_failed").replace("%s", it.message.orEmpty())
                            }
                        },
                        onImportData = {
                            runCatching { importBackup() }.getOrElse {
                                desktopText(data.preferences.language, "runtime.import_failed").replace("%s", it.message.orEmpty())
                            }
                        },
                        onResetData = ::resetDesktopData,
                        onWebSearchSettingsChange = { value -> updateSettingsDraft { it.copy(webSearchSettings = value) } },
                        onMcpServersChange = { servers -> updateSettingsDraft { it.copy(mcpServers = servers) } },
                        onPreferencesChange = { value -> updateSettingsDraft { it.copy(preferences = value) } },
                        onGlobalMemoriesChange = { memories ->
                            updateSettingsDraft { it.copy(globalMemories = memories.filter { memory -> memory.content.isNotBlank() }) }
                        }
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
                        // The chat toolbar is also the folder management entry point, so it must
                        // expose every saved folder rather than only the current assistant's scope.
                        folders = data.folders,
                        mcpServers = data.mcpServers,
                        mcpClient = mcpClient,
                        selectedProviderId = selectedAssistant.providerId.ifBlank { data.activeProvider().id },
                        webSearchEnabled = selected.webSearchEnabled ?: selectedAssistant.enableWebSearch,
                        jumpToMessageId = jumpToMessageId,
                        jumpToMessageRequest = jumpToMessageRequest,
                        conversationScrollPositions = conversationScrollPositions,
                        onAskUserAnswer = ::submitAskUserAnswer,
                        onSaveMermaidImage = { mermaidImageExportTarget = it },
                        showMenu = compact,
                        onMenu = { showSidebar = true },
                        onNew = ::newConversation,
                        onSettings = {
                            openSettings(DesktopSettingsSection.PROVIDERS)
                        },
                        onAssistantSettings = {
                            openSettings(DesktopSettingsSection.ASSISTANTS)
                        },
                        onProviderModelSelect = { providerId, selectedModel ->
                            selectAssistantModel(selectedAssistant.id, providerId, selectedModel)
                        },
                        onReasoningEffortChange = { effort -> updateAssistantReasoningEffort(selectedAssistant.id, effort) },
                        client = client,
                        onAssistantSelect = { assistantId ->
                            selectConversationAssistant(selected.id, assistantId)
                        },
                        onMcpServersChange = { servers -> update(data.copy(mcpServers = servers)) },
                        onAssistantMcpServerIdsChange = { serverIds ->
                            saveAssistant(selectedAssistant.copy(mcpServerIds = serverIds))
                        },
                        onMcpSettings = {
                            openSettings(DesktopSettingsSection.PROVIDERS)
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
                        onRenameFolder = { folder -> folderRenameTarget = folder },
                        onDeleteFolder = { folder -> update(data.deleteFolder(folder.id)) },
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
                        onSaveMessageEdit = { index, content ->
                            val message = selected.messages.getOrNull(index) ?: return@ChatPane
                            if (content.isBlank()) return@ChatPane
                            updateConversation(selected.id) { conversation -> conversation.editMessageAt(index, content) }
                            if (message.role == "user") {
                                val requestMessages = selected.messages.take(index) + message.addVariant(content)
                                startGeneration(selected.id, requestMessages)
                            }
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

    if (settingsExitConfirmationOpen) {
        AlertDialog(
            onDismissRequest = {
                settingsExitConfirmationOpen = false
                pendingSettingsExit = null
                pendingSettingsExitLanguage = null
            },
            title = { Text(desktopText(data.preferences.language, "settings.unsaved_changes_title")) },
            text = { Text(desktopText(data.preferences.language, "settings.unsaved_changes_description")) },
            confirmButton = {
                Button(onClick = {
                    saveSettingsDraft()
                    settingsDraft = null
                    pendingSettingsExitLanguage = null
                    showSettings = false
                    settingsExitConfirmationOpen = false
                    pendingSettingsExit?.invoke()
                    pendingSettingsExit = null
                }) { Text(desktopText(data.preferences.language, "common.save")) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        settingsExitConfirmationOpen = false
                        pendingSettingsExit = null
                        pendingSettingsExitLanguage = null
                    }) { Text(desktopText(data.preferences.language, "common.cancel")) }
                    TextButton(onClick = {
                        settingsDraft = null
                        pendingSettingsExitLanguage = null
                        showSettings = false
                        settingsExitConfirmationOpen = false
                        pendingSettingsExit?.invoke()
                        pendingSettingsExit = null
                    }) { Text(desktopText(data.preferences.language, "settings.discard_changes")) }
                }
            }
        )
    }

    renameTarget?.let { target ->
        TextEditDialog(
            title = desktopText(data.preferences.language, "dialog.rename_conversation"),
            language = data.preferences.language,
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
            title = desktopText(data.preferences.language, "dialog.conversation_system_prompt"),
            language = data.preferences.language,
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
            title = desktopText(data.preferences.language, "chat.new_folder"),
            language = data.preferences.language,
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
    folderRenameTarget?.let { folder ->
        TextEditDialog(
            title = desktopText(data.preferences.language, "sidebar.rename_folder"),
            language = data.preferences.language,
            initialValue = folder.name,
            singleLine = true,
            onDismiss = { folderRenameTarget = null },
            onSave = { name ->
                if (name.isNotBlank()) update(data.renameFolder(folder.id, name))
                folderRenameTarget = null
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
                language = data.preferences.language,
                onDismiss = { compressionTarget = null },
                onConfirm = { targetTokens, keepRecentMessages, additionalPrompt ->
                    compressionTarget = null
                    startCompression(target.conversationId, targetTokens, keepRecentMessages, additionalPrompt)
                }
            )
        }
    }
    if (showConversationStats) {
        ConversationStatsDialog(
            conversation = selected,
            language = data.preferences.language,
            onDismiss = { showConversationStats = false }
        )
    }
    translationTarget?.let { target ->
        TranslationDialog(
            language = data.preferences.language,
            onDismiss = { translationTarget = null },
            onConfirm = { language ->
                translationTarget = null
                startTranslation(target, language)
            }
        )
    }
    if (attachmentPickerOpen) {
        DesktopAttachmentPickerDialog(
            language = data.preferences.language,
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
                    onFailure = { error ->
                        generationErrors[selected.id] = error.message
                            ?: desktopText(data.preferences.language, "runtime.add_attachment_failed")
                    }
                )
            }
        )
    }
    markdownExportTarget?.let { conversation ->
        DesktopSaveFileDialog(
            title = desktopText(data.preferences.language, "file.export_conversation_markdown"),
            language = data.preferences.language,
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
                    generationErrors[conversation.id] = desktopText(data.preferences.language, "runtime.export_failed")
                        .replace("%s", error.message ?: desktopText(data.preferences.language, "runtime.unknown_error"))
                }
            }
        )
    }
    if (backupExportRequested) {
        DesktopSaveFileDialog(
            title = desktopText(data.preferences.language, "file.export_backup"),
            language = data.preferences.language,
            suggestedName = "rikkahub-desktop-backup.json",
            requiredExtension = "json",
            onDismiss = { backupExportRequested = false },
            onSave = { destination ->
                backupExportRequested = false
                runCatching { store.exportData(destination.toPath(), data) }.onFailure { error ->
                    generationErrors[data.selectedConversationId] = desktopText(data.preferences.language, "runtime.export_failed")
                        .replace("%s", error.message ?: desktopText(data.preferences.language, "runtime.unknown_error"))
                }
            }
        )
    }
    mermaidImageExportTarget?.let { renderedDiagram ->
        DesktopSaveFileDialog(
            title = desktopText(data.preferences.language, "mermaid.save_image"),
            language = data.preferences.language,
            suggestedName = "mermaid-diagram.png",
            requiredExtension = "png",
            allowedExtensions = listOf("png", "svg"),
            onDismiss = { mermaidImageExportTarget = null },
            onSave = { destination ->
                mermaidImageExportTarget = null
                scope.launch {
                    runCatching {
                        val bytes = withContext(Dispatchers.IO) {
                            if (destination.extension.equals("svg", ignoreCase = true)) {
                                DesktopMermaidRenderer.renderSvg(renderedDiagram)
                                    ?: error("Mermaid SVG rendering failed")
                            } else {
                                renderedDiagram.pngBytes
                            }
                        }
                        destination.writeBytes(bytes)
                    }.onFailure { error ->
                        generationErrors[data.selectedConversationId] = desktopText(data.preferences.language, "runtime.save_failed")
                            .replace("%s", error.message ?: desktopText(data.preferences.language, "runtime.unknown_error"))
                    }
                }
            }
        )
    }
    pendingAgentApproval?.let { pending ->
        DesktopAgentApprovalDialog(
            request = pending.request,
            language = data.preferences.language,
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
    language: DesktopLanguage,
    onApprove: (Boolean) -> Unit,
    onDeny: () -> Unit
) {
    var autoApprove by remember(request) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDeny,
        title = { Text(request.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(desktopText(language, "agent.approval_request"))
                Text(request.detail, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text(
                    when (request.kind) {
                        DesktopAgentApprovalKind.SHELL -> when (request.backend) {
                            DesktopAgentBackend.DOCKER -> if (request.network) {
                                desktopText(language, "agent.docker_network_warning")
                            } else {
                                desktopText(language, "agent.docker_warning")
                            }
                            DesktopAgentBackend.LOCAL -> desktopText(language, "agent.local_shell_warning")
                            null -> desktopText(language, "agent.confirm_impact")
                        }
                        DesktopAgentApprovalKind.IMAGE_PULL -> desktopText(language, "agent.image_pull_warning")
                        DesktopAgentApprovalKind.SKILL -> desktopText(language, "agent.skill_warning")
                        DesktopAgentApprovalKind.WRITE -> desktopText(language, "agent.write_warning")
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
                                    DesktopAgentApprovalScope.DOCKER_SHELL -> desktopText(language, "agent.remember_docker_shell")
                                    DesktopAgentApprovalScope.DOCKER_NETWORK -> desktopText(language, "agent.remember_docker_network")
                                    DesktopAgentApprovalScope.IMAGE_PULL -> desktopText(language, "agent.remember_image_pull")
                                    else -> desktopText(language, "agent.remember_operation")
                                }
                            )
                        }
                    )
                }
            }
        },
        confirmButton = { Button(onClick = { onApprove(autoApprove) }) { Text(desktopText(language, "agent.allow")) } },
        dismissButton = { TextButton(onClick = onDeny) { Text(desktopText(language, "agent.deny")) } }
    )
}

@Composable
private fun CompressionDialog(
    messageCount: Int,
    language: DesktopLanguage,
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
        title = { Text(desktopText(language, "dialog.compress_history")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    desktopText(language, "dialog.compress_history_description"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = targetTokens,
                    onValueChange = { value -> if (value.all(Char::isDigit)) targetTokens = value },
                    label = { Text(desktopText(language, "dialog.target_tokens")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = keepRecentMessages,
                    onValueChange = { value -> if (value.all(Char::isDigit)) keepRecentMessages = value },
                    label = { Text(desktopText(language, "dialog.keep_recent_messages").replace("%d", messageCount.toString())) },
                    isError = keep != null && (keep < 0 || keep >= messageCount),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = additionalPrompt,
                    onValueChange = { additionalPrompt = it },
                    label = { Text(desktopText(language, "dialog.additional_instructions")) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onConfirm(target!!, keep!!, additionalPrompt) }
            ) { Text(desktopText(language, "dialog.compress")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(desktopText(language, "common.cancel")) } }
    )
}

@Composable
private fun ConversationStatsDialog(conversation: DesktopConversation, language: DesktopLanguage, onDismiss: () -> Unit) {
    val stats = conversation.stats()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(desktopText(language, "chat.statistics")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ConversationStatRow(desktopText(language, "stats.messages"), "${stats.messageCount} (${stats.userMessageCount} / ${stats.assistantMessageCount})")
                ConversationStatRow(desktopText(language, "stats.attachments"), stats.attachmentCount.toString())
                ConversationStatRow(desktopText(language, "stats.characters"), stats.characterCount.toString())
                ConversationStatRow(desktopText(language, "message.input_tokens"), stats.promptTokens.toString())
                ConversationStatRow(desktopText(language, "message.output_tokens"), stats.completionTokens.toString())
                ConversationStatRow(desktopText(language, "message.cached_tokens"), stats.cachedTokens.toString())
                ConversationStatRow(desktopText(language, "stats.created_at"), MessageTimeFormatter.format(Instant.ofEpochMilli(conversation.createdAt)))
                ConversationStatRow(desktopText(language, "stats.updated_at"), MessageTimeFormatter.format(Instant.ofEpochMilli(conversation.updatedAt)))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(desktopText(language, "chat.close")) } }
    )
}

@Composable
private fun TranslationDialog(language: DesktopLanguage, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var targetLanguage by remember(language) { mutableStateOf(desktopText(language, "language.chinese")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(desktopText(language, "dialog.translate_message")) },
        text = {
            OutlinedTextField(
                value = targetLanguage,
                onValueChange = { targetLanguage = it },
                label = { Text(desktopText(language, "dialog.target_language")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(targetLanguage.trim()) }, enabled = targetLanguage.isNotBlank()) {
                Text(desktopText(language, "message.translate"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(desktopText(language, "common.cancel")) } }
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
private fun DesktopAttachmentPickerDialog(
    language: DesktopLanguage,
    onDismiss: () -> Unit,
    onSelect: (List<File>) -> Unit
) {
    var directory by remember { mutableStateOf(File(System.getProperty("user.home"))) }
    var selectedPaths by remember { mutableStateOf(emptySet<String>()) }
    val entries = remember(directory) {
        directory.listFiles().orEmpty()
            .filter { it.isDirectory || isDesktopAttachmentSupported(it) }
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(desktopText(language, "file.add_attachments")) },
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
                    ) { Icon(Lucide.ChevronLeft, desktopText(language, "file.parent_directory")) }
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
                                Icon(Lucide.Sparkles, desktopText(language, "file.selected"), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(selectedPaths.map(::File)) }, enabled = selectedPaths.isNotEmpty()) {
                Text(desktopText(language, "file.add_count").replace("%d", selectedPaths.size.toString()))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(desktopText(language, "common.cancel")) } }
    )
}

@Composable
private fun DesktopSaveFileDialog(
    title: String,
    language: DesktopLanguage,
    suggestedName: String,
    requiredExtension: String,
    allowedExtensions: List<String> = listOf(requiredExtension),
    onDismiss: () -> Unit,
    onSave: (File) -> Unit
) {
    var directory by remember { mutableStateOf(File(System.getProperty("user.home"))) }
    var fileName by remember(suggestedName) { mutableStateOf(suggestedName) }
    val extensions = allowedExtensions.map { it.lowercase() }.distinct().ifEmpty { listOf(requiredExtension.lowercase()) }
    var selectedExtension by remember(suggestedName, extensions) { mutableStateOf(extensions.first()) }
    val entries = remember(directory) {
        directory.listFiles().orEmpty()
            .filter { it.isDirectory || it.isFile }
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
    }
    val normalizedName = fileName.trim().let { name ->
        if (File(name).extension.equals(selectedExtension, ignoreCase = true)) name else "$name.$selectedExtension"
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
                    ) { Icon(Lucide.ChevronLeft, desktopText(language, "file.parent_directory")) }
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
                    label = { Text(desktopText(language, "file.file_name")) },
                    isError = fileName.isNotBlank() && !validName,
                    singleLine = true
                )
                if (extensions.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        extensions.forEach { extension ->
                            FilterChip(
                                selected = selectedExtension == extension,
                                onClick = {
                                    selectedExtension = extension
                                    fileName = fileName.substringBeforeLast('.', fileName) + ".${extension}"
                                },
                                label = { Text(extension.uppercase()) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(File(directory, normalizedName)) }, enabled = validName) {
                Text(desktopText(language, "common.save"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(desktopText(language, "common.cancel")) } }
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
    onCreateFolder: (String) -> Unit,
    onConversationSortChange: (DesktopConversationSort) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = data.preferences.language
    val appIcon = rememberDesktopResourcePainter("icon.png")
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var assistantFilterId by remember { mutableStateOf<String?>(null) }
    var assistantFilterOpen by remember { mutableStateOf(false) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var tagFilterOpen by remember { mutableStateOf(false) }
    var folderFilterId by remember { mutableStateOf<String?>(null) }
    var showFavorites by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val conversationListState = rememberLazyListState()
    val availableFolders = data.folders
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

    LaunchedEffect(folderFilterId, data.folders) {
        if (folderFilterId != null && data.folders.none { it.id == folderFilterId }) {
            folderFilterId = null
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
                    Text(desktopText(language, "sidebar.welcome"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            DrawerAction(Lucide.Plus, desktopText(language, "sidebar.new_chat"), onNew)
            DrawerAction(Lucide.Search, desktopText(language, "sidebar.search_chats")) { searching = !searching }
            DrawerAction(Lucide.Star, desktopText(language, if (showFavorites) "sidebar.back_to_chats" else "sidebar.favorite_messages")) {
                showFavorites = !showFavorites
            }
            if (searching) {
                OutlinedTextField(
                    query,
                    { query = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    placeholder = { Text(desktopText(language, "sidebar.search")) },
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
                        filterAssistant?.name ?: desktopText(language, "sidebar.all_assistants"),
                        Modifier.padding(start = 9.dp).weight(1f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Lucide.ChevronDown, null, Modifier.size(15.dp))
                }
                DropdownMenu(assistantFilterOpen, onDismissRequest = { assistantFilterOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(desktopText(language, "sidebar.all_assistants")) },
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
                    Text(tagFilter ?: desktopText(language, "sidebar.all_tags"), Modifier.padding(start = 9.dp).weight(1f), fontSize = 13.sp, maxLines = 1)
                    Icon(Lucide.ChevronDown, null, Modifier.size(15.dp))
                }
                DropdownMenu(tagFilterOpen, onDismissRequest = { tagFilterOpen = false }) {
                    DropdownMenuItem(text = { Text(desktopText(language, "sidebar.all_tags")) }, onClick = { tagFilter = null; tagFilterOpen = false })
                    tags.forEach { tag ->
                        DropdownMenuItem(text = { Text(tag) }, onClick = { tagFilter = tag; tagFilterOpen = false })
                    }
                }
            }
            if (!showFavorites) {
                FolderCapsuleBar(
                    folders = availableFolders,
                    selectedFolderId = folderFilterId,
                    language = language,
                    onSelect = { folderFilterId = it },
                    onCreate = { onCreateFolder(assistantFilterId ?: data.activeAssistant().id) }
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 10.dp, top = 18.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    desktopText(language, if (showFavorites) "sidebar.favorite_messages" else "sidebar.chats"),
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
                                    desktopText(language, "sidebar.recent")
                                } else {
                                    desktopText(language, "sidebar.most_active")
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
                                            desktopText(
                                                language,
                                                if (sort == DesktopConversationSort.RECENT) "sidebar.sort_recent" else "sidebar.sort_most_active"
                                            )
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
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = conversationListState,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
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
                                    message.content.ifBlank { message.reasoning }.ifBlank { desktopText(language, "sidebar.tool_call") },
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
                                val timelineLabel = conversationTimelineLabel(conversation.updatedAt, language)
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
                                language = language,
                                selected = !settingsSelected && conversation.id == data.selectedConversationId,
                                generating = conversation.id in generatingConversationIds,
                                onClick = { onSelect(conversation.id) },
                                onPin = { onPin(conversation.id) },
                                onDelete = { onDelete(conversation.id) },
                                folders = data.folders,
                                onMoveToFolder = { folderId -> onMoveToFolder(conversation.id, folderId) }
                            )
                            }
                        }
                    }
                }
                if (conversationListState.canScrollBackward) {
                    Box(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth().height(28.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceContainerLow,
                                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                if (conversationListState.canScrollForward) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(28.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f),
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                )
                            )
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
                Icon(Lucide.Settings, desktopText(language, "settings.title"), Modifier.size(19.dp))
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(desktopText(language, "settings.title"), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun FolderCapsuleBar(
    folders: List<DesktopFolder>,
    selectedFolderId: String?,
    language: DesktopLanguage,
    onSelect: (String?) -> Unit,
    onCreate: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrollDistance = with(LocalDensity.current) { 180.dp.toPx() }
    val wheelScrollDistance = with(LocalDensity.current) { 48.dp.toPx() }
    Box(modifier = Modifier.fillMaxWidth().height(40.dp).padding(top = 4.dp)) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().onPointerEvent(PointerEventType.Scroll) { event ->
                val scrollDelta = event.changes.firstOrNull()?.scrollDelta ?: return@onPointerEvent
                val horizontalDelta = scrollDelta.y.takeIf { it != 0f } ?: scrollDelta.x
                if (horizontalDelta != 0f) {
                    event.changes.forEach { it.consume() }
                    scope.launch { listState.animateScrollBy(horizontalDelta * wheelScrollDistance) }
                }
            },
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FolderCapsule(
                    label = desktopText(language, "sidebar.chats"),
                    selected = selectedFolderId == null,
                    onClick = { onSelect(null) }
                )
            }
            items(folders, key = { it.id }) { folder ->
                FolderCapsule(
                    label = folder.name,
                    selected = selectedFolderId == folder.id,
                    icon = Lucide.Folder,
                    onClick = { onSelect(folder.id) }
                )
            }
            item {
                FolderCapsule(
                    label = desktopText(language, "common.new"),
                    selected = false,
                    icon = Lucide.Plus,
                    onClick = onCreate
                )
            }
        }
        if (listState.canScrollBackward) {
            Box(
                Modifier.align(Alignment.CenterStart).fillMaxHeight().width(42.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.76f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                IconButton(
                    onClick = { scope.launch { listState.animateScrollBy(-scrollDistance) } },
                    modifier = Modifier.align(Alignment.CenterStart).size(36.dp)
                ) {
                    Icon(Lucide.ChevronLeft, desktopText(language, "sidebar.chats"))
                }
            }
        }
        if (listState.canScrollForward) {
            Box(
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(42.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.76f),
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
            ) {
                IconButton(
                    onClick = { scope.launch { listState.animateScrollBy(scrollDistance) } },
                    modifier = Modifier.align(Alignment.CenterEnd).size(36.dp)
                ) {
                    Icon(Lucide.ChevronRight, desktopText(language, "sidebar.chats"))
                }
            }
        }
    }
}

@Composable
private fun FolderCapsule(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.clip(CircleShape).combinedClickable(
            onClick = onClick
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

private fun conversationTimelineLabel(
    updatedAt: Long,
    language: DesktopLanguage,
    today: LocalDate = LocalDate.now()
): String {
    val date = Instant.ofEpochMilli(updatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(date, today)
    return when (daysAgo) {
        0L -> desktopText(language, "timeline.today")
        1L -> desktopText(language, "timeline.yesterday")
        2L -> desktopText(language, "timeline.day_before_yesterday")
        in 3L..30L -> desktopText(language, "timeline.days_ago").replace("%d", daysAgo.toString())
        else -> date.toString()
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
    language: DesktopLanguage,
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
                desktopText(language, "conversation.branch"),
                Modifier.padding(end = 7.dp).size(15.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(conversation.title, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
        if (generating) {
            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
        } else if (conversation.isPinned) {
            Icon(Lucide.Pin, desktopText(language, "conversation.pinned"), Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(34.dp)) {
            Icon(Lucide.Ellipsis, desktopText(language, "chat.options"), Modifier.size(17.dp))
            }
            DropdownMenu(menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(desktopText(language, "conversation.move_to_chat")) },
                    leadingIcon = { Icon(Lucide.Folder, null, Modifier.size(18.dp)) },
                    onClick = {
                        menuOpen = false
                        onMoveToFolder(null)
                    }
                )
                folders.forEach { folder ->
                    DropdownMenuItem(
                        text = { Text(desktopText(language, "conversation.move_to_folder").replace("%s", folder.name)) },
                        leadingIcon = { Icon(Lucide.Folder, null, Modifier.size(18.dp)) },
                        onClick = {
                            menuOpen = false
                            onMoveToFolder(folder.id)
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(desktopText(language, if (conversation.isPinned) "conversation.unpin" else "conversation.pin")) },
                    leadingIcon = {
                        Icon(if (conversation.isPinned) Lucide.PinOff else Lucide.Pin, null, Modifier.size(18.dp))
                    },
                    onClick = {
                        menuOpen = false
                        onPin()
                    }
                )
                DropdownMenuItem(
                    text = { Text(desktopText(language, "message.delete")) },
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
    mcpServers: List<DesktopMcpServer>,
    mcpClient: DesktopMcpClient,
    selectedProviderId: String,
    webSearchEnabled: Boolean,
    jumpToMessageId: String?,
    jumpToMessageRequest: Int,
    conversationScrollPositions: MutableMap<String, Pair<Int, Int>>,
    onAskUserAnswer: (String, DesktopToolCall, String) -> Unit,
    onSaveMermaidImage: (MermaidRenderResult) -> Unit,
    showMenu: Boolean,
    onMenu: () -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit,
    onAssistantSettings: () -> Unit,
    onProviderModelSelect: (String, String) -> Unit,
    onReasoningEffortChange: (String) -> Unit,
    client: OpenAiClient,
    onAssistantSelect: (String) -> Unit,
    onMcpServersChange: (List<DesktopMcpServer>) -> Unit,
    onAssistantMcpServerIdsChange: (Set<String>) -> Unit,
    onMcpSettings: () -> Unit,
    onToggleWebSearch: () -> Unit,
    onRename: () -> Unit,
    onExportConversation: () -> Unit,
    onMoveToFolder: (String?) -> Unit,
    onCreateFolder: () -> Unit,
    onRenameFolder: (DesktopFolder) -> Unit,
    onDeleteFolder: (DesktopFolder) -> Unit,
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
    onSaveMessageEdit: (Int, String) -> Unit,
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
    val language = preferences.language
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val hazeState = rememberHazeState()
    val smoothScrollStepSize = with(LocalDensity.current) { 100.dp.toPx() }
    val smoothScrollQueue = remember { mutableListOf<SmoothScrollImpulse>() }
    var smoothScrollInProgress by remember { mutableStateOf(false) }
    var smoothScrollDirection by remember { mutableStateOf(0) }
    var lastSmoothScrollTimeMillis by remember { mutableStateOf(0L) }
    var composerHeightPx by remember { mutableStateOf(164) }
    val messageBottomPadding = with(LocalDensity.current) { composerHeightPx.toDp() + 16.dp }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    var editingMessageIndex by remember(conversation.id) { mutableStateOf<Int?>(null) }
    val providerName = providers.firstOrNull { it.id == assistant.providerId }?.name
        ?: providers.firstOrNull { it.id == selectedProviderId }?.name
        ?: "OpenAI"
    val lastContent = conversation.messages.lastOrNull()?.content
    val lastReasoning = conversation.messages.lastOrNull()?.reasoning
    val displayItems = remember(conversation.messages) {
        buildDesktopChatDisplayItems(conversation.messages)
    }
    val navigationItems = remember(displayItems) { buildDesktopMessageNavigationItems(displayItems) }
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
    LaunchedEffect(jumpToMessageId, jumpToMessageRequest, conversation.id) {
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
                IconButton(onClick = onMenu) { Icon(Lucide.Menu, desktopText(language, "chat.open_conversations")) }
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
                    Icon(Lucide.Ellipsis, desktopText(language, "chat.options"))
                }
                DropdownMenu(conversationMenuOpen, onDismissRequest = { conversationMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(desktopText(language, "chat.rename")) },
                        leadingIcon = { Icon(Lucide.Pencil, null, Modifier.size(18.dp)) },
                        onClick = {
                            conversationMenuOpen = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(desktopText(language, "chat.export_markdown")) },
                        leadingIcon = { Icon(Lucide.Download, null, Modifier.size(18.dp)) },
                        onClick = {
                            conversationMenuOpen = false
                            onExportConversation()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(desktopText(language, "chat.generate_title")) },
                        leadingIcon = { Icon(Lucide.Sparkles, null, Modifier.size(18.dp)) },
                        enabled = !isGenerating && conversation.messages.isNotEmpty(),
                        onClick = {
                            conversationMenuOpen = false
                            onGenerateTitle()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(desktopText(language, "chat.statistics")) },
                        enabled = conversation.messages.isNotEmpty(),
                        onClick = {
                            conversationMenuOpen = false
                            onShowStats()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(desktopText(language, "chat.generate_suggestions")) },
                        leadingIcon = { Icon(Lucide.Sparkles, null, Modifier.size(18.dp)) },
                        enabled = !isGenerating && conversation.messages.isNotEmpty(),
                        onClick = {
                            conversationMenuOpen = false
                            onGenerateSuggestions()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(desktopText(language, "chat.compress_history")) },
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
                                        desktopText(language, "chat.set_system_prompt")
                                    } else {
                                        desktopText(language, "chat.edit_system_prompt")
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
                                    desktopText(
                                        language,
                                        if (conversation.usesPromptInjections(assistant)) {
                                            "chat.disable_lorebooks"
                                        } else {
                                            "chat.enable_lorebooks"
                                        }
                                    )
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
                                        Text(
                                            desktopText(language, "chat.restore_snapshot")
                                                .replace("%d", (index + 1).toString())
                                                .replace("%s", branch.name)
                                        )
                                        Text(
                                            desktopText(language, "chat.snapshot_messages")
                                                .replace("%d", branch.messages.size.toString()),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Lucide.RotateCcw, null, Modifier.size(18.dp)) },
                                trailingIcon = {
                                    IconButton(onClick = { onDeleteBranch(branch.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Lucide.Trash2, desktopText(language, "chat.delete_snapshot"), Modifier.size(15.dp))
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
                    Icon(Lucide.Folder, desktopText(language, "chat.move_to_folder"))
                }
                FolderManagementMenu(
                    expanded = folderMenuOpen,
                    folders = folders,
                    language = language,
                    onDismiss = { folderMenuOpen = false },
                    onMoveToFolder = { folderId ->
                        folderMenuOpen = false
                        onMoveToFolder(folderId)
                    },
                    onCreateFolder = {
                        folderMenuOpen = false
                        onCreateFolder()
                    },
                    onRenameFolder = { folder ->
                        folderMenuOpen = false
                        onRenameFolder(folder)
                    },
                    onDeleteFolder = { folder ->
                        folderMenuOpen = false
                        onDeleteFolder(folder)
                    }
                )
            }
            IconButton(onClick = onNew) { Icon(Lucide.Plus, desktopText(language, "sidebar.new_chat")) }
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
                    EmptyConversation(model, assistant.quickMessages, preferences.language, onPromptChange)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().onPointerEvent(
                            PointerEventType.Scroll,
                            PointerEventPass.Main
                        ) { event ->
                            if (!preferences.enableSmoothScroll) return@onPointerEvent
                            if (event.changes.any { it.isConsumed }) return@onPointerEvent

                            val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: return@onPointerEvent
                            if (scrollDelta != 0f) {
                                event.changes.forEach { it.consume() }
                                val direction = if (scrollDelta > 0f) 1 else -1
                                val currentTimeMillis = System.currentTimeMillis()
                                if (smoothScrollDirection != direction) {
                                    smoothScrollDirection = direction
                                    smoothScrollQueue.clear()
                                }
                                val elapsedMillis = currentTimeMillis - lastSmoothScrollTimeMillis
                                val acceleration = if (elapsedMillis in 1 until SmoothScrollAccelerationDeltaMillis) {
                                    min(
                                        (1f + SmoothScrollAccelerationDeltaMillis.toFloat() / elapsedMillis) / 2f,
                                        SmoothScrollAccelerationMax
                                    )
                                } else {
                                    1f
                                }
                                lastSmoothScrollTimeMillis = currentTimeMillis
                                smoothScrollQueue += SmoothScrollImpulse(
                                    distance = scrollDelta * smoothScrollStepSize * acceleration,
                                    startTimeNanos = System.nanoTime()
                                )
                                if (smoothScrollInProgress) return@onPointerEvent

                                smoothScrollInProgress = true
                                scope.launch {
                                    try {
                                        while (smoothScrollQueue.isNotEmpty()) {
                                            val currentTimeNanos = System.nanoTime()
                                            var scrollDistance = 0f
                                            val impulses = smoothScrollQueue.iterator()
                                            while (impulses.hasNext()) {
                                                val impulse = impulses.next()
                                                val elapsed = (currentTimeNanos - impulse.startTimeNanos) / 1_000_000f
                                                val completed = elapsed >= SmoothScrollAnimationTimeMillis
                                                val progress = if (completed) 1f else {
                                                    smoothScrollPulse(elapsed / SmoothScrollAnimationTimeMillis)
                                                }
                                                val distance = (impulse.distance * progress - impulse.appliedDistance).toInt().toFloat()
                                                scrollDistance += distance
                                                impulse.appliedDistance += distance
                                                if (completed) impulses.remove()
                                            }
                                            if (scrollDistance != 0f) listState.scrollBy(scrollDistance)
                                            if (smoothScrollQueue.isNotEmpty()) delay(SmoothScrollFrameDelayMillis)
                                        }
                                    } finally {
                                        smoothScrollInProgress = false
                                    }
                                }
                            }
                        },
                        contentPadding = PaddingValues(top = 22.dp, bottom = messageBottomPadding),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        itemsIndexed(displayItems, key = { _, item -> item.key }) { _, item ->
                            val renderedItem = when (item) {
                                is DesktopChatDisplayItem.Message -> RenderedChatItem(
                                    item.messageIndex,
                                    item.messageIndex,
                                    item.message,
                                    emptyList(),
                                    false,
                                    item.message.id == highlightedMessageId,
                                )
                                is DesktopChatDisplayItem.AssistantTurn -> RenderedChatItem(
                                    item.messageIndex,
                                    item.startMessageIndex,
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
                                            editing = editingMessageIndex == renderedItem.messageIndex,
                                            onEdit = { editingMessageIndex = renderedItem.messageIndex },
                                            onCancelEdit = { editingMessageIndex = null },
                                            onSaveEdit = { content ->
                                                onSaveMessageEdit(renderedItem.messageIndex, content)
                                                editingMessageIndex = null
                                            },
                                            onDelete = { onDeleteMessage(renderedItem.startMessageIndex) },
                                            onToggleFavorite = { onToggleMessageFavorite(renderedItem.messageIndex) },
                                            onFork = { onForkAtMessage(renderedItem.messageIndex) },
                                            onTranslate = { onTranslateMessage(renderedItem.messageIndex) },
                                            highlighted = renderedItem.highlighted,
                                            onRegenerate = { onRegenerateMessage(renderedItem.startMessageIndex) },
                                            onSelectVariant = { variantIndex ->
                                                onSelectMessageVariant(renderedItem.messageIndex, variantIndex)
                                            },
                                            onAskUserAnswer = { toolCall, answer ->
                                                onAskUserAnswer(conversation.id, toolCall, answer)
                                            },
                                            onSaveMermaidImage = onSaveMermaidImage
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
                    items = navigationItems,
                    language = preferences.language,
                    onPointerOverChange = { pointerOverMessageJumper = it },
                    onMessageSelected = { messageId ->
                        highlightedMessageId = messageId
                        scope.launch {
                            delay(1_800)
                            if (highlightedMessageId == messageId) highlightedMessageId = null
                        }
                    }
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
                language = preferences.language,
                onAssistantSelect = onAssistantSelect,
                mcpServers = mcpServers,
                mcpClient = mcpClient,
                onMcpServersChange = onMcpServersChange,
                onAssistantMcpServerIdsChange = onAssistantMcpServerIdsChange,
                onMcpSettings = onMcpSettings,
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
                    TextButton(onClick = onDismissError) { Text(desktopText(language, "chat.close")) }
                }
            }
        }
        }
    }
}

@Composable
private fun FolderManagementMenu(
    expanded: Boolean,
    folders: List<DesktopFolder>,
    language: DesktopLanguage,
    onDismiss: () -> Unit,
    onMoveToFolder: (String?) -> Unit,
    onCreateFolder: () -> Unit,
    onRenameFolder: (DesktopFolder) -> Unit,
    onDeleteFolder: (DesktopFolder) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 280.dp, max = 360.dp)
    ) {
        DropdownMenuItem(
            text = { Text(desktopText(language, "chat.new_folder")) },
            leadingIcon = { Icon(Lucide.Plus, null, Modifier.size(18.dp)) },
            onClick = onCreateFolder
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(desktopText(language, "chat.uncategorized")) },
            leadingIcon = { Icon(Lucide.Folder, null, Modifier.size(18.dp)) },
            onClick = { onMoveToFolder(null) }
        )
        folders.forEach { folder ->
            DropdownMenuItem(
                text = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Lucide.Folder, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    Row {
                        IconButton(
                            onClick = { onRenameFolder(folder) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Lucide.Pencil,
                                desktopText(language, "sidebar.rename_folder"),
                                Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteFolder(folder) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Lucide.Trash2,
                                desktopText(language, "message.delete"),
                                Modifier.size(16.dp)
                            )
                        }
                    }
                },
                onClick = { onMoveToFolder(folder.id) }
            )
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
    items: List<DesktopMessageNavigationItem>,
    language: DesktopLanguage,
    onPointerOverChange: (Boolean) -> Unit,
    onMessageSelected: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val alignment = if (onLeft) Alignment.CenterStart else Alignment.CenterEnd
    val color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
    val horizontalOffset: (Int) -> Int = { width -> if (onLeft) -width else width }
    val messageCount = items.size
    val currentMessage = state.firstVisibleItemIndex.coerceIn(0, messageCount.coerceAtLeast(1) - 1) + 1
    var previewOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var pointerOverControls by remember { mutableStateOf(false) }
    val filteredItems = remember(items, query) { items.filterForNavigation(query) }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(alignment)
            .padding(horizontal = 12.dp)
            .onPointerEvent(PointerEventType.Enter) {
                pointerOverControls = true
                onPointerOverChange(true)
            }
            .onPointerEvent(PointerEventType.Exit) {
                pointerOverControls = false
                if (!previewOpen) onPointerOverChange(false)
            },
        enter = fadeIn(tween(180)) + slideInHorizontally(tween(180), initialOffsetX = horizontalOffset),
        exit = fadeOut(tween(220)) + slideOutHorizontally(tween(220), targetOffsetX = horizontalOffset)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MessageJumperButton(Lucide.ArrowUpToLine, desktopText(language, "jumper.top"), color) {
                scope.launch { state.animateScrollToItem(0) }
            }
            MessageJumperButton(Lucide.ArrowUp, desktopText(language, "jumper.previous"), color) {
                scope.launch { state.animateScrollToPreviousMessage() }
            }
            Box {
                Surface(
                    onClick = { previewOpen = true },
                    modifier = Modifier.size(width = 40.dp, height = 28.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = color,
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$currentMessage/$messageCount", fontSize = 10.sp)
                    }
                }
                DropdownMenu(
                    expanded = previewOpen,
                    onDismissRequest = {
                        previewOpen = false
                        onPointerOverChange(pointerOverControls)
                    },
                    offset = if (onLeft) DpOffset(48.dp, 0.dp) else DpOffset((-348).dp, 0.dp),
                    modifier = Modifier.width(340.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        singleLine = true,
                        placeholder = { Text(desktopText(language, "jumper.search_messages")) },
                        leadingIcon = { Icon(Lucide.Search, null, Modifier.size(18.dp)) }
                    )
                    if (filteredItems.isEmpty()) {
                        Text(
                            desktopText(language, "jumper.no_matching_messages"),
                            Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    } else {
                        Column(
                            Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())
                        ) {
                            filteredItems.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    desktopText(language, if (item.role == "user") "jumper.user" else "jumper.assistant"),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    MessageTimeFormatter.format(Instant.ofEpochMilli(item.createdAt)),
                                                    Modifier.padding(start = 8.dp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                item.summary.ifBlank { desktopText(language, "jumper.empty_message") },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 13.sp
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (item.role == "user") Lucide.UserRound else Lucide.Bot,
                                            null,
                                            Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        previewOpen = false
                                        onPointerOverChange(pointerOverControls)
                                        onMessageSelected(item.messageId)
                                        scope.launch { state.animateScrollToItem(item.displayIndex) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            MessageJumperButton(Lucide.ArrowDown, desktopText(language, "jumper.next"), color) {
                scope.launch {
                    state.animateScrollToItem((state.firstVisibleItemIndex + 1).coerceAtMost(messageCount))
                }
            }
            MessageJumperButton(Lucide.ArrowDownToLine, desktopText(language, "jumper.bottom"), color) {
                scope.launch { state.animateScrollToItem(messageCount) }
            }
        }
    }
}

private suspend fun LazyListState.animateScrollToPreviousMessage() {
    val previousIndex = (firstVisibleItemIndex - 1).coerceAtLeast(0)
    animateScrollToItem(previousIndex)

    val previousItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == previousIndex } ?: return
    val visibleHeight = layoutInfo.viewportEndOffset - previousItem.offset
    val remainingHeight = (previousItem.size - visibleHeight).coerceAtLeast(0)
    if (remainingHeight > 0) animateScrollBy(remainingHeight.toFloat())
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
    language: DesktopLanguage,
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
        Text(desktopText(language, "empty_chat.greeting"), fontSize = 24.sp, fontWeight = FontWeight.Medium)
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
    editing: Boolean,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onFork: () -> Unit,
    onTranslate: () -> Unit,
    highlighted: Boolean,
    onRegenerate: () -> Unit,
    onSelectVariant: (Int) -> Unit,
    onAskUserAnswer: (DesktopToolCall, String) -> Unit,
    onSaveMermaidImage: (MermaidRenderResult) -> Unit
) {
    val isUser = message.role == "user"
    val language = preferences.language
    val configuredUserNickname = preferences.userNickname.trim()
    val userNickname = configuredUserNickname.ifBlank { desktopText(language, "user.you") }
    val hasVisibleExecutionSteps = executionSteps.any {
        it !is DesktopExecutionStep.Reasoning || preferences.showReasoning
    }
    val displayContent = assistant.applyRegexRules(message.content, message.role, visualOnly = true)
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    var editedContent by remember(message.id, message.content) { mutableStateOf(message.content) }
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
        enableChineseTypography = preferences.enableChineseTypography,
        enableMermaidRendering = preferences.enableMermaidRendering,
        enableMermaidCli = preferences.enableMermaidCli,
        mermaidCliPath = preferences.mermaidCliPath,
        mermaidUseSystemBrowser = preferences.mermaidUseSystemBrowser,
        language = preferences.language,
        onSaveMermaidImage = onSaveMermaidImage
    )
    val highlightAlpha by animateFloatAsState(
        targetValue = if (highlighted) 0.45f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "messageHighlightAlpha"
    )
    Column(
        Modifier.fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = highlightAlpha),
                RoundedCornerShape(8.dp)
            )
            // Keep the highlight inset stable so fading the background never reflows message content.
            .padding(8.dp),
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
                                    Icon(Lucide.UserRound, desktopText(language, "message.user_avatar"), Modifier.size(16.dp))
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
                if (editing) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editedContent,
                            onValueChange = { editedContent = it },
                            modifier = Modifier.widthIn(min = 300.dp, max = 630.dp),
                            minLines = 3,
                            maxLines = 12,
                            placeholder = { Text(desktopText(language, "message.edit")) }
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onCancelEdit) { Text(desktopText(language, "common.cancel")) }
                            Button(
                                onClick = { onSaveEdit(editedContent.trim()) },
                                enabled = editedContent.isNotBlank()
                            ) { Text(desktopText(language, "common.submit")) }
                        }
                    }
                } else {
                    MarkdownContent(
                        displayContent,
                        Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        markdownOptions
                    )
                }
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
                    language = language,
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
                    Text(desktopText(language, "message.thinking"), Modifier.padding(start = 9.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    AttachmentPreview(attachment, language)
                }
            }
        }
        if (message.translation.isNotBlank()) {
            TranslationBlock(
                messageId = message.id,
                translation = message.translation,
                targetLanguage = message.translationTargetLanguage,
                language = language,
                markdownOptions = markdownOptions
            )
        }
        if (!isUser && message.citations.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(desktopText(language, "message.sources"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
                                    citation.title.ifBlank {
                                        desktopText(language, "message.source_number").replace("%d", (index + 1).toString())
                                    },
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
                        desktopText(language, "message.input_tokens"),
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
                        desktopText(language, "message.output_tokens"),
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
                        desktopText(language, "message.cached_tokens"),
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
        if (!editing) Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            val variants = message.availableVariants()
            val currentVariantIndex = message.selectedVariantIndex.coerceIn(variants.indices)
            if (variants.size > 1) {
                MessageAction(
                    Lucide.ChevronLeft,
                    desktopText(language, "message.previous_version"),
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
                    desktopText(language, "message.next_version"),
                    enabled = actionsEnabled && currentVariantIndex < variants.lastIndex
                ) { onSelectVariant(currentVariantIndex + 1) }
            }
            MessageAction(if (copied) Lucide.Check else Lucide.Copy, desktopText(language, "message.copy"), enabled = displayContent.isNotEmpty()) {
                clipboardScope.launch {
                    clipboard.setClipEntry(ClipEntry(StringSelection(displayContent)))
                    copyVersion++
                }
            }
            MessageAction(Lucide.GitFork, desktopText(language, "message.fork"), enabled = actionsEnabled, onClick = onFork)
            if (!isUser) {
                MessageAction(Lucide.RotateCcw, desktopText(language, "message.regenerate"), enabled = actionsEnabled, onClick = onRegenerate)
            }
            MessageAction(Lucide.Languages, desktopText(language, "message.translate"), enabled = actionsEnabled && displayContent.isNotBlank(), onClick = onTranslate)
            MessageAction(
                if (message.isFavorite) FilledStar else Lucide.Star,
                desktopText(language, if (message.isFavorite) "message.unfavorite" else "message.favorite"),
                enabled = actionsEnabled,
                onClick = onToggleFavorite
            )
            if (isUser) {
                MessageAction(Lucide.Pencil, desktopText(language, "message.edit"), enabled = actionsEnabled, onClick = onEdit)
            }
            MessageAction(Lucide.Trash2, desktopText(language, "message.delete"), enabled = actionsEnabled, onClick = onDelete)
        }
    }
}

@Composable
private fun AttachmentPreview(attachment: DesktopAttachment, language: DesktopLanguage) {
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
                    if (attachment.kind == DesktopAttachmentKind.AUDIO) {
                        "${desktopText(language, "attachment.audio")} · ${attachment.name}"
                    } else attachment.name,
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
    language: DesktopLanguage,
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (canCollapse) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (expanded) Lucide.ChevronDown else Lucide.ChevronRight,
                        desktopText(language, if (expanded) "timeline.collapse_execution" else "timeline.expand_execution"),
                        Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (expanded) {
                            desktopText(language, "timeline.execution")
                        } else {
                            desktopText(language, "timeline.expand_steps")
                                .replace("%d", (visibleSteps.size - renderedSteps.size).toString())
                        },
                        Modifier.padding(start = 7.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            AnimatedContent(
                targetState = renderedSteps,
                transitionSpec = {
                    (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.96f)) togetherWith
                        (fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.96f))
                },
                label = "executionSteps"
            ) { currentSteps ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    currentSteps.forEachIndexed { index, step ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                            )
                        }
                        DesktopExecutionTimelineStep(
                            step = step,
                            generating = generating,
                            language = language,
                            markdownOptions = markdownOptions,
                            onAskUserAnswer = onAskUserAnswer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopExecutionTimelineStep(
    step: DesktopExecutionStep,
    generating: Boolean,
    language: DesktopLanguage,
    markdownOptions: MarkdownRenderOptions,
    onAskUserAnswer: (DesktopToolCall, String) -> Unit,
) {
    when (step) {
        is DesktopExecutionStep.Reasoning -> {
            var expanded by remember(step.message.id) { mutableStateOf(generating) }
            val message = step.message
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.Lightbulb, desktopText(language, "timeline.reasoning"), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    message.reasoningDurationMillis?.let {
                        desktopText(language, "timeline.reasoning_duration").replace("%s", formatReasoningDuration(it))
                    } ?: desktopText(language, "timeline.reasoning"),
                    Modifier.padding(start = 7.dp).weight(1f),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(if (expanded) Lucide.ChevronDown else Lucide.ChevronRight, null, Modifier.size(15.dp))
            }
            DesktopExecutionStepDetails(expanded) {
                MarkdownContent(
                    message.reasoning,
                    Modifier.fillMaxWidth().padding(start = 31.dp, bottom = 8.dp),
                    markdownOptions
                )
            }
        }

        is DesktopExecutionStep.ToolCall -> {
            if (step.call.name == DesktopAskUserToolName && step.result == null) {
                AskUserToolStep(step.call, language, onAskUserAnswer)
            } else {
                DesktopToolCallTimelineStep(
                    toolCall = step.call,
                    result = step.result,
                    generating = generating && step.result == null,
                    language = language,
                    markdownOptions = markdownOptions,
                )
            }
        }

        is DesktopExecutionStep.ToolResult -> DesktopToolResultTimelineStep(step.message.content, language, markdownOptions)
    }
}

@Composable
private fun DesktopToolCallTimelineStep(
    toolCall: DesktopToolCall,
    result: ChatMessage?,
    generating: Boolean,
    language: DesktopLanguage,
    markdownOptions: MarkdownRenderOptions,
) {
    var expanded by remember(toolCall.id) { mutableStateOf(false) }
    val input = toolCall.arguments.takeIf { it.isNotBlank() && it != "{}" }
    val output = result?.content?.takeIf { it.isNotBlank() }
    val hasDetails = input != null || output != null
    val status = desktopText(language, if (generating && result == null) "tool.calling" else "tool.completed")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = hasDetails) { expanded = !expanded }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Lucide.Wrench, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            "${toolCall.displayName(language)} · $status",
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
    DesktopExecutionStepDetails(expanded && hasDetails) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 31.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            input?.let { value ->
                DesktopToolDetail(desktopText(language, "tool.input")) {
                    Text(value, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
            output?.let { value ->
                DesktopToolDetail(desktopText(language, "tool.output")) {
                    MarkdownContent(value, Modifier.fillMaxWidth(), markdownOptions)
                }
            }
        }
    }
}

@Composable
private fun DesktopExecutionStepDetails(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(220, easing = FastOutSlowInEasing), expandFrom = Alignment.Top) +
            fadeIn(tween(150)) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.98f),
        exit = shrinkVertically(tween(180, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Top) +
            fadeOut(tween(120)) + scaleOut(tween(180, easing = FastOutSlowInEasing), targetScale = 0.98f),
        label = "executionStepDetails"
    ) {
        content()
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
private fun DesktopToolResultTimelineStep(content: String, language: DesktopLanguage, markdownOptions: MarkdownRenderOptions) {
    var expanded by remember(content) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Lucide.Wrench, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            desktopText(language, "tool.result"),
            Modifier.padding(start = 7.dp).weight(1f),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(if (expanded) Lucide.ChevronDown else Lucide.ChevronRight, null, Modifier.size(15.dp))
    }
    DesktopExecutionStepDetails(expanded && content.isNotBlank()) {
        Box(Modifier.fillMaxWidth().padding(start = 31.dp, bottom = 8.dp)) {
            DesktopToolDetail(desktopText(language, "tool.output")) {
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
private fun AskUserToolStep(
    toolCall: DesktopToolCall,
    language: DesktopLanguage,
    onSubmit: (DesktopToolCall, String) -> Unit
) {
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
            Text(desktopText(language, "tool.answer_required"), fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                Text(desktopText(language, "common.submit"))
            }
        }
    }
}

private fun formatReasoningDuration(durationMillis: Long): String =
    String.format(java.util.Locale.getDefault(), "%.1f", durationMillis.coerceAtLeast(0) / 1_000.0)

private fun DesktopToolCall.displayName(language: DesktopLanguage): String = when (name) {
    DesktopWebSearchToolName -> desktopText(language, "tool.web_search")
    DesktopCurrentTimeToolName -> desktopText(language, "tool.current_time")
    DesktopMemoryToolName -> desktopText(language, "tool.memory")
    DesktopAgentListFilesToolName -> desktopText(language, "tool.list_files")
    DesktopAgentSearchFilesToolName -> desktopText(language, "tool.search_files")
    DesktopAgentReadFileToolName -> desktopText(language, "tool.read_file")
    DesktopAgentWriteFileToolName -> desktopText(language, "tool.write_file")
    DesktopAgentEditFileToolName -> desktopText(language, "tool.edit_file")
    DesktopAgentShellToolName -> desktopText(language, "tool.run_command")
    DesktopUseSkillToolName -> desktopText(language, "tool.use_skill")
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
    language: DesktopLanguage,
    markdownOptions: MarkdownRenderOptions
) {
    var expanded by remember(messageId) { mutableStateOf(true) }
    val title = desktopText(language, "message.translation") +
        targetLanguage.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()

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
                    desktopText(language, if (expanded) "common.collapse" else "common.expand") + title,
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
private fun ModelPickerMenu(
    providers: List<DesktopProviderProfile>,
    selectedProviderId: String,
    selectedModel: String,
    reasoningEffort: String,
    language: DesktopLanguage,
    client: OpenAiClient,
    onSelect: (String, String) -> Unit,
    onReasoningEffortChange: (String) -> Unit,
    onSettings: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var reasoningSliderValue by remember(reasoningEffort) {
        mutableFloatStateOf(listOf("", "low", "medium", "high").indexOf(reasoningEffort).coerceAtLeast(0).toFloat())
    }
    val balances = remember { mutableStateMapOf<String, String>() }
    val loadingBalanceIds = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    val filteredProviders = providers.mapNotNull { provider ->
        val models = (provider.discoveredModels + provider.config.model)
            .filter { it.isNotBlank() && it.contains(query.trim(), ignoreCase = true) }
            .distinct()
        models.takeIf { it.isNotEmpty() }?.let { provider to it }
    }

    fun refreshBalance(provider: DesktopProviderProfile, forceRefresh: Boolean = false) {
        if (provider.config.balanceOptions.enabled != true || loadingBalanceIds[provider.id] == true) return
        loadingBalanceIds[provider.id] = true
        scope.launch {
            balances[provider.id] = runCatching {
                desktopText(language, "model_picker.balance")
                    .replace("%s", client.getCachedBalance(provider.config, forceRefresh = forceRefresh))
            }.getOrElse { desktopText(language, "model_picker.balance_failed") }
            loadingBalanceIds.remove(provider.id)
        }
    }
    LaunchedEffect(providers) {
        providers.filter { it.config.balanceOptions.enabled }.forEach(::refreshBalance)
    }

    Column(
        Modifier.widthIn(min = 300.dp, max = 380.dp).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(desktopText(language, "model_picker.title"), Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = onSettings, modifier = Modifier.size(30.dp)) {
                Icon(Lucide.Settings, desktopText(language, "model_picker.manage_providers"), Modifier.size(16.dp))
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(desktopText(language, "model_picker.filter")) },
            singleLine = true
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(desktopText(language, "model_picker.reasoning"), Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                listOf("model_picker.default", "model_picker.low", "model_picker.medium", "model_picker.high")
                    .map { desktopText(language, it) }[reasoningSliderValue.roundToInt().coerceIn(0, 3)],
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = reasoningSliderValue,
            onValueChange = { reasoningSliderValue = it },
            onValueChangeFinished = {
                onReasoningEffortChange(listOf("", "low", "medium", "high")[reasoningSliderValue.roundToInt().coerceIn(0, 3)])
            },
            valueRange = 0f..3f,
            steps = 2
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("model_picker.default", "model_picker.low", "model_picker.medium", "model_picker.high").forEach { key ->
                Text(desktopText(language, key), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider()
        Column(
            Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            filteredProviders.forEachIndexed { providerIndex, (provider, models) ->
                if (providerIndex > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DesktopProviderIcon(provider.name, Modifier.size(16.dp))
                    Text(provider.name, Modifier.padding(start = 6.dp).weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    if (provider.config.balanceOptions.enabled) {
                        Text(
                            balances[provider.id] ?: desktopText(language, "model_picker.checking_balance"),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        IconButton(
                            enabled = loadingBalanceIds[provider.id] != true,
                            onClick = { refreshBalance(provider, forceRefresh = true) },
                            modifier = Modifier.padding(start = 2.dp).size(24.dp)
                        ) {
                            if (loadingBalanceIds[provider.id] == true) {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Lucide.RotateCcw,
                                    desktopText(language, "model_picker.refresh_balance").replace("%s", provider.name),
                                    Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                models.forEach { availableModel ->
                    val selected = provider.id == selectedProviderId && availableModel == selectedModel
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(provider.id, availableModel) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(availableModel, Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                if (selected) Icon(Lucide.Sparkles, desktopText(language, "model_picker.current_model"), Modifier.size(14.dp), MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                modelCapabilityLabels(availableModel, language).joinToString(" · "),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun modelCapabilityLabels(model: String, language: DesktopLanguage): List<String> {
    val normalized = model.lowercase()
    return buildList {
        add(desktopText(language, "model_capability.text"))
        if (listOf("gpt-4o", "gpt-4.1", "gemini", "claude", "qwen-vl", "vision").any(normalized::contains)) add(desktopText(language, "model_capability.vision"))
        if (listOf("o1", "o3", "r1", "reasoner", "thinking", "deepseek-r").any(normalized::contains)) add(desktopText(language, "model_capability.reasoning"))
        if (!listOf("embedding", "image", "tts", "whisper").any(normalized::contains)) add(desktopText(language, "model_capability.tools"))
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
    language: DesktopLanguage,
    onAssistantSelect: (String) -> Unit,
    mcpServers: List<DesktopMcpServer>,
    mcpClient: DesktopMcpClient,
    onMcpServersChange: (List<DesktopMcpServer>) -> Unit,
    onAssistantMcpServerIdsChange: (Set<String>) -> Unit,
    onMcpSettings: () -> Unit,
    onToggleWebSearch: () -> Unit,
    onQuickMessageSelect: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionSelect: (String) -> Unit,
    hazeState: HazeState
) {
    var modelMenuOpen by remember { mutableStateOf(false) }
    var assistantMenuOpen by remember { mutableStateOf(false) }
    var quickMessageMenuOpen by remember { mutableStateOf(false) }
    var mcpMenuOpen by remember { mutableStateOf(false) }
    var composerExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val inputFocusRequester = remember { FocusRequester() }
    val composerShape = RoundedCornerShape(if (composerExpanded) 16.dp else 24.dp)
    val glassSurface = MaterialTheme.colorScheme.surface
    LaunchedEffect(composerExpanded) {
        if (composerExpanded) inputFocusRequester.requestFocus()
    }
    Box(
        modifier
            .animateContentSize(animationSpec = tween(280, easing = FastOutSlowInEasing))
            .then(
                if (composerExpanded) {
                    Modifier.fillMaxSize().padding(16.dp)
                } else {
                    Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .then(if (composerExpanded) Modifier.fillMaxHeight() else Modifier.widthIn(max = 920.dp))
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
                Column(
                    Modifier
                        .then(if (composerExpanded) Modifier.fillMaxSize() else Modifier)
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
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
                                            "${desktopText(language, "attachment.audio")} · ${attachment.name}"
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
                                    ) { Icon(Lucide.Trash2, desktopText(language, "attachment.remove"), Modifier.size(14.dp)) }
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
                        .then(
                            if (composerExpanded) {
                                Modifier.weight(1f)
                            } else {
                                Modifier.heightIn(min = 58.dp, max = 150.dp)
                            }
                        )
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
                    placeholder = { Text(desktopText(language, "composer.placeholder")) },
                    trailingIcon = {
                        IconButton(onClick = { composerExpanded = !composerExpanded }) {
                            Icon(
                                if (composerExpanded) Lucide.Minimize2 else Lucide.Maximize2,
                                desktopText(language, if (composerExpanded) "composer.collapse" else "composer.expand"),
                                Modifier.size(18.dp)
                            )
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
                    minLines = if (composerExpanded) 12 else 2,
                    maxLines = if (composerExpanded) Int.MAX_VALUE else 6
                )
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddAttachments, modifier = Modifier.size(34.dp)) {
                        Icon(Lucide.Paperclip, desktopText(language, "composer.add_attachment"), Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleWebSearch, modifier = Modifier.size(34.dp)) {
                        Icon(
                            Lucide.Globe,
                            desktopText(language, if (webSearchEnabled) "composer.disable_web_search" else "composer.enable_web_search"),
                            Modifier.size(18.dp),
                            tint = if (webSearchEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Box {
                        IconButton(onClick = { mcpMenuOpen = true }, modifier = Modifier.size(34.dp)) {
                            Icon(
                                Lucide.Wrench,
                                desktopText(language, "mcp.manage"),
                                Modifier.size(18.dp),
                                tint = if (mcpServers.any { it.enabled && it.id in assistant.mcpServerIds }) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        DropdownMenu(
                            expanded = mcpMenuOpen,
                            onDismissRequest = { mcpMenuOpen = false }
                        ) {
                            McpQuickManager(
                                servers = mcpServers,
                                selectedServerIds = assistant.mcpServerIds,
                                mcpClient = mcpClient,
                                language = language,
                                onServersChange = onMcpServersChange,
                                onSelectedServerIdsChange = onAssistantMcpServerIdsChange,
                                onOpenSettings = {
                                    mcpMenuOpen = false
                                    onMcpSettings()
                                }
                            )
                        }
                    }
                    if (assistant.quickMessages.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { quickMessageMenuOpen = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Lucide.Plus, desktopText(language, "composer.quick_messages"), Modifier.size(18.dp))
                            }
                            DropdownMenu(quickMessageMenuOpen, onDismissRequest = { quickMessageMenuOpen = false }) {
                                assistant.quickMessages.forEach { message ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(message.title.ifBlank { desktopText(language, "common.unnamed") }, fontSize = 13.sp)
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
                                text = { Text(desktopText(language, "composer.manage_assistants")) },
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
                        DropdownMenu(
                            expanded = modelMenuOpen,
                            onDismissRequest = { modelMenuOpen = false }
                        ) {
                            ModelPickerMenu(
                                providers = providers,
                                selectedProviderId = selectedProviderId,
                                selectedModel = model,
                                reasoningEffort = assistant.reasoningEffort,
                                language = language,
                                client = client,
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
                                    desktopText(language, "composer.stop_generation"),
                                    Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    Lucide.Send,
                                    desktopText(language, "composer.send"),
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
}

@Composable
private fun McpQuickManager(
    servers: List<DesktopMcpServer>,
    selectedServerIds: Set<String>,
    mcpClient: DesktopMcpClient,
    language: DesktopLanguage,
    onServersChange: (List<DesktopMcpServer>) -> Unit,
    onSelectedServerIdsChange: (Set<String>) -> Unit,
    onOpenSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val availability = remember { mutableStateMapOf<String, McpAvailability>() }

    fun canCheck(server: DesktopMcpServer): Boolean =
        server.name.matches(Regex("[A-Za-z0-9]+")) &&
            if (server.transport == DesktopMcpTransport.STDIO) server.command.isNotBlank() else server.url.isNotBlank()

    fun check(server: DesktopMcpServer) {
        if (!canCheck(server) || availability[server.id] == McpAvailability.CHECKING) return
        availability[server.id] = McpAvailability.CHECKING
        scope.launch {
            runCatching { mcpClient.syncTools(server) }
                .onSuccess { tools ->
                    availability[server.id] = McpAvailability.AVAILABLE
                    val existingTools = server.tools.associateBy { it.name }
                    onServersChange(servers.map { candidate ->
                        if (candidate.id == server.id) {
                            candidate.copy(tools = tools.map { tool ->
                                tool.copy(enabled = existingTools[tool.name]?.enabled ?: true)
                            })
                        } else {
                            candidate
                        }
                    })
                }
                .onFailure { availability[server.id] = McpAvailability.UNAVAILABLE }
        }
    }

    Column(
        Modifier.widthIn(min = 300.dp, max = 380.dp).padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(desktopText(language, "mcp.title"), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    desktopText(language, "mcp.services_for_assistant")
                        .replace("%d", servers.count { it.enabled && it.id in selectedServerIds }.toString()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            TextButton(
                onClick = { servers.forEach(::check) },
                enabled = servers.any(::canCheck)
            ) { Text(desktopText(language, "mcp.check_all")) }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
                Icon(Lucide.Settings, desktopText(language, "mcp.open_settings"), Modifier.size(17.dp))
            }
        }
        if (servers.isEmpty()) {
            Text(
                desktopText(language, "mcp.no_servers"),
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        servers.forEachIndexed { index, server ->
            if (index > 0) HorizontalDivider()
            val state = availability[server.id] ?: McpAvailability.UNKNOWN
            val status = when (state) {
                McpAvailability.UNKNOWN -> if (server.tools.isEmpty()) desktopText(language, "mcp.not_checked") else {
                    desktopText(language, "mcp.tools_synced").replace("%d", server.tools.size.toString())
                }
                McpAvailability.CHECKING -> desktopText(language, "mcp.checking")
                McpAvailability.AVAILABLE -> desktopText(language, "mcp.available_tools").replace("%d", server.tools.size.toString())
                McpAvailability.UNAVAILABLE -> desktopText(language, "mcp.unavailable")
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(server.name.ifBlank { desktopText(language, "mcp.unnamed_service") }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            status,
                            color = when (state) {
                                McpAvailability.AVAILABLE -> MaterialTheme.colorScheme.primary
                                McpAvailability.UNAVAILABLE -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = { check(server) },
                        enabled = canCheck(server) && state != McpAvailability.CHECKING,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (state == McpAvailability.CHECKING) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                    Icon(Lucide.RotateCcw, desktopText(language, "mcp.check"), Modifier.size(16.dp))
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(desktopText(language, "mcp.current_assistant"), Modifier.weight(1f), fontSize = 12.sp)
                    Switch(
                        checked = server.id in selectedServerIds,
                        onCheckedChange = { selected ->
                            onSelectedServerIdsChange(
                                if (selected) selectedServerIds + server.id else selectedServerIds - server.id
                            )
                        }
                    )
                    Text(desktopText(language, "mcp.enabled"), Modifier.padding(start = 10.dp, end = 6.dp), fontSize = 12.sp)
                    Switch(
                        checked = server.enabled,
                        onCheckedChange = { enabled ->
                            onServersChange(servers.map { candidate ->
                                if (candidate.id == server.id) candidate.copy(enabled = enabled) else candidate
                            })
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TextEditDialog(
    title: String,
    initialValue: String,
    language: DesktopLanguage = DesktopLanguage.SYSTEM,
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
            Button(onClick = { onSave(value) }, enabled = allowBlank || value.isNotBlank()) {
                Text(desktopText(language, "common.save"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(desktopText(language, "common.cancel")) } }
    )
}
