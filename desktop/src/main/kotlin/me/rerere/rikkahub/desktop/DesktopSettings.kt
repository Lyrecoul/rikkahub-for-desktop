package me.rerere.rikkahub.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Bot
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CircleX
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Keyboard
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.MessageSquareText
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.ServerCog
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.Upload
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

internal enum class DesktopSettingsSection(
    val labelKey: String,
    val icon: ImageVector
) {
    GENERAL("settings.section.general", Lucide.Palette),
    MESSAGE_DISPLAY("settings.section.messages", Lucide.MessageSquareText),
    INTERACTION("settings.section.interaction", Lucide.Keyboard),
    DATA("settings.section.data", Lucide.Save),
    ASSISTANTS("settings.section.assistants", Lucide.Bot),
    PROVIDERS("settings.section.providers", Lucide.ServerCog)
}

@Composable
internal fun DesktopSettingsPane(
    providers: List<DesktopProviderProfile>,
    selectedProviderId: String,
    assistants: List<DesktopAssistantProfile>,
    selectedAssistantId: String,
    preferences: DesktopPreferences,
    globalMemories: List<DesktopMemory>,
    webSearchSettings: DesktopWebSearchSettings,
    client: OpenAiClient,
    mcpServers: List<DesktopMcpServer>,
    mcpClient: DesktopMcpClient,
    initialSection: DesktopSettingsSection,
    showMenu: Boolean,
    onMenu: () -> Unit,
    onBack: () -> Unit,
    onProviderSelect: (String) -> Unit,
    onProviderSave: (DesktopProviderProfile) -> Unit,
    onProviderAdd: () -> Unit,
    onProviderDelete: (String) -> Unit,
    onAssistantSelect: (String) -> Unit,
    onAssistantSave: (DesktopAssistantProfile) -> Unit,
    onAssistantAdd: () -> Unit,
    onAssistantCopy: (String) -> Unit,
    onAssistantDelete: (String) -> Unit,
    onExportData: () -> String?,
    onImportData: () -> String?,
    onResetData: () -> Unit,
    onWebSearchSettingsChange: (DesktopWebSearchSettings) -> Unit,
    onMcpServersChange: (List<DesktopMcpServer>) -> Unit,
    onPreferencesChange: (DesktopPreferences) -> Unit,
    onGlobalMemoriesChange: (List<DesktopMemory>) -> Unit
) {
    val selectedProvider = providers.firstOrNull { it.id == selectedProviderId } ?: providers.first()
    val selectedAssistant = assistants.firstOrNull { it.id == selectedAssistantId } ?: assistants.first()
    var draftProvider by remember(selectedProvider) { mutableStateOf(selectedProvider) }
    var draftAssistant by remember(selectedAssistant) { mutableStateOf(selectedAssistant) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var connectionState by remember(selectedProvider.id) {
        mutableStateOf<ConnectionState>(ConnectionState.Idle)
    }
    var modelMenuOpen by remember(selectedProvider.id) { mutableStateOf(false) }
    var providerPresetMenuOpen by remember(selectedProvider.id) { mutableStateOf(false) }
    var assistantProviderMenuOpen by remember(selectedAssistant.id) { mutableStateOf(false) }
    var assistantModelMenuOpen by remember(selectedAssistant.id) { mutableStateOf(false) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    var webSearchTestStatus by remember { mutableStateOf<String?>(null) }
    var webSearchTesting by remember { mutableStateOf(false) }
    val feedbackHostState = remember { SnackbarHostState() }
    var balanceStatus by remember(selectedProvider.id) { mutableStateOf<String?>(null) }
    var balanceStatusIsError by remember(selectedProvider.id) { mutableStateOf(false) }
    var resetConfirmationOpen by remember { mutableStateOf(false) }
    val messageTemplateValid = remember(draftAssistant.messageTemplate) {
        validateMessageTemplate(draftAssistant.messageTemplate).isSuccess
    }
    val regexRulesValid = remember(draftAssistant.regexRules) {
        draftAssistant.regexRules.all { rule ->
            rule.findRegex.isNotBlank() && runCatching { Regex(rule.findRegex) }.isSuccess
        }
    }
    val memoriesValid = remember(draftAssistant.memories) {
        draftAssistant.memories.all { it.content.isNotBlank() }
    }
    val injectionsValid = remember(draftAssistant.promptInjections) {
        draftAssistant.promptInjections.all { injection ->
            injection.content.isNotBlank() && (injection.constantActive || injection.keywords.isNotEmpty()) &&
                (!injection.useRegex || injection.keywords.all { runCatching { Regex(it) }.isSuccess })
        }
    }
    val assistantBodiesValid = remember(draftAssistant.customHeaders, draftAssistant.customBodies) {
        draftAssistant.customHeaders.all { it.name.isNotBlank() } && draftAssistant.customBodies.all { body ->
            body.key.isNotBlank() && runCatching { Json.parseToJsonElement(body.value) }.isSuccess
        }
    }
    val providerBodiesValid = remember(
        draftProvider.config.customHeaders,
        draftProvider.config.customBodies
    ) {
        draftProvider.config.customHeaders.all { it.name.isNotBlank() } && draftProvider.config.customBodies.all { body ->
            body.key.isNotBlank() && runCatching { Json.parseToJsonElement(body.value) }.isSuccess
        }
    }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val sectionTopPadding = with(LocalDensity.current) { 22.dp.toPx() }
    val sectionCoordinates = remember { mutableMapOf<DesktopSettingsSection, LayoutCoordinates>() }
    var scrollViewportCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var sectionAnchorsReady by remember { mutableStateOf(false) }
    var scrollViewportHeight by remember { mutableIntStateOf(0) }
    val settingsScrollbarAdapter = remember(scrollState, scrollViewportHeight) {
        object : ScrollbarAdapter {
            override val scrollOffset: Double get() = scrollState.value.toDouble()
            override val viewportSize: Double get() = scrollViewportHeight.toDouble()
            override val contentSize: Double get() = scrollState.maxValue + viewportSize

            override suspend fun scrollTo(scrollOffset: Double) {
                scrollState.scrollTo(scrollOffset.roundToInt())
            }
        }
    }
    var activeSection by remember { mutableStateOf(initialSection) }
    var sectionNavigationJob by remember { mutableStateOf<Job?>(null) }

    suspend fun scrollToSection(section: DesktopSettingsSection) {
        val sectionCoordinates = sectionCoordinates[section] ?: return
        val viewportCoordinates = scrollViewportCoordinates ?: return
        val target = scrollState.value + sectionCoordinates.positionInRoot().y -
            viewportCoordinates.positionInRoot().y - sectionTopPadding
        scrollState.animateScrollTo(target.roundToInt().coerceIn(0, scrollState.maxValue))
    }

    LaunchedEffect(initialSection, scrollViewportCoordinates, sectionAnchorsReady) {
        activeSection = initialSection
        sectionNavigationJob?.cancel()
        sectionNavigationJob = launch {
            scrollToSection(initialSection)
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = if (showMenu) onMenu else onBack) {
                Icon(if (showMenu) Lucide.Menu else Lucide.ArrowLeft, desktopText(preferences.language, "settings.back"))
            }
            Column(Modifier.padding(start = 4.dp)) {
                Text(desktopText(preferences.language, "settings.title"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(
                    desktopText(preferences.language, "settings.subtitle"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

        Row(Modifier.fillMaxSize()) {
            if (!showMenu) {
                DesktopSettingsNavigation(
                    activeSection = activeSection,
                    language = preferences.language,
                    onSectionClick = { section ->
                        activeSection = section
                        sectionNavigationJob?.cancel()
                        sectionNavigationJob = scope.launch {
                            scrollToSection(section)
                        }
                    }
                )
                Box(
                    Modifier.fillMaxHeight().width(1.dp).background(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                )
            }

            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .onSizeChanged { scrollViewportHeight = it.height }
                    .onGloballyPositioned { scrollViewportCoordinates = it },
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().widthIn(max = 880.dp)
                        .verticalScroll(scrollState)
                        .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    Modifier.onGloballyPositioned { coordinates ->
                        sectionCoordinates[DesktopSettingsSection.GENERAL] = coordinates
                        sectionAnchorsReady = sectionCoordinates.size == DesktopSettingsSection.entries.size
                    }
                ) {
                        SettingsSection(desktopText(preferences.language, "settings.section.general"), Lucide.Palette) {
                        SettingsRow(
                            title = desktopText(preferences.language, "settings.color_mode"),
                            description = desktopText(preferences.language, "settings.color_mode_description")
                        ) {
                            SingleChoiceSegmentedButtonRow {
                                DesktopColorMode.entries.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        selected = preferences.colorMode == mode,
                                        onClick = { onPreferencesChange(preferences.copy(colorMode = mode)) },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = DesktopColorMode.entries.size
                                        ),
                                        label = {
                                            Text(
                                                when (mode) {
                                                    DesktopColorMode.SYSTEM -> desktopText(preferences.language, "settings.system")
                                                    DesktopColorMode.LIGHT -> desktopText(preferences.language, "settings.light")
                                                    DesktopColorMode.DARK -> desktopText(preferences.language, "settings.dark")
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        SettingsDivider()
                        SettingsRow(
                            title = desktopText(preferences.language, "settings.theme_color"),
                            description = desktopText(preferences.language, "settings.theme_color_description")
                        ) {
                            ThemeColorSelector(
                                language = preferences.language,
                                selected = preferences.themeColor,
                                dark = preferences.colorMode == DesktopColorMode.DARK,
                                onSelect = { onPreferencesChange(preferences.copy(themeColor = it)) }
                            )
                        }
                        SettingsDivider()
                        SettingsRow(
                            title = desktopText(preferences.language, "settings.font"),
                            description = desktopText(preferences.language, "settings.font_description")
                                .replace("%s", preferences.fontFamily.displayName(preferences.language))
                        ) {
                            FontFamilySelector(
                                language = preferences.language,
                                selected = preferences.fontFamily,
                                onSelect = { onPreferencesChange(preferences.copy(fontFamily = it)) }
                            )
                        }
                        SettingsDivider()
                        SettingsRow(
                            title = desktopText(preferences.language, "settings.language"),
                            description = desktopText(preferences.language, "settings.language_description")
                        ) {
                            DesktopLanguageSelector(
                                selected = preferences.language,
                                onSelect = { onPreferencesChange(preferences.copy(language = it)) }
                            )
                        }
                        SettingsDivider()
                        SettingsRow(
                            title = desktopText(preferences.language, "settings.chat_font_size"),
                            description = "${(preferences.fontScale * 100).roundToInt()}%"
                        ) {
                            Slider(
                                value = preferences.fontScale,
                                onValueChange = {
                                    onPreferencesChange(preferences.copy(fontScale = it))
                                },
                                valueRange = 0.8f..1.4f,
                                steps = 5,
                                modifier = Modifier.widthIn(min = 180.dp, max = 240.dp)
                            )
                        }
                    }
                }

                Box(
                    Modifier.onGloballyPositioned { coordinates ->
                        sectionCoordinates[DesktopSettingsSection.MESSAGE_DISPLAY] = coordinates
                        sectionAnchorsReady = sectionCoordinates.size == DesktopSettingsSection.entries.size
                    }
                ) {
                    SettingsSection(desktopText(preferences.language, "settings.section.messages"), Lucide.MessageSquareText) {
                        PreferenceSwitch(
                            desktopText(preferences.language, "settings.show_user_avatar"),
                            desktopText(preferences.language, "settings.show_user_avatar_description"),
                            preferences.showUserAvatar
                        ) { onPreferencesChange(preferences.copy(showUserAvatar = it)) }
                        SettingsDivider()
                        SettingsRow(
                            title = desktopText(preferences.language, "settings.user_nickname"),
                            description = desktopText(preferences.language, "settings.user_nickname_description")
                        ) {
                            OutlinedTextField(
                                value = preferences.userNickname,
                                onValueChange = { nickname ->
                                    onPreferencesChange(preferences.copy(userNickname = nickname))
                                },
                                modifier = Modifier.widthIn(min = 180.dp, max = 240.dp),
                                placeholder = { Text(desktopText(preferences.language, "settings.user_nickname_placeholder")) },
                                singleLine = true
                            )
                        }
                        SettingsDivider()
                        PreferenceSwitch(
                            desktopText(preferences.language, "settings.show_model_icon"),
                            desktopText(preferences.language, "settings.show_model_icon_description"),
                            preferences.showModelIcon
                        ) { onPreferencesChange(preferences.copy(showModelIcon = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            desktopText(preferences.language, "settings.show_model_name"),
                            desktopText(preferences.language, "settings.show_model_name_description"),
                            preferences.showModelName
                        ) { onPreferencesChange(preferences.copy(showModelName = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            desktopText(preferences.language, "settings.assistant_bubble"),
                            desktopText(preferences.language, "settings.assistant_bubble_description"),
                            preferences.showAssistantBubble
                        ) { onPreferencesChange(preferences.copy(showAssistantBubble = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            desktopText(preferences.language, "settings.message_timestamp"),
                            desktopText(preferences.language, "settings.message_timestamp_description"),
                            preferences.showMessageTimestamp
                        ) { onPreferencesChange(preferences.copy(showMessageTimestamp = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            desktopText(preferences.language, "settings.reasoning"),
                            desktopText(preferences.language, "settings.reasoning_description"),
                            preferences.showReasoning
                        ) { onPreferencesChange(preferences.copy(showReasoning = it)) }
                        if (preferences.showReasoning) {
                            SettingsDivider()
                            PreferenceSwitch(
                                desktopText(preferences.language, "settings.collapse_reasoning"),
                                desktopText(preferences.language, "settings.collapse_reasoning_description"),
                                preferences.autoCollapseReasoning
                            ) { onPreferencesChange(preferences.copy(autoCollapseReasoning = it)) }
                        }
                        SettingsDivider()
                        PreferenceSwitch(
                            desktopText(preferences.language, "settings.code_wrap"),
                            desktopText(preferences.language, "settings.code_wrap_description"),
                            preferences.codeBlockAutoWrap
                        ) { onPreferencesChange(preferences.copy(codeBlockAutoWrap = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            desktopText(preferences.language, "settings.chinese_typography"),
                            desktopText(preferences.language, "settings.chinese_typography_description"),
                            preferences.enableChineseTypography
                        ) { onPreferencesChange(preferences.copy(enableChineseTypography = it)) }
                    }
                }

                Box(
                    Modifier.onGloballyPositioned { coordinates ->
                        sectionCoordinates[DesktopSettingsSection.INTERACTION] = coordinates
                        sectionAnchorsReady = sectionCoordinates.size == DesktopSettingsSection.entries.size
                    }
                ) {
                    SettingsSection(desktopText(preferences.language, "settings.section.interaction"), Lucide.Keyboard) {
                        PreferenceSwitch(
                            desktopText(preferences.language, "settings.send_on_enter"),
                            desktopText(preferences.language, "settings.send_on_enter_description"),
                            preferences.sendOnEnter
                        ) { onPreferencesChange(preferences.copy(sendOnEnter = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            desktopText(preferences.language, "settings.auto_scroll"),
                            desktopText(preferences.language, "settings.auto_scroll_description"),
                            preferences.enableAutoScroll
                        ) { onPreferencesChange(preferences.copy(enableAutoScroll = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            desktopText(preferences.language, "settings.message_navigation"),
                            desktopText(preferences.language, "settings.message_navigation_description"),
                            preferences.showMessageJumper
                        ) { onPreferencesChange(preferences.copy(showMessageJumper = it)) }
                        if (preferences.showMessageJumper) {
                            SettingsDivider()
                            PreferenceSwitch(
                                desktopText(preferences.language, "settings.message_navigation_left"),
                                desktopText(preferences.language, "settings.message_navigation_left_description"),
                                preferences.messageJumperOnLeft
                            ) { onPreferencesChange(preferences.copy(messageJumperOnLeft = it)) }
                        }
                    }
                }

                Box(
                    Modifier.onGloballyPositioned { coordinates ->
                        sectionCoordinates[DesktopSettingsSection.DATA] = coordinates
                        sectionAnchorsReady = sectionCoordinates.size == DesktopSettingsSection.entries.size
                    }
                ) {
                    SettingsSection(desktopText(preferences.language, "settings.section.data"), Lucide.Save) {
                        SettingsRow(
                            title = desktopText(preferences.language, "settings.export_backup"),
                            description = desktopText(preferences.language, "settings.export_backup_description")
                        ) {
                            OutlinedButton(onClick = { backupStatus = onExportData() }) {
                                Icon(Lucide.Download, null, Modifier.size(17.dp))
                                Text(desktopText(preferences.language, "settings.export"), Modifier.padding(start = 7.dp))
                            }
                        }
                        SettingsDivider()
                        SettingsRow(
                            title = desktopText(preferences.language, "settings.import_backup"),
                            description = desktopText(preferences.language, "settings.import_backup_description")
                        ) {
                            OutlinedButton(onClick = { backupStatus = onImportData() }) {
                                Icon(Lucide.Upload, null, Modifier.size(17.dp))
                                Text(desktopText(preferences.language, "settings.import"), Modifier.padding(start = 7.dp))
                            }
                        }
                        backupStatus?.let { status ->
                            SettingsDivider()
                            Text(
                                status,
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                color = if (status.contains("failed", ignoreCase = true)) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 12.sp
                            )
                        }
                        SettingsDivider()
                        SettingsRow(
                            title = desktopText(preferences.language, "settings.reset_data"),
                            description = desktopText(preferences.language, "settings.reset_data_description")
                        ) {
                            OutlinedButton(onClick = { resetConfirmationOpen = true }) {
                                Icon(Lucide.Trash2, null, Modifier.size(17.dp))
                                Text(desktopText(preferences.language, "settings.reset"), Modifier.padding(start = 7.dp))
                            }
                        }
                        SettingsDivider()
                        SettingsRow(
                            title = desktopText(preferences.language, "settings.web_search"),
                            description = desktopText(preferences.language, "settings.web_search_description")
                        ) {
                            Column(Modifier.widthIn(min = 240.dp, max = 390.dp)) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    DesktopSearchProviderType.entries.forEach { provider ->
                                        FilterChip(
                                            selected = webSearchSettings.providerType == provider,
                                            onClick = {
                                                onWebSearchSettingsChange(
                                                    webSearchSettings.selectProvider(provider)
                                                )
                                            },
                                            label = { Text(provider.displayName) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                if (webSearchSettings.providerType == DesktopSearchProviderType.SEARXNG) {
                                    OutlinedTextField(
                                        value = webSearchSettings.searxngUrl,
                                        onValueChange = { onWebSearchSettingsChange(webSearchSettings.copy(searxngUrl = it.trim())) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(desktopText(preferences.language, "settings.searxng_url")) },
                                        placeholder = { Text("https://searx.example.com") },
                                        singleLine = true
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = webSearchSettings.apiKey,
                                        onValueChange = { onWebSearchSettingsChange(webSearchSettings.withApiKey(it.trim())) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("${webSearchSettings.providerType.displayName} ${desktopText(preferences.language, "settings.api_key")}") },
                                        singleLine = true
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    desktopText(preferences.language, "settings.search_result_count")
                                        .replace("%d", webSearchSettings.resultCount.coerceIn(1, 10).toString()),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                                Slider(
                                    value = webSearchSettings.resultCount.coerceIn(1, 10).toFloat(),
                                    onValueChange = {
                                        onWebSearchSettingsChange(webSearchSettings.copy(resultCount = it.roundToInt()))
                                    },
                                    valueRange = 1f..10f,
                                    steps = 8
                                )
                                OutlinedButton(
                                    enabled = webSearchSettings.isConfigured && !webSearchTesting,
                                    onClick = {
                                        webSearchTesting = true
                                        webSearchTestStatus = null
                                        scope.launch {
                                            webSearchTestStatus = runCatching {
                                                client.testWebSearch(webSearchSettings, "RikkaHub")
                                                desktopText(preferences.language, "settings.search_test_success")
                                            }.getOrElse { error ->
                                                "${desktopText(preferences.language, "settings.search_test_failed")}: ${error.message ?: ""}"
                                            }
                                            webSearchTesting = false
                                        }
                                    }
                                ) {
                                    if (webSearchTesting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    else Text(desktopText(preferences.language, "settings.test_search"))
                                }
                                webSearchTestStatus?.let { status ->
                                    Text(
                                        status,
                                        Modifier.padding(top = 6.dp),
                                        color = if (status == desktopText(preferences.language, "settings.search_test_success")) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    Modifier.onGloballyPositioned { coordinates ->
                        sectionCoordinates[DesktopSettingsSection.ASSISTANTS] = coordinates
                        sectionAnchorsReady = sectionCoordinates.size == DesktopSettingsSection.entries.size
                    }
                ) {
                    SettingsSection(desktopText(preferences.language, "settings.section.assistants"), Lucide.Bot) {
                        FlowRow(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            assistants.forEach { assistant ->
                                FilterChip(
                                    selected = assistant.id == selectedAssistant.id,
                                    onClick = { onAssistantSelect(assistant.id) },
                                    label = { Text(assistant.name) },
                                    leadingIcon = { Icon(Lucide.Bot, null, Modifier.size(16.dp)) }
                                )
                            }
                            IconButton(onClick = onAssistantAdd, modifier = Modifier.size(40.dp)) {
                                Icon(Lucide.Plus, desktopText(preferences.language, "settings.add_assistant"), Modifier.size(18.dp))
                            }
                        }
                        SettingsDivider()
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    draftAssistant.name,
                                    { draftAssistant = draftAssistant.copy(name = it) },
                                    Modifier.weight(1f),
                                    label = { Text(desktopText(preferences.language, "settings.assistant_name")) },
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = { onAssistantCopy(selectedAssistant.id) },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(Lucide.Copy, desktopText(preferences.language, "settings.copy_assistant"), Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onAssistantDelete(selectedAssistant.id) },
                                    enabled = assistants.size > 1,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(Lucide.Trash2, desktopText(preferences.language, "settings.delete_assistant"), Modifier.size(18.dp))
                                }
                            }
                            OutlinedTextField(
                                draftAssistant.tags.joinToString(", "),
                                { value ->
                                    draftAssistant = draftAssistant.copy(
                                        tags = value.split(',').map { it.trim() }.filter { it.isNotBlank() }
                                            .distinctBy { it.lowercase() }.toSet()
                                    )
                                },
                                Modifier.fillMaxWidth(),
                                label = { Text(desktopText(preferences.language, "settings.tags")) },
                                singleLine = true
                            )
                            Box {
                                val assistantProvider = providers.firstOrNull {
                                    it.id == draftAssistant.providerId
                                } ?: selectedProvider
                                OutlinedButton(
                                    onClick = { assistantProviderMenuOpen = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Lucide.ServerCog, null, Modifier.size(17.dp))
                                    Text(assistantProvider.name, Modifier.padding(horizontal = 8.dp).weight(1f))
                                    Icon(Lucide.ChevronDown, null, Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = assistantProviderMenuOpen,
                                    onDismissRequest = { assistantProviderMenuOpen = false }
                                ) {
                                    providers.forEach { provider ->
                                        DropdownMenuItem(
                                            text = { Text(provider.name) },
                                            onClick = {
                                                assistantProviderMenuOpen = false
                                                draftAssistant = draftAssistant.copy(
                                                    providerId = provider.id,
                                                    model = provider.config.model
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            Box {
                                val assistantProvider = providers.firstOrNull {
                                    it.id == draftAssistant.providerId
                                } ?: selectedProvider
                                OutlinedTextField(
                                    draftAssistant.model,
                                    { draftAssistant = draftAssistant.copy(model = it) },
                                    Modifier.fillMaxWidth(),
                                    label = { Text(desktopText(preferences.language, "settings.model_inherit")) },
                                    trailingIcon = {
                                        if (assistantProvider.discoveredModels.isNotEmpty()) {
                                            IconButton(onClick = { assistantModelMenuOpen = true }) {
                                                Icon(Lucide.ChevronDown, desktopText(preferences.language, "settings.select_model"))
                                            }
                                        }
                                    },
                                    singleLine = true
                                )
                                DropdownMenu(
                                    expanded = assistantModelMenuOpen,
                                    onDismissRequest = { assistantModelMenuOpen = false }
                                ) {
                                    assistantProvider.discoveredModels.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model) },
                                            onClick = {
                                                assistantModelMenuOpen = false
                                                draftAssistant = draftAssistant.copy(model = model)
                                            }
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                draftAssistant.systemPrompt,
                                { draftAssistant = draftAssistant.copy(systemPrompt = it) },
                                Modifier.fillMaxWidth(),
                                label = { Text(desktopText(preferences.language, "settings.system_prompt_inherit")) },
                                minLines = 4
                            )
                            OutlinedTextField(
                                draftAssistant.messageTemplate,
                                { draftAssistant = draftAssistant.copy(messageTemplate = it) },
                                Modifier.fillMaxWidth(),
                                label = { Text(desktopText(preferences.language, "assistant.message_template")) },
                                supportingText = {
                                    Text(
                                        if (messageTemplateValid) {
                                            desktopText(preferences.language, "assistant.template_variables")
                                        } else {
                                            desktopText(preferences.language, "assistant.template_invalid")
                                        }
                                    )
                                },
                                isError = !messageTemplateValid,
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                                minLines = 3,
                                maxLines = 8
                            )
                            PreferenceSwitch(
                                desktopText(preferences.language, "assistant.conversation_system_prompt"),
                                desktopText(preferences.language, "assistant.conversation_system_prompt_help"),
                                draftAssistant.allowConversationSystemPrompt
                            ) {
                                draftAssistant = draftAssistant.copy(allowConversationSystemPrompt = it)
                            }
                            PreferenceSwitch(
                                desktopText(preferences.language, "assistant.conversation_lorebooks"),
                                desktopText(preferences.language, "assistant.conversation_lorebooks_help"),
                                draftAssistant.allowConversationPromptInjection
                            ) {
                                draftAssistant = draftAssistant.copy(allowConversationPromptInjection = it)
                            }
                            PreferenceSwitch(
                                desktopText(preferences.language, "assistant.web_search"),
                                desktopText(preferences.language, "assistant.web_search_help"),
                                draftAssistant.enableWebSearch
                            ) {
                                draftAssistant = draftAssistant.copy(enableWebSearch = it)
                            }
                            PreferenceSwitch(
                                desktopText(preferences.language, "assistant.local_time_tool"),
                                desktopText(preferences.language, "assistant.local_time_tool_help"),
                                DesktopLocalTool.CURRENT_TIME in draftAssistant.localTools
                            ) { enabled ->
                                draftAssistant = draftAssistant.copy(
                                    localTools = if (enabled) {
                                        draftAssistant.localTools + DesktopLocalTool.CURRENT_TIME
                                    } else {
                                        draftAssistant.localTools - DesktopLocalTool.CURRENT_TIME
                                    }
                                )
                            }
                            PreferenceSwitch(
                                desktopText(preferences.language, "assistant.ask_user_tool"),
                                desktopText(preferences.language, "assistant.ask_user_tool_help"),
                                DesktopLocalTool.ASK_USER in draftAssistant.localTools
                            ) { enabled ->
                                draftAssistant = draftAssistant.copy(
                                    localTools = if (enabled) {
                                        draftAssistant.localTools + DesktopLocalTool.ASK_USER
                                    } else {
                                        draftAssistant.localTools - DesktopLocalTool.ASK_USER
                                    }
                                )
                            }
                            OutlinedTextField(
                                value = draftAssistant.maxToolRounds.toString(),
                                onValueChange = { value ->
                                    value.toIntOrNull()?.takeIf { it >= 0 }?.let { limit ->
                                        draftAssistant = draftAssistant.copy(maxToolRounds = limit)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(desktopText(preferences.language, "assistant.max_tool_rounds")) },
                                supportingText = { Text(desktopText(preferences.language, "assistant.max_tool_rounds_help")) },
                                singleLine = true
                            )
                            PreferenceSwitch(
                                desktopText(preferences.language, "assistant.agent_workspace"),
                                desktopText(preferences.language, "assistant.agent_workspace_help"),
                                draftAssistant.agentWorkspace != null
                            ) { enabled ->
                                draftAssistant = draftAssistant.copy(
                                    agentWorkspace = if (enabled) DesktopAgentWorkspace() else null
                                )
                            }
                            draftAssistant.agentWorkspace?.let { workspace ->
                                OutlinedTextField(
                                    value = workspace.rootPath,
                                    onValueChange = { root ->
                                        draftAssistant = draftAssistant.copy(agentWorkspace = workspace.copy(rootPath = root.trim()))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(desktopText(preferences.language, "assistant.workspace_directory")) },
                                    supportingText = { Text(desktopText(preferences.language, "assistant.workspace_directory_help")) },
                                    singleLine = true
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = workspace.backend == DesktopAgentBackend.DOCKER,
                                        onClick = {
                                            draftAssistant = draftAssistant.copy(
                                                agentWorkspace = workspace.copy(backend = DesktopAgentBackend.DOCKER)
                                            )
                                        },
                                        label = { Text(desktopText(preferences.language, "assistant.docker_container")) }
                                    )
                                    FilterChip(
                                        selected = workspace.backend == DesktopAgentBackend.LOCAL,
                                        onClick = {
                                            draftAssistant = draftAssistant.copy(
                                                agentWorkspace = workspace.copy(backend = DesktopAgentBackend.LOCAL)
                                            )
                                        },
                                        label = { Text(desktopText(preferences.language, "assistant.local_shell")) }
                                    )
                                }
                                if (workspace.backend == DesktopAgentBackend.DOCKER) {
                                    OutlinedTextField(
                                        value = workspace.dockerImage,
                                        onValueChange = { image ->
                                            draftAssistant = draftAssistant.copy(agentWorkspace = workspace.copy(dockerImage = image.trim()))
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(desktopText(preferences.language, "assistant.docker_image")) },
                                        supportingText = { Text(desktopText(preferences.language, "assistant.docker_image_help")) },
                                        singleLine = true
                                    )
                                }
                                OutlinedTextField(
                                    value = draftAssistant.enabledSkillNames.joinToString(", "),
                                    onValueChange = { names ->
                                        draftAssistant = draftAssistant.copy(
                                            enabledSkillNames = names.split(',').map(String::trim).filter(String::isNotBlank).toSet()
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(desktopText(preferences.language, "assistant.enabled_skills")) },
                                    supportingText = { Text(desktopText(preferences.language, "assistant.enabled_skills_help")) },
                                    singleLine = true
                                )
                            }
                            PreferenceSwitch(
                                desktopText(preferences.language, "assistant.stream_output"),
                                desktopText(preferences.language, "assistant.stream_output_help"),
                                draftAssistant.streamOutput
                            ) {
                                draftAssistant = draftAssistant.copy(streamOutput = it)
                            }
                            PreferenceSwitch(
                                desktopText(preferences.language, "assistant.memory"),
                                desktopText(preferences.language, "assistant.memory_help"),
                                draftAssistant.enableMemory
                            ) {
                                draftAssistant = draftAssistant.copy(enableMemory = it)
                            }
                            if (draftAssistant.enableMemory) {
                                PreferenceSwitch(
                                    desktopText(preferences.language, "assistant.global_memory"),
                                    desktopText(preferences.language, "assistant.global_memory_help"),
                                    draftAssistant.useGlobalMemory
                                ) {
                                    draftAssistant = draftAssistant.copy(useGlobalMemory = it)
                                }
                                if (draftAssistant.useGlobalMemory) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(desktopText(preferences.language, "assistant.global_memory"), Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                        IconButton(onClick = { onGlobalMemoriesChange(globalMemories + DesktopMemory()) }) {
                                            Icon(Lucide.Plus, desktopText(preferences.language, "assistant.add_global_memory"), Modifier.size(18.dp))
                                        }
                                    }
                                    globalMemories.forEachIndexed { index, memory ->
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                            OutlinedTextField(memory.content, { value ->
                                                onGlobalMemoriesChange(globalMemories.mapIndexed { i, item -> if (i == index) item.copy(content = value) else item })
                                            }, Modifier.weight(1f), label = { Text(desktopText(preferences.language, "assistant.shared_memory_content")) }, minLines = 2)
                                            IconButton(onClick = { onGlobalMemoriesChange(globalMemories.filterIndexed { i, _ -> i != index }) }) {
                                                Icon(Lucide.Trash2, desktopText(preferences.language, "assistant.delete_global_memory"), Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                                if (!draftAssistant.useGlobalMemory) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(desktopText(preferences.language, "assistant.memory_entries"), Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                    IconButton(
                                        onClick = {
                                            draftAssistant = draftAssistant.copy(
                                                memories = draftAssistant.memories + DesktopMemory()
                                            )
                                        }
                                    ) { Icon(Lucide.Plus, desktopText(preferences.language, "assistant.add_memory"), Modifier.size(18.dp)) }
                                }
                                draftAssistant.memories.forEachIndexed { index, memory ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        OutlinedTextField(
                                            memory.content,
                                            { value ->
                                                draftAssistant = draftAssistant.copy(
                                                    memories = draftAssistant.memories.mapIndexed { itemIndex, item ->
                                                        if (itemIndex == index) item.copy(content = value) else item
                                                    }
                                                )
                                            },
                                            Modifier.weight(1f),
                                            label = { Text(desktopText(preferences.language, "assistant.memory_content")) },
                                            minLines = 2,
                                            maxLines = 5,
                                            isError = memory.content.isBlank()
                                        )
                                        IconButton(
                                            onClick = {
                                                draftAssistant = draftAssistant.copy(
                                                    memories = draftAssistant.memories.filterIndexed {
                                                            itemIndex, _ -> itemIndex != index
                                                    }
                                                )
                                            },
                                            modifier = Modifier.padding(top = 8.dp)
                                        ) { Icon(Lucide.Trash2, desktopText(preferences.language, "assistant.delete_memory"), Modifier.size(18.dp)) }
                                    }
                                }
                                }
                            }
                            SettingsDivider()
                            DesktopMcpSettings(
                                language = preferences.language,
                                servers = mcpServers,
                                selectedServerIds = draftAssistant.mcpServerIds,
                                mcpClient = mcpClient,
                                onServersChange = onMcpServersChange,
                                onSelectedServerIdsChange = { ids ->
                                    draftAssistant = draftAssistant.copy(mcpServerIds = ids)
                                }
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    draftAssistant.temperature?.toString().orEmpty(),
                                    { value ->
                                        if (value.isBlank() || value.toDoubleOrNull()?.let { it in 0.0..2.0 } == true) {
                                            draftAssistant = draftAssistant.copy(temperature = value.toDoubleOrNull())
                                        }
                                    },
                                    Modifier.weight(1f),
                                    label = { Text(desktopText(preferences.language, "assistant.temperature")) },
                                    supportingText = { Text(desktopText(preferences.language, "assistant.inherit_provider")) },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    draftAssistant.maxTokens?.toString().orEmpty(),
                                    { value ->
                                        if (value.isBlank() || value.toIntOrNull()?.let { it >= 0 } == true) {
                                            draftAssistant = draftAssistant.copy(maxTokens = value.toIntOrNull())
                                        }
                                    },
                                    Modifier.weight(1f),
                                    label = { Text(desktopText(preferences.language, "assistant.max_output_tokens")) },
                                    supportingText = { Text(desktopText(preferences.language, "assistant.inherit_provider")) },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    draftAssistant.contextMessageSize.takeIf { it > 0 }?.toString().orEmpty(),
                                    { value ->
                                        if (value.isBlank() || value.toIntOrNull()?.let { it > 0 } == true) {
                                            draftAssistant = draftAssistant.copy(
                                                contextMessageSize = value.toIntOrNull() ?: 0
                                            )
                                        }
                                    },
                                    Modifier.weight(1f),
                                    label = { Text(desktopText(preferences.language, "assistant.context_messages")) },
                                    supportingText = { Text(desktopText(preferences.language, "assistant.keep_all_when_empty")) },
                                    singleLine = true
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    draftAssistant.topP?.toString().orEmpty(),
                                    { value ->
                                        if (value.isBlank() || value.toDoubleOrNull()?.let { it in 0.0..1.0 } == true) {
                                            draftAssistant = draftAssistant.copy(topP = value.toDoubleOrNull())
                                        }
                                    },
                                    Modifier.weight(1f),
                                    label = { Text("Top P") },
                                    supportingText = { Text(desktopText(preferences.language, "assistant.inherit_provider")) },
                                    singleLine = true
                                )
                                Column(Modifier.weight(2f)) {
                                    Text(desktopText(preferences.language, "model_picker.reasoning"), fontSize = 12.sp)
                                    SingleChoiceSegmentedButtonRow {
                                        listOf("", "low", "medium", "high").forEachIndexed { index, effort ->
                                            SegmentedButton(
                                                selected = draftAssistant.reasoningEffort == effort,
                                                onClick = {
                                                    draftAssistant = draftAssistant.copy(reasoningEffort = effort)
                                                },
                                                shape = SegmentedButtonDefaults.itemShape(index, 4),
                                                label = { Text(if (effort.isBlank()) desktopText(preferences.language, "model_picker.default") else desktopText(preferences.language, "model_picker.$effort")) }
                                            )
                                        }
                                    }
                                }
                            }
                            SettingsDivider()
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(desktopText(preferences.language, "assistant.preset_messages"), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        desktopText(preferences.language, "assistant.preset_messages_help"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        val nextRole = if (
                                            draftAssistant.presetMessages.lastOrNull()?.role == "assistant"
                                        ) "user" else "assistant"
                                        draftAssistant = draftAssistant.copy(
                                            presetMessages = draftAssistant.presetMessages +
                                                DesktopPresetMessage(role = nextRole)
                                        )
                                    }
                                ) {
                                    Icon(Lucide.Plus, null, Modifier.size(17.dp))
                                    Text(desktopText(preferences.language, "common.add"), Modifier.padding(start = 7.dp))
                                }
                            }
                            draftAssistant.presetMessages.forEachIndexed { index, presetMessage ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    SingleChoiceSegmentedButtonRow(Modifier.padding(top = 8.dp)) {
                                        listOf("user", "assistant").forEachIndexed { roleIndex, role ->
                                            SegmentedButton(
                                                selected = presetMessage.role == role,
                                                onClick = {
                                                    draftAssistant = draftAssistant.copy(
                                                        presetMessages = draftAssistant.presetMessages.mapIndexed {
                                                                itemIndex, item ->
                                                            if (itemIndex == index) item.copy(role = role) else item
                                                        }
                                                    )
                                                },
                                                shape = SegmentedButtonDefaults.itemShape(roleIndex, 2),
                                                label = { Text(desktopText(preferences.language, "role.$role")) }
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        presetMessage.content,
                                        { value ->
                                            draftAssistant = draftAssistant.copy(
                                                presetMessages = draftAssistant.presetMessages.mapIndexed {
                                                        itemIndex, item ->
                                                    if (itemIndex == index) item.copy(content = value) else item
                                                }
                                            )
                                        },
                                        Modifier.weight(1f),
                                        label = { Text(desktopText(preferences.language, "assistant.message")) },
                                        minLines = 2,
                                        maxLines = 5
                                    )
                                    IconButton(
                                        onClick = {
                                            draftAssistant = draftAssistant.copy(
                                                presetMessages = draftAssistant.presetMessages.filterIndexed {
                                                        itemIndex, _ -> itemIndex != index
                                                }
                                            )
                                        },
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Icon(Lucide.Trash2, desktopText(preferences.language, "assistant.delete_preset_message"), Modifier.size(18.dp))
                                    }
                                }
                            }
                            SettingsDivider()
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(desktopText(preferences.language, "assistant.quick_messages"), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        desktopText(preferences.language, "assistant.quick_messages_help"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        draftAssistant = draftAssistant.copy(
                                            quickMessages = draftAssistant.quickMessages + DesktopQuickMessage()
                                        )
                                    }
                                ) {
                                    Icon(Lucide.Plus, null, Modifier.size(17.dp))
                                    Text(desktopText(preferences.language, "common.add"), Modifier.padding(start = 7.dp))
                                }
                            }
                            draftAssistant.quickMessages.forEachIndexed { index, quickMessage ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    OutlinedTextField(
                                        quickMessage.title,
                                        { value ->
                                            draftAssistant = draftAssistant.copy(
                                                quickMessages = draftAssistant.quickMessages.mapIndexed { itemIndex, item ->
                                                    if (itemIndex == index) item.copy(title = value) else item
                                                }
                                            )
                                        },
                                        Modifier.weight(0.7f),
                                        label = { Text(desktopText(preferences.language, "assistant.title")) },
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        quickMessage.content,
                                        { value ->
                                            draftAssistant = draftAssistant.copy(
                                                quickMessages = draftAssistant.quickMessages.mapIndexed { itemIndex, item ->
                                                    if (itemIndex == index) item.copy(content = value) else item
                                                }
                                            )
                                        },
                                        Modifier.weight(1.3f),
                                        label = { Text(desktopText(preferences.language, "assistant.content")) },
                                        minLines = 2,
                                        maxLines = 4
                                    )
                                    IconButton(
                                        onClick = {
                                            draftAssistant = draftAssistant.copy(
                                                quickMessages = draftAssistant.quickMessages.filterIndexed {
                                                        itemIndex, _ -> itemIndex != index
                                                }
                                            )
                                        },
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Icon(Lucide.Trash2, desktopText(preferences.language, "assistant.delete_quick_message"), Modifier.size(18.dp))
                                    }
                                }
                            }
                            SettingsDivider()
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(desktopText(preferences.language, "assistant.regex_transforms"), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        desktopText(preferences.language, "assistant.regex_transforms_help"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        draftAssistant = draftAssistant.copy(
                                            regexRules = draftAssistant.regexRules + DesktopRegexRule()
                                        )
                                    }
                                ) {
                                    Icon(Lucide.Plus, null, Modifier.size(17.dp))
                                    Text(desktopText(preferences.language, "common.add"), Modifier.padding(start = 7.dp))
                                }
                            }
                            draftAssistant.regexRules.forEachIndexed { index, rule ->
                                Column(
                                    Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            rule.name,
                                            { value ->
                                                draftAssistant = draftAssistant.copy(
                                                    regexRules = draftAssistant.regexRules.mapIndexed { itemIndex, item ->
                                                        if (itemIndex == index) item.copy(name = value) else item
                                                    }
                                                )
                                            },
                                            Modifier.weight(1f),
                                            label = { Text(desktopText(preferences.language, "assistant.rule_name")) },
                                            singleLine = true
                                        )
                                        Switch(
                                            checked = rule.enabled,
                                            onCheckedChange = { enabled ->
                                                draftAssistant = draftAssistant.copy(
                                                    regexRules = draftAssistant.regexRules.mapIndexed { itemIndex, item ->
                                                        if (itemIndex == index) item.copy(enabled = enabled) else item
                                                    }
                                                )
                                            }
                                        )
                                        IconButton(
                                            onClick = {
                                                draftAssistant = draftAssistant.copy(
                                                    regexRules = draftAssistant.regexRules.filterIndexed {
                                                            itemIndex, _ -> itemIndex != index
                                                    }
                                                )
                                            }
                                        ) {
                                            Icon(Lucide.Trash2, desktopText(preferences.language, "assistant.delete_regex_rule"), Modifier.size(18.dp))
                                        }
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            rule.findRegex,
                                            { value ->
                                                draftAssistant = draftAssistant.copy(
                                                    regexRules = draftAssistant.regexRules.mapIndexed { itemIndex, item ->
                                                        if (itemIndex == index) item.copy(findRegex = value) else item
                                                    }
                                                )
                                            },
                                            Modifier.weight(1f),
                                            label = { Text(desktopText(preferences.language, "assistant.find_regex")) },
                                            isError = rule.findRegex.isBlank() ||
                                                runCatching { Regex(rule.findRegex) }.isFailure,
                                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            rule.replaceString,
                                            { value ->
                                                draftAssistant = draftAssistant.copy(
                                                    regexRules = draftAssistant.regexRules.mapIndexed { itemIndex, item ->
                                                        if (itemIndex == index) item.copy(replaceString = value) else item
                                                    }
                                                )
                                            },
                                            Modifier.weight(1f),
                                            label = { Text(desktopText(preferences.language, "assistant.replace_with")) },
                                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                                            singleLine = true
                                        )
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("user", "assistant").forEach { role ->
                                            FilterChip(
                                                selected = role in rule.roles,
                                                onClick = {
                                                    val roles = if (role in rule.roles) {
                                                        rule.roles - role
                                                    } else {
                                                        rule.roles + role
                                                    }
                                                    draftAssistant = draftAssistant.copy(
                                                        regexRules = draftAssistant.regexRules.mapIndexed {
                                                                itemIndex, item ->
                                                            if (itemIndex == index) item.copy(roles = roles) else item
                                                        }
                                                    )
                                                },
                                                label = { Text(desktopText(preferences.language, "role.$role")) }
                                            )
                                        }
                                        FilterChip(
                                            selected = rule.visualOnly,
                                            onClick = {
                                                draftAssistant = draftAssistant.copy(
                                                    regexRules = draftAssistant.regexRules.mapIndexed { itemIndex, item ->
                                                        if (itemIndex == index) {
                                                            item.copy(visualOnly = !item.visualOnly)
                                                        } else item
                                                    }
                                                )
                                            },
                                            label = { Text(desktopText(preferences.language, "assistant.visual_only")) }
                                        )
                                    }
                                }
                            }
                            SettingsDivider()
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(desktopText(preferences.language, "assistant.prompt_injections"), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        desktopText(preferences.language, "assistant.prompt_injections_help"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        draftAssistant = draftAssistant.copy(
                                            promptInjections = draftAssistant.promptInjections + DesktopPromptInjection()
                                        )
                                    }
                                ) {
                                    Icon(Lucide.Plus, null, Modifier.size(17.dp))
                                    Text(desktopText(preferences.language, "common.add"), Modifier.padding(start = 7.dp))
                                }
                            }
                            draftAssistant.promptInjections.forEachIndexed { index, injection ->
                                Column(
                                    Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            injection.name,
                                            { value ->
                                                draftAssistant = draftAssistant.copy(
                                                    promptInjections = draftAssistant.promptInjections.mapIndexed { itemIndex, item ->
                                                        if (itemIndex == index) item.copy(name = value) else item
                                                    }
                                                )
                                            },
                                            Modifier.weight(1f),
                                            label = { Text(desktopText(preferences.language, "assistant.entry_name")) },
                                            singleLine = true
                                        )
                                        Switch(
                                            checked = injection.enabled,
                                            onCheckedChange = { enabled ->
                                                draftAssistant = draftAssistant.copy(
                                                    promptInjections = draftAssistant.promptInjections.mapIndexed { itemIndex, item ->
                                                        if (itemIndex == index) item.copy(enabled = enabled) else item
                                                    }
                                                )
                                            }
                                        )
                                        IconButton(
                                            onClick = {
                                                draftAssistant = draftAssistant.copy(
                                                    promptInjections = draftAssistant.promptInjections.filterIndexed {
                                                            itemIndex, _ -> itemIndex != index
                                                    }
                                                )
                                            }
                                        ) { Icon(Lucide.Trash2, desktopText(preferences.language, "assistant.delete_injection"), Modifier.size(18.dp)) }
                                    }
                                    OutlinedTextField(
                                        injection.content,
                                        { value ->
                                            draftAssistant = draftAssistant.copy(
                                                promptInjections = draftAssistant.promptInjections.mapIndexed { itemIndex, item ->
                                                    if (itemIndex == index) item.copy(content = value) else item
                                                }
                                            )
                                        },
                                        Modifier.fillMaxWidth(),
                                        label = { Text(desktopText(preferences.language, "assistant.injection_content")) },
                                        minLines = 2
                                    )
                                    OutlinedTextField(
                                        injection.keywords.joinToString(", "),
                                        { value ->
                                            draftAssistant = draftAssistant.copy(
                                                promptInjections = draftAssistant.promptInjections.mapIndexed { itemIndex, item ->
                                                    if (itemIndex == index) {
                                                        item.copy(keywords = value.split(',').map { it.trim() }.filter { it.isNotBlank() })
                                                    } else item
                                                }
                                            )
                                        },
                                        Modifier.fillMaxWidth(),
                                        label = { Text(desktopText(preferences.language, "assistant.keywords")) },
                                        supportingText = { Text(desktopText(preferences.language, "assistant.keywords_help")) },
                                        singleLine = true
                                    )
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            injection.priority.toString(),
                                            { value ->
                                                if (value == "-" || value.toIntOrNull() != null) {
                                                    draftAssistant = draftAssistant.copy(
                                                        promptInjections = draftAssistant.promptInjections.mapIndexed { itemIndex, item ->
                                                            if (itemIndex == index) item.copy(priority = value.toIntOrNull() ?: 0) else item
                                                        }
                                                    )
                                                }
                                            },
                                            Modifier.weight(1f),
                                            label = { Text(desktopText(preferences.language, "assistant.priority")) },
                                            supportingText = { Text(desktopText(preferences.language, "assistant.priority_help")) },
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            injection.scanDepth.toString(),
                                            { value ->
                                                if (value.toIntOrNull()?.let { it > 0 } == true) {
                                                    draftAssistant = draftAssistant.copy(
                                                        promptInjections = draftAssistant.promptInjections.mapIndexed { itemIndex, item ->
                                                            if (itemIndex == index) item.copy(scanDepth = value.toInt()) else item
                                                        }
                                                    )
                                                }
                                            },
                                            Modifier.weight(1f),
                                            label = { Text(desktopText(preferences.language, "assistant.scan_depth")) },
                                            supportingText = { Text(desktopText(preferences.language, "assistant.scan_depth_help")) },
                                            singleLine = true
                                        )
                                        if (injection.position == DesktopInjectionPosition.AT_DEPTH) {
                                            OutlinedTextField(
                                                injection.injectDepth.toString(),
                                                { value ->
                                                    if (value.toIntOrNull()?.let { it >= 0 } == true) {
                                                        draftAssistant = draftAssistant.copy(
                                                            promptInjections = draftAssistant.promptInjections.mapIndexed { itemIndex, item ->
                                                                if (itemIndex == index) item.copy(injectDepth = value.toInt()) else item
                                                            }
                                                        )
                                                    }
                                                },
                                                Modifier.weight(1f),
                                                label = { Text(desktopText(preferences.language, "assistant.injection_depth")) },
                                                supportingText = { Text(desktopText(preferences.language, "assistant.injection_depth_help")) },
                                                singleLine = true
                                            )
                                        }
                                    }
                                    Text(desktopText(preferences.language, "assistant.injection_role"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    SingleChoiceSegmentedButtonRow {
                                        listOf("system", "user", "assistant").forEachIndexed { roleIndex, role ->
                                            SegmentedButton(
                                                selected = injection.role == role,
                                                onClick = {
                                                    draftAssistant = draftAssistant.copy(
                                                        promptInjections = draftAssistant.promptInjections.mapIndexed { itemIndex, item ->
                                                            if (itemIndex == index) item.copy(role = role) else item
                                                        }
                                                    )
                                                },
                                                shape = SegmentedButtonDefaults.itemShape(roleIndex, 3),
                                                label = { Text(desktopText(preferences.language, "role.$role")) }
                                            )
                                        }
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DesktopInjectionPosition.entries.forEach { position ->
                                            FilterChip(
                                                selected = injection.position == position,
                                                onClick = {
                                                    draftAssistant = draftAssistant.copy(
                                                        promptInjections = draftAssistant.promptInjections.mapIndexed { itemIndex, item ->
                                                            if (itemIndex == index) item.copy(position = position) else item
                                                        }
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        when (position) {
                                                            DesktopInjectionPosition.BEFORE_SYSTEM_PROMPT -> desktopText(preferences.language, "assistant.before_system")
                                                            DesktopInjectionPosition.AFTER_SYSTEM_PROMPT -> desktopText(preferences.language, "assistant.after_system")
                                                            DesktopInjectionPosition.TOP_OF_CHAT -> desktopText(preferences.language, "assistant.top_of_chat")
                                                            DesktopInjectionPosition.BOTTOM_OF_CHAT -> desktopText(preferences.language, "assistant.bottom_of_chat")
                                                            DesktopInjectionPosition.AT_DEPTH -> desktopText(preferences.language, "assistant.at_depth")
                                                        }
                                                    )
                                                }
                                            )
                                        }
                                        FilterChip(
                                            selected = injection.constantActive,
                                            onClick = {
                                                draftAssistant = draftAssistant.copy(
                                                    promptInjections = draftAssistant.promptInjections.mapIndexed { itemIndex, item ->
                                                        if (itemIndex == index) item.copy(constantActive = !item.constantActive) else item
                                                    }
                                                )
                                            },
                                            label = { Text(desktopText(preferences.language, "assistant.always_active")) }
                                        )
                                        FilterChip(
                                            selected = injection.useRegex,
                                            onClick = {
                                                draftAssistant = draftAssistant.copy(
                                                    promptInjections = draftAssistant.promptInjections.mapIndexed { itemIndex, item ->
                                                        if (itemIndex == index) item.copy(useRegex = !item.useRegex) else item
                                                    }
                                                )
                                            },
                                            label = { Text(desktopText(preferences.language, "assistant.regex_keywords")) }
                                        )
                                        FilterChip(
                                            selected = injection.caseSensitive,
                                            onClick = {
                                                draftAssistant = draftAssistant.copy(
                                                    promptInjections = draftAssistant.promptInjections.mapIndexed { itemIndex, item ->
                                                        if (itemIndex == index) item.copy(caseSensitive = !item.caseSensitive) else item
                                                    }
                                                )
                                            },
                                            label = { Text(desktopText(preferences.language, "assistant.case_sensitive")) }
                                        )
                                    }
                                }
                            }
                            SettingsDivider()
                            RequestOverridesEditor(
                                language = preferences.language,
                                headers = draftAssistant.customHeaders,
                                bodies = draftAssistant.customBodies,
                                onHeadersChange = { draftAssistant = draftAssistant.copy(customHeaders = it) },
                                onBodiesChange = { draftAssistant = draftAssistant.copy(customBodies = it) }
                            )
                            Button(
                                onClick = {
                                    onAssistantSave(draftAssistant)
                                    scope.launch { feedbackHostState.showSnackbar(desktopText(preferences.language, "assistant.saved")) }
                                },
                                enabled = draftAssistant.name.isNotBlank() &&
                                    draftAssistant.presetMessages.all { it.content.isNotBlank() } &&
                                    draftAssistant.quickMessages.all { it.content.isNotBlank() } &&
                                    messageTemplateValid && regexRulesValid && assistantBodiesValid && memoriesValid && injectionsValid,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Lucide.Save, null, Modifier.size(17.dp))
                                Text(desktopText(preferences.language, "assistant.save"), Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }

                Box(
                    Modifier.onGloballyPositioned { coordinates ->
                        sectionCoordinates[DesktopSettingsSection.PROVIDERS] = coordinates
                        sectionAnchorsReady = sectionCoordinates.size == DesktopSettingsSection.entries.size
                    }
                ) {
                    SettingsSection(desktopText(preferences.language, "settings.section.providers"), Lucide.ServerCog) {
                        FlowRow(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            providers.forEach { provider ->
                                FilterChip(
                                    selected = provider.id == selectedProvider.id,
                                    onClick = { onProviderSelect(provider.id) },
                                    label = { Text(provider.name) },
                                    leadingIcon = {
                                        DesktopProviderIcon(provider.name, Modifier.size(16.dp))
                                    }
                                )
                            }
                            Box {
                                OutlinedButton(onClick = { providerPresetMenuOpen = true }) {
                                    Text(desktopText(preferences.language, "settings.apply_preset"))
                                    Icon(Lucide.ChevronDown, null, Modifier.padding(start = 4.dp).size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = providerPresetMenuOpen,
                                    onDismissRequest = { providerPresetMenuOpen = false }
                                ) {
                                    DesktopProviderPresets.forEach { preset ->
                                        DropdownMenuItem(
                                            text = { Text(preset.name) },
                                            onClick = {
                                                providerPresetMenuOpen = false
                                                draftProvider = draftProvider.copy(
                                                    name = preset.name,
                                                    config = draftProvider.config.copy(
                                                        baseUrl = preset.baseUrl,
                                                        balanceOptions = preset.balanceOptions
                                                    )
                                                )
                                                balanceStatus = null
                                                balanceStatusIsError = false
                                            }
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = onProviderAdd, modifier = Modifier.size(40.dp)) {
                                Icon(Lucide.Plus, desktopText(preferences.language, "settings.add_provider"), Modifier.size(18.dp))
                            }
                        }
                        SettingsDivider()
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    draftProvider.name,
                                    { draftProvider = draftProvider.copy(name = it) },
                                    Modifier.weight(1f),
                                    label = { Text(desktopText(preferences.language, "settings.provider_name")) },
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = { onProviderDelete(selectedProvider.id) },
                                    enabled = providers.size > 1,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(Lucide.Trash2, desktopText(preferences.language, "settings.delete_provider"), Modifier.size(18.dp))
                                }
                            }
                            OutlinedTextField(
                                draftProvider.config.baseUrl,
                                { value ->
                                    draftProvider = draftProvider.copy(
                                        config = draftProvider.config.copy(baseUrl = value)
                                    )
                                },
                                Modifier.fillMaxWidth(),
                                label = { Text(desktopText(preferences.language, "settings.base_url")) },
                                singleLine = true
                            )
                            OutlinedTextField(
                                draftProvider.config.apiKey,
                                { value ->
                                    draftProvider = draftProvider.copy(
                                        config = draftProvider.config.copy(apiKey = value)
                                    )
                                },
                                Modifier.fillMaxWidth(),
                                label = { Text(desktopText(preferences.language, "settings.api_key")) },
                                singleLine = true,
                                visualTransformation = if (apiKeyVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            if (apiKeyVisible) Lucide.EyeOff else Lucide.Eye,
                                            desktopText(preferences.language, if (apiKeyVisible) "settings.hide_api_key" else "settings.show_api_key")
                                        )
                                    }
                                }
                            )
                            Box {
                                OutlinedTextField(
                                    draftProvider.config.model,
                                    { value ->
                                        draftProvider = draftProvider.copy(
                                            config = draftProvider.config.copy(model = value)
                                        )
                                    },
                                    Modifier.fillMaxWidth(),
                                    label = { Text(desktopText(preferences.language, "settings.model")) },
                                    leadingIcon = { Icon(Lucide.Bot, null) },
                                    trailingIcon = {
                                        if (draftProvider.discoveredModels.isNotEmpty()) {
                                            IconButton(onClick = { modelMenuOpen = true }) {
                                                Icon(Lucide.ChevronDown, desktopText(preferences.language, "settings.select_discovered_model"))
                                            }
                                        }
                                    },
                                    singleLine = true
                                )
                                DropdownMenu(
                                    expanded = modelMenuOpen,
                                    onDismissRequest = { modelMenuOpen = false }
                                ) {
                                    draftProvider.discoveredModels.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model) },
                                            onClick = {
                                                modelMenuOpen = false
                                                draftProvider = draftProvider.copy(
                                                    config = draftProvider.config.copy(model = model)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                draftProvider.config.titleModel,
                                { value ->
                                    draftProvider = draftProvider.copy(
                                        config = draftProvider.config.copy(titleModel = value)
                                    )
                                },
                                Modifier.fillMaxWidth(),
                                label = { Text(desktopText(preferences.language, "provider.title_model")) },
                                supportingText = { Text(desktopText(preferences.language, "provider.title_model_help")) },
                                singleLine = true
                            )
                            OutlinedTextField(
                                draftProvider.config.titlePrompt,
                                { value ->
                                    draftProvider = draftProvider.copy(
                                        config = draftProvider.config.copy(titlePrompt = value)
                                    )
                                },
                                Modifier.fillMaxWidth(),
                                label = { Text(desktopText(preferences.language, "provider.title_prompt")) },
                                supportingText = { Text(desktopText(preferences.language, "provider.title_prompt_help")) },
                                minLines = 4,
                                maxLines = 8
                            )
                            Text(
                                "${desktopText(preferences.language, "assistant.temperature")}  ${"%.1f".format(draftProvider.config.temperature)}",
                                fontSize = 13.sp
                            )
                            Slider(
                                value = draftProvider.config.temperature.toFloat(),
                                onValueChange = { value ->
                                    draftProvider = draftProvider.copy(
                                        config = draftProvider.config.copy(temperature = value.toDouble())
                                    )
                                },
                                valueRange = 0f..2f,
                                steps = 19
                            )
                            Text(
                                "Top P  ${"%.2f".format(draftProvider.config.topP)}",
                                fontSize = 13.sp
                            )
                            Slider(
                                value = draftProvider.config.topP.toFloat(),
                                onValueChange = { value ->
                                    draftProvider = draftProvider.copy(
                                        config = draftProvider.config.copy(topP = value.toDouble())
                                    )
                                },
                                valueRange = 0f..1f,
                                steps = 19
                            )
                            Text(desktopText(preferences.language, "model_picker.reasoning"), fontSize = 13.sp)
                            SingleChoiceSegmentedButtonRow {
                                listOf("", "low", "medium", "high").forEachIndexed { index, effort ->
                                    SegmentedButton(
                                        selected = draftProvider.config.reasoningEffort == effort,
                                        onClick = {
                                            draftProvider = draftProvider.copy(
                                                config = draftProvider.config.copy(reasoningEffort = effort)
                                            )
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index, 4),
                                        label = { Text(if (effort.isBlank()) desktopText(preferences.language, "model_picker.default") else desktopText(preferences.language, "model_picker.$effort")) }
                                    )
                                }
                            }
                            PreferenceSwitch(
                                desktopText(preferences.language, "provider.token_usage"),
                                desktopText(preferences.language, "provider.token_usage_help"),
                                draftProvider.config.requestTokenUsage
                            ) {
                                draftProvider = draftProvider.copy(
                                    config = draftProvider.config.copy(requestTokenUsage = it)
                                )
                            }
                            OutlinedTextField(
                                if (draftProvider.config.maxTokens == 0) {
                                    ""
                                } else {
                                    draftProvider.config.maxTokens.toString()
                                },
                                { value ->
                                    if (value.all(Char::isDigit)) {
                                        draftProvider = draftProvider.copy(
                                            config = draftProvider.config.copy(
                                                maxTokens = value.toIntOrNull() ?: 0
                                            )
                                        )
                                    }
                                },
                                Modifier.fillMaxWidth(),
                                label = { Text(desktopText(preferences.language, "assistant.max_output_tokens")) },
                                supportingText = { Text(desktopText(preferences.language, "provider.use_provider_default")) },
                                singleLine = true
                            )
                            OutlinedTextField(
                                draftProvider.config.systemPrompt,
                                { value ->
                                    draftProvider = draftProvider.copy(
                                        config = draftProvider.config.copy(systemPrompt = value)
                                    )
                                },
                                Modifier.fillMaxWidth(),
                                label = { Text(desktopText(preferences.language, "provider.system_prompt")) },
                                minLines = 3,
                                maxLines = 7
                            )
                            SettingsDivider()
                            PreferenceSwitch(
                                desktopText(preferences.language, "provider.balance_query"),
                                desktopText(preferences.language, "provider.balance_query_help"),
                                draftProvider.config.balanceOptions.enabled
                            ) { enabled ->
                                draftProvider = draftProvider.copy(
                                    config = draftProvider.config.copy(
                                        balanceOptions = draftProvider.config.balanceOptions.copy(enabled = enabled)
                                    )
                                )
                            }
                            if (draftProvider.config.balanceOptions.enabled) {
                                SettingsDivider()
                                OutlinedTextField(
                                    draftProvider.config.balanceOptions.apiPath,
                                    { path ->
                                        draftProvider = draftProvider.copy(
                                            config = draftProvider.config.copy(
                                                balanceOptions = draftProvider.config.balanceOptions.copy(apiPath = path)
                                            )
                                        )
                                    },
                                    Modifier.fillMaxWidth(),
                                    label = { Text(desktopText(preferences.language, "provider.balance_api_path")) },
                                    supportingText = { Text(desktopText(preferences.language, "provider.balance_api_path_help")) },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    draftProvider.config.balanceOptions.resultPath,
                                    { path ->
                                        draftProvider = draftProvider.copy(
                                            config = draftProvider.config.copy(
                                                balanceOptions = draftProvider.config.balanceOptions.copy(resultPath = path)
                                            )
                                        )
                                    },
                                    Modifier.fillMaxWidth(),
                                    label = { Text(desktopText(preferences.language, "provider.balance_result_path")) },
                                    supportingText = { Text(desktopText(preferences.language, "provider.balance_result_path_help")) },
                                    singleLine = true
                                )
                                OutlinedButton(
                                    onClick = {
                                        balanceStatus = desktopText(preferences.language, "provider.checking_balance")
                                        balanceStatusIsError = false
                                        scope.launch {
                                            balanceStatus = runCatching { client.getBalance(draftProvider.config) }
                                                .fold(
                                                    onSuccess = {
                                                        balanceStatusIsError = false
                                                        desktopText(preferences.language, "provider.current_balance").replace("%s", it)
                                                    },
                                                    onFailure = {
                                                        balanceStatusIsError = true
                                                        desktopText(preferences.language, "provider.balance_failed").replace("%s", it.message.orEmpty())
                                                    }
                                                )
                                        }
                                    },
                                    enabled = draftProvider.config.balanceOptions.resultPath.isNotBlank()
                                ) { Text(desktopText(preferences.language, "provider.check_balance")) }
                                balanceStatus?.let { status ->
                                    Text(
                                        status,
                                        color = if (balanceStatusIsError) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            RequestOverridesEditor(
                                language = preferences.language,
                                headers = draftProvider.config.customHeaders,
                                bodies = draftProvider.config.customBodies,
                                onHeadersChange = { headers ->
                                    draftProvider = draftProvider.copy(
                                        config = draftProvider.config.copy(customHeaders = headers)
                                    )
                                },
                                onBodiesChange = { bodies ->
                                    draftProvider = draftProvider.copy(
                                        config = draftProvider.config.copy(customBodies = bodies)
                                    )
                                }
                            )
                            ConnectionResult(connectionState, preferences.language)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        connectionState = ConnectionState.Testing
                                        scope.launch {
                                            connectionState = runCatching {
                                                client.listModels(draftProvider.config)
                                            }
                                                .fold(
                                                    onSuccess = { models ->
                                                        draftProvider = draftProvider.copy(
                                                            discoveredModels = models
                                                        )
                                                        ConnectionState.Success(models)
                                                    },
                                                    onFailure = {
                                                        ConnectionState.Failure(
                                                            it.message ?: "Connection failed"
                                                        )
                                                    }
                                                )
                                        }
                                    },
                                    enabled = connectionState !is ConnectionState.Testing
                                ) {
                                    if (connectionState is ConnectionState.Testing) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Lucide.ServerCog, null, Modifier.size(17.dp))
                                    }
                                    Text(desktopText(preferences.language, "settings.test_and_fetch_models"), Modifier.padding(start = 7.dp))
                                }
                                Button(
                                    onClick = {
                                        onProviderSave(draftProvider)
                                        scope.launch { feedbackHostState.showSnackbar(desktopText(preferences.language, "settings.provider_saved")) }
                                    },
                                    enabled = draftProvider.name.isNotBlank() && providerBodiesValid &&
                                        draftProvider.config.baseUrl.isNotBlank() &&
                                        draftProvider.config.model.isNotBlank()
                                ) {
                                    Icon(Lucide.Save, null, Modifier.size(17.dp))
                                    Text(desktopText(preferences.language, "settings.save_provider"), Modifier.padding(start = 7.dp))
                                }
                            }
                        }
                    }
                }

                Box {
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Icon(Lucide.ChevronLeft, null, Modifier.size(17.dp))
                        Text(desktopText(preferences.language, "settings.back_to_chat"), Modifier.padding(start = 6.dp))
                    }
                }
                }
                VerticalScrollbar(
                    adapter = settingsScrollbarAdapter,
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(12.dp)
                        .padding(vertical = 12.dp),
                    style = ScrollbarStyle(
                        minimalHeight = 72.dp,
                        thickness = 12.dp,
                        shape = RoundedCornerShape(12.dp),
                        hoverDurationMillis = 220,
                        unhoverColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f),
                        hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
    SnackbarHost(
        hostState = feedbackHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
    )
    }

    if (resetConfirmationOpen) {
        AlertDialog(
            onDismissRequest = { resetConfirmationOpen = false },
            title = { Text(desktopText(preferences.language, "settings.reset_confirmation_title")) },
            text = { Text(desktopText(preferences.language, "settings.reset_confirmation_description")) },
            confirmButton = {
                Button(onClick = {
                    resetConfirmationOpen = false
                    onResetData()
                }) { Text(desktopText(preferences.language, "settings.confirm_reset")) }
            },
            dismissButton = {
                TextButton(onClick = { resetConfirmationOpen = false }) { Text(desktopText(preferences.language, "common.cancel")) }
            }
        )
    }
}

private fun DesktopThemeColor.displayName(language: DesktopLanguage): String = desktopText(language, "theme.${name.lowercase()}")

private fun DesktopFontFamily.displayName(language: DesktopLanguage): String = desktopText(language, "font.${name.lowercase()}")

@Composable
private fun ThemeColorSelector(
    language: DesktopLanguage,
    selected: DesktopThemeColor,
    dark: Boolean,
    onSelect: (DesktopThemeColor) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DesktopThemeColor.entries.forEach { theme ->
            val chosen = theme == selected
            Surface(
                modifier = Modifier
                    .size(width = 86.dp, height = 58.dp)
                    .border(
                        width = if (chosen) 2.dp else 1.dp,
                        color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(7.dp)
                    )
                    .clickable { onSelect(theme) },
                shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        theme.previewColors(dark).forEach { color ->
                            Surface(Modifier.size(12.dp), shape = RoundedCornerShape(3.dp), color = color) {}
                        }
                    }
                    Text(theme.displayName(language), fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FontFamilySelector(
    language: DesktopLanguage,
    selected: DesktopFontFamily,
    onSelect: (DesktopFontFamily) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DesktopFontFamily.entries.forEach { family ->
            val chosen = family == selected
            Surface(
                modifier = Modifier
                    .size(width = 98.dp, height = 58.dp)
                    .border(
                        width = if (chosen) 2.dp else 1.dp,
                        color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(7.dp)
                    )
                    .clickable { onSelect(family) },
                shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                    Text(
                        "Aa",
                        fontSize = 20.sp,
                        fontFamily = family.composeFontFamily,
                        lineHeight = 22.sp
                    )
                    Text(family.displayName(language), fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun DesktopLanguageSelector(selected: DesktopLanguage, onSelect: (DesktopLanguage) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.widthIn(min = 180.dp, max = 240.dp)) {
            Text(selected.displayName, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Lucide.ChevronDown, null, Modifier.padding(start = 6.dp).size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DesktopLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        expanded = false
                        onSelect(language)
                    }
                )
            }
        }
    }
}

private val DesktopFontFamily.composeFontFamily: FontFamily
    get() = when (this) {
        DesktopFontFamily.SYSTEM -> FontFamily.Default
        DesktopFontFamily.SANS_SERIF -> FontFamily.SansSerif
        DesktopFontFamily.SERIF -> FontFamily.Serif
        DesktopFontFamily.MONOSPACE -> FontFamily.Monospace
    }

private val DesktopSearchProviderType.displayName: String
    get() = when (this) {
        DesktopSearchProviderType.SEARXNG -> "SearXNG"
        DesktopSearchProviderType.BRAVE -> "Brave"
        DesktopSearchProviderType.ZHIPU -> "智谱"
        DesktopSearchProviderType.TAVILY -> "Tavily"
        DesktopSearchProviderType.EXA -> "Exa"
        DesktopSearchProviderType.FIRECRAWL -> "Firecrawl"
        DesktopSearchProviderType.JINA -> "Jina"
        DesktopSearchProviderType.BOCHA -> "博查"
        DesktopSearchProviderType.PERPLEXITY -> "Perplexity"
        DesktopSearchProviderType.SERPER -> "Serper"
        DesktopSearchProviderType.OLLAMA -> "Ollama"
        DesktopSearchProviderType.METASO -> "秘塔"
        DesktopSearchProviderType.LINKUP -> "LinkUp"
        DesktopSearchProviderType.RIKKAHUB -> "RikkaHub"
    }

@Composable
private fun RequestOverridesEditor(
    language: DesktopLanguage,
    headers: List<DesktopCustomHeader>,
    bodies: List<DesktopCustomBody>,
    onHeadersChange: (List<DesktopCustomHeader>) -> Unit,
    onBodiesChange: (List<DesktopCustomBody>) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(desktopText(language, "request_overrides.headers"), fontWeight = FontWeight.Medium)
                Text(desktopText(language, "request_overrides.headers_help"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            IconButton(onClick = { onHeadersChange(headers + DesktopCustomHeader()) }) {
                Icon(Lucide.Plus, desktopText(language, "request_overrides.add_header"), Modifier.size(18.dp))
            }
        }
        headers.forEachIndexed { index, header ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    header.name,
                    { value ->
                        onHeadersChange(headers.mapIndexed { itemIndex, item ->
                            if (itemIndex == index) item.copy(name = value) else item
                        })
                    },
                    Modifier.weight(1f), label = { Text(desktopText(language, "request_overrides.header")) }, singleLine = true
                )
                OutlinedTextField(
                    header.value,
                    { value ->
                        onHeadersChange(headers.mapIndexed { itemIndex, item ->
                            if (itemIndex == index) item.copy(value = value) else item
                        })
                    },
                    Modifier.weight(1f), label = { Text(desktopText(language, "request_overrides.value")) }, singleLine = true
                )
                IconButton(
                    onClick = { onHeadersChange(headers.filterIndexed { itemIndex, _ -> itemIndex != index }) },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Icon(Lucide.Trash2, desktopText(language, "request_overrides.delete_header"), Modifier.size(18.dp)) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(desktopText(language, "request_overrides.body"), fontWeight = FontWeight.Medium)
                Text(desktopText(language, "request_overrides.body_help"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            IconButton(onClick = { onBodiesChange(bodies + DesktopCustomBody()) }) {
                Icon(Lucide.Plus, desktopText(language, "request_overrides.add_body"), Modifier.size(18.dp))
            }
        }
        bodies.forEachIndexed { index, body ->
            val validJson = body.key.isNotBlank() && runCatching { Json.parseToJsonElement(body.value) }.isSuccess
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    body.key,
                    { value ->
                        onBodiesChange(bodies.mapIndexed { itemIndex, item ->
                            if (itemIndex == index) item.copy(key = value) else item
                        })
                    },
                    Modifier.weight(0.7f), label = { Text(desktopText(language, "request_overrides.key")) }, isError = body.key.isBlank(), singleLine = true
                )
                OutlinedTextField(
                    body.value,
                    { value ->
                        onBodiesChange(bodies.mapIndexed { itemIndex, item ->
                            if (itemIndex == index) item.copy(value = value) else item
                        })
                    },
                    Modifier.weight(1.3f),
                    label = { Text(desktopText(language, "request_overrides.json_value")) },
                    isError = !validJson,
                    supportingText = if (validJson) null else ({ Text(desktopText(language, "request_overrides.invalid_json")) }),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                    minLines = 1,
                    maxLines = 5
                )
                IconButton(
                    onClick = { onBodiesChange(bodies.filterIndexed { itemIndex, _ -> itemIndex != index }) },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Icon(Lucide.Trash2, desktopText(language, "request_overrides.delete_body"), Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                title,
                Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun DesktopSettingsNavigation(
    activeSection: DesktopSettingsSection,
    language: DesktopLanguage,
    onSectionClick: (DesktopSettingsSection) -> Unit
) {
    Column(
        Modifier.widthIn(min = 196.dp, max = 196.dp).fillMaxHeight().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            desktopText(language, "settings.navigation"),
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        DesktopSettingsSection.entries.forEach { section ->
            val selected = activeSection == section
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onSectionClick(section) },
                shape = RoundedCornerShape(6.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                }
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        section.icon,
                        null,
                        Modifier.size(18.dp),
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        desktopText(language, section.labelKey),
                        Modifier.padding(start = 10.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    description: String,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        trailing()
    }
}

@Composable
private fun PreferenceSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(title, description) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    )
}

@Composable
private fun DesktopMcpSettings(
    language: DesktopLanguage,
    servers: List<DesktopMcpServer>,
    selectedServerIds: Set<String>,
    mcpClient: DesktopMcpClient,
    onServersChange: (List<DesktopMcpServer>) -> Unit,
    onSelectedServerIdsChange: (Set<String>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var syncingServerId by remember { mutableStateOf<String?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var expandedToolServerIds by remember { mutableStateOf(emptySet<String>()) }

    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(desktopText(language, "mcp_settings.title"), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(
                    desktopText(language, "mcp_settings.description"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = { onServersChange(servers + DesktopMcpServer()) }) {
                Icon(Lucide.Plus, desktopText(language, "mcp_settings.add_server"), Modifier.size(18.dp))
            }
        }
        servers.forEach { server ->
            var argumentsText by remember(server.id) { mutableStateOf(server.arguments.joinToString("\n")) }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = server.id in selectedServerIds,
                        onCheckedChange = { selected ->
                            onSelectedServerIdsChange(if (selected) selectedServerIds + server.id else selectedServerIds - server.id)
                        }
                    )
                    Text(
                        server.name.ifBlank { desktopText(language, "mcp_settings.unnamed_server") },
                        Modifier.padding(start = 10.dp).weight(1f)
                    )
                    IconButton(onClick = { onServersChange(servers.filterNot { it.id == server.id }) }) {
                        Icon(Lucide.Trash2, desktopText(language, "mcp_settings.delete_server"), Modifier.size(18.dp))
                    }
                }
                OutlinedTextField(
                    value = server.name,
                    onValueChange = { value -> onServersChange(servers.replaceMcpServer(server.id) { it.copy(name = value) }) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(desktopText(language, "mcp_settings.server_name")) },
                    supportingText = { Text(desktopText(language, "mcp_settings.server_name_help")) },
                    singleLine = true,
                    isError = server.name.isNotBlank() && !server.name.matches(Regex("[A-Za-z0-9]+"))
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    DesktopMcpTransport.entries.forEachIndexed { index, transport ->
                        SegmentedButton(
                            selected = server.transport == transport,
                            onClick = { onServersChange(servers.replaceMcpServer(server.id) { it.copy(transport = transport) }) },
                            modifier = Modifier.weight(1f),
                            shape = SegmentedButtonDefaults.itemShape(index, DesktopMcpTransport.entries.size),
                            label = {
                                Text(
                                    when (transport) {
                                        DesktopMcpTransport.STREAMABLE_HTTP -> "Stream HTTP"
                                        DesktopMcpTransport.SSE -> "SSE"
                                        DesktopMcpTransport.STDIO -> "Stdio"
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        )
                    }
                }
                if (server.transport == DesktopMcpTransport.STDIO) {
                    OutlinedTextField(
                        value = server.command,
                        onValueChange = { value -> onServersChange(servers.replaceMcpServer(server.id) { it.copy(command = value) }) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(desktopText(language, "mcp_settings.command")) },
                        placeholder = { Text("npx") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = argumentsText,
                        onValueChange = { value ->
                            argumentsText = value
                            onServersChange(servers.replaceMcpServer(server.id) {
                                it.copy(arguments = value.lineSequence().map(String::trim).filter(String::isNotBlank).toList())
                            })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(desktopText(language, "mcp_settings.arguments")) },
                        supportingText = { Text(desktopText(language, "mcp_settings.arguments_help")) },
                        minLines = 2
                    )
                    OutlinedTextField(
                        value = server.environment.joinToString("\n") { "${it.name}=${it.value}" },
                        onValueChange = { value ->
                            onServersChange(servers.replaceMcpServer(server.id) {
                                it.copy(environment = value.lineSequence().mapNotNull { line ->
                                    line.substringBefore('=', "").takeIf(String::isNotBlank)?.let { name ->
                                        DesktopCustomHeader(name, line.substringAfter('=', ""))
                                    }
                                }.toList())
                            })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(desktopText(language, "mcp_settings.environment")) },
                        supportingText = { Text(desktopText(language, "mcp_settings.environment_help")) },
                        minLines = 2
                    )
                } else {
                    OutlinedTextField(
                        value = server.url,
                        onValueChange = { value -> onServersChange(servers.replaceMcpServer(server.id) { it.copy(url = value) }) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(desktopText(language, "mcp_settings.server_url")) },
                        placeholder = { Text("https://example.com/mcp") },
                        singleLine = true
                    )
                }
                PreferenceSwitch(
                    desktopText(language, "mcp_settings.enable_server"),
                    desktopText(language, "mcp_settings.enable_server_help"),
                    server.enabled
                ) { enabled ->
                    onServersChange(servers.replaceMcpServer(server.id) { it.copy(enabled = enabled) })
                }
                OutlinedButton(
                    enabled = syncingServerId == null && server.name.matches(Regex("[A-Za-z0-9]+")) &&
                        (if (server.transport == DesktopMcpTransport.STDIO) server.command.isNotBlank() else server.url.isNotBlank()),
                    onClick = {
                        syncingServerId = server.id
                        syncError = null
                        scope.launch {
                            runCatching { mcpClient.syncTools(server) }
                                .onSuccess { tools ->
                                    val prior = server.tools.associateBy { it.name }
                                    onServersChange(servers.replaceMcpServer(server.id) {
                                        it.copy(tools = tools.map { tool ->
                                            tool.copy(enabled = prior[tool.name]?.enabled ?: true)
                                        })
                                    })
                                }
                                .onFailure { syncError = it.message ?: desktopText(language, "mcp_settings.sync_failed") }
                            syncingServerId = null
                        }
                    }
                ) {
                    if (syncingServerId == server.id) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Lucide.Download, null, Modifier.size(17.dp))
                    Text(desktopText(language, "mcp_settings.sync_tools"), Modifier.padding(start = 7.dp))
                }
                if (server.tools.isNotEmpty()) {
                    val toolsExpanded = server.id in expandedToolServerIds
                    val enabledToolCount = server.tools.count { it.enabled }
                    TextButton(
                        onClick = {
                            expandedToolServerIds = if (toolsExpanded) {
                                expandedToolServerIds - server.id
                            } else {
                                expandedToolServerIds + server.id
                            }
                        }
                    ) {
                        Text(
                            if (toolsExpanded) {
                                desktopText(language, "mcp_settings.collapse_tools")
                            } else {
                                desktopText(language, "mcp_settings.manage_tools")
                                    .replace("%d", server.tools.size.toString())
                                    .replace("%e", enabledToolCount.toString())
                            }
                        )
                        Icon(
                            if (toolsExpanded) Lucide.ChevronUp else Lucide.ChevronDown,
                            null,
                            Modifier.padding(start = 4.dp).size(17.dp)
                        )
                    }
                    if (toolsExpanded) {
                        server.tools.forEach { tool ->
                            PreferenceSwitch(
                                tool.name,
                                tool.description.ifBlank { desktopText(language, "mcp_settings.tool") },
                                tool.enabled
                            ) { enabled ->
                                onServersChange(servers.replaceMcpServer(server.id) {
                                    it.copy(tools = it.tools.map { existing ->
                                        if (existing.name == tool.name) existing.copy(enabled = enabled) else existing
                                    })
                                })
                            }
                        }
                    }
                }
                SettingsDivider()
            }
        }
        syncError?.let { Text(it, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
    }
}

private fun List<DesktopMcpServer>.replaceMcpServer(
    id: String,
    transform: (DesktopMcpServer) -> DesktopMcpServer
): List<DesktopMcpServer> = map { if (it.id == id) transform(it) else it }

private sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Testing : ConnectionState
    data class Success(val models: List<String>) : ConnectionState
    data class Failure(val message: String) : ConnectionState
}

@Composable
private fun ConnectionResult(state: ConnectionState, language: DesktopLanguage) {
    when (state) {
        ConnectionState.Idle, ConnectionState.Testing -> Unit
        is ConnectionState.Success -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.CircleCheck, null, Modifier.size(17.dp), tint = Color(0xFF2E7D32))
            Text(
                desktopText(language, "provider.connection_success").replace("%d", state.models.size.toString()),
                Modifier.padding(start = 7.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        is ConnectionState.Failure -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.CircleX, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.error)
            Text(
                state.message,
                Modifier.padding(start = 7.dp).weight(1f),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }
    }
}
