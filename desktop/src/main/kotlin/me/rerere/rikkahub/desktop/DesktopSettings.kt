package me.rerere.rikkahub.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

internal enum class DesktopSettingsSection(val itemIndex: Int) {
    GENERAL(0),
    DATA(3),
    ASSISTANTS(4),
    PROVIDERS(5)
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
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialSection.itemIndex)
    var activeSection by remember { mutableStateOf(initialSection) }

    LaunchedEffect(initialSection) {
        activeSection = initialSection
        listState.scrollToItem(initialSection.itemIndex)
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = if (showMenu) onMenu else onBack) {
                Icon(if (showMenu) Lucide.Menu else Lucide.ArrowLeft, "返回")
            }
            Column(Modifier.padding(start = 4.dp)) {
                Text("设置", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(
                    "外观、交互与模型服务",
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
                    onSectionClick = { section ->
                        activeSection = section
                        scope.launch { listState.animateScrollToItem(section.itemIndex) }
                    }
                )
                Box(
                    Modifier.fillMaxHeight().width(1.dp).background(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                )
            }

            Box(
                Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().widthIn(max = 880.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = 22.dp,
                    bottom = 36.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item("general") {
                    SettingsSection("通用设置", Lucide.Palette) {
                        SettingsRow(
                            title = "颜色模式",
                            description = "跟随系统，或固定使用浅色或深色主题"
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
                                                    DesktopColorMode.SYSTEM -> "跟随系统"
                                                    DesktopColorMode.LIGHT -> "浅色"
                                                    DesktopColorMode.DARK -> "深色"
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        SettingsDivider()
                        SettingsRow(
                            title = "聊天字体大小",
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

                item("message-display") {
                    SettingsSection("消息显示", Lucide.MessageSquareText) {
                        PreferenceSwitch(
                            "显示用户头像",
                            "在用户消息旁显示头像",
                            preferences.showUserAvatar
                        ) { onPreferencesChange(preferences.copy(showUserAvatar = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            "显示模型图标",
                            "在助手消息旁显示模型图标",
                            preferences.showModelIcon
                        ) { onPreferencesChange(preferences.copy(showModelIcon = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            "显示模型名称",
                            "在助手消息上方显示模型名称",
                            preferences.showModelName
                        ) { onPreferencesChange(preferences.copy(showModelName = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            "助手气泡",
                            "用填充气泡显示助手文本",
                            preferences.showAssistantBubble
                        ) { onPreferencesChange(preferences.copy(showAssistantBubble = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            "消息时间",
                            "在每条消息标题中显示创建时间",
                            preferences.showMessageTimestamp
                        ) { onPreferencesChange(preferences.copy(showMessageTimestamp = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            "思考内容",
                            "显示兼容模型返回的推理内容",
                            preferences.showReasoning
                        ) { onPreferencesChange(preferences.copy(showReasoning = it)) }
                        if (preferences.showReasoning) {
                            SettingsDivider()
                            PreferenceSwitch(
                                "完成后折叠思考内容",
                                "仅在生成进行中展开推理内容",
                                preferences.autoCollapseReasoning
                            ) { onPreferencesChange(preferences.copy(autoCollapseReasoning = it)) }
                        }
                        SettingsDivider()
                        PreferenceSwitch(
                            "代码块自动换行",
                            "长代码行自动换行，而不是横向滚动",
                            preferences.codeBlockAutoWrap
                        ) { onPreferencesChange(preferences.copy(codeBlockAutoWrap = it)) }
                    }
                }

                item("interaction") {
                    SettingsSection("交互", Lucide.Keyboard) {
                        PreferenceSwitch(
                            "按 Enter 发送",
                            "关闭后，Ctrl+Enter 发送，Enter 换行",
                            preferences.sendOnEnter
                        ) { onPreferencesChange(preferences.copy(sendOnEnter = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            "自动滚动",
                            "生成时跟随新的推理和回复内容",
                            preferences.enableAutoScroll
                        ) { onPreferencesChange(preferences.copy(enableAutoScroll = it)) }
                        SettingsDivider()
                        PreferenceSwitch(
                            "消息导航",
                            "滚动消息后显示跳转到顶部、上一条、下一条和底部的按钮",
                            preferences.showMessageJumper
                        ) { onPreferencesChange(preferences.copy(showMessageJumper = it)) }
                        if (preferences.showMessageJumper) {
                            SettingsDivider()
                            PreferenceSwitch(
                                "消息导航置左",
                                "默认显示在消息区域右侧",
                                preferences.messageJumperOnLeft
                            ) { onPreferencesChange(preferences.copy(messageJumperOnLeft = it)) }
                        }
                    }
                }

                item("data") {
                    SettingsSection("数据、备份与联网搜索", Lucide.Save) {
                        SettingsRow(
                            title = "导出备份",
                            description = "将服务商、助手、偏好和对话保存为 JSON"
                        ) {
                            OutlinedButton(onClick = { backupStatus = onExportData() }) {
                                Icon(Lucide.Download, null, Modifier.size(17.dp))
                                Text("导出", Modifier.padding(start = 7.dp))
                            }
                        }
                        SettingsDivider()
                        SettingsRow(
                            title = "导入备份",
                            description = "使用 RikkaHub 备份替换当前桌面端数据"
                        ) {
                            OutlinedButton(onClick = { backupStatus = onImportData() }) {
                                Icon(Lucide.Upload, null, Modifier.size(17.dp))
                                Text("导入", Modifier.padding(start = 7.dp))
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
                            title = "重置全部数据",
                            description = "删除本机的服务商、助手、偏好和对话，此操作无法撤销"
                        ) {
                            OutlinedButton(onClick = { resetConfirmationOpen = true }) {
                                Icon(Lucide.Trash2, null, Modifier.size(17.dp))
                                Text("重置", Modifier.padding(start = 7.dp))
                            }
                        }
                        SettingsDivider()
                        SettingsRow(
                            title = "联网搜索服务",
                            description = "配置后优先使用外部搜索服务；未配置时使用模型服务商的原生搜索"
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
                                                    webSearchSettings.copy(providerType = provider)
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
                                        label = { Text("SearXNG 地址") },
                                        placeholder = { Text("https://searx.example.com") },
                                        singleLine = true
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = webSearchSettings.apiKey,
                                        onValueChange = { onWebSearchSettingsChange(webSearchSettings.copy(apiKey = it.trim())) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("${webSearchSettings.providerType.displayName} API 密钥") },
                                        singleLine = true
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "每次搜索返回 ${webSearchSettings.resultCount.coerceIn(1, 10)} 条结果",
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
                                                "联网搜索连接正常"
                                            }.getOrElse { error -> "搜索测试失败：${error.message ?: "未知错误"}" }
                                            webSearchTesting = false
                                        }
                                    }
                                ) {
                                    if (webSearchTesting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    else Text("测试搜索")
                                }
                                webSearchTestStatus?.let { status ->
                                    Text(
                                        status,
                                        Modifier.padding(top = 6.dp),
                                        color = if (status.startsWith("联网搜索")) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item("assistants") {
                    SettingsSection("助手", Lucide.Bot) {
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
                                Icon(Lucide.Plus, "添加助手", Modifier.size(18.dp))
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
                                    label = { Text("助手名称") },
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = { onAssistantCopy(selectedAssistant.id) },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(Lucide.Copy, "复制助手", Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onAssistantDelete(selectedAssistant.id) },
                                    enabled = assistants.size > 1,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(Lucide.Trash2, "删除助手", Modifier.size(18.dp))
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
                                label = { Text("标签（逗号分隔）") },
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
                                    label = { Text("模型（留空使用服务商默认值）") },
                                    trailingIcon = {
                                        if (assistantProvider.discoveredModels.isNotEmpty()) {
                                            IconButton(onClick = { assistantModelMenuOpen = true }) {
                                                Icon(Lucide.ChevronDown, "选择模型")
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
                                label = { Text("系统提示词（留空使用服务商默认值）") },
                                minLines = 4
                            )
                            OutlinedTextField(
                                draftAssistant.messageTemplate,
                                { draftAssistant = draftAssistant.copy(messageTemplate = it) },
                                Modifier.fillMaxWidth(),
                                label = { Text("消息模板") },
                                supportingText = {
                                    Text(
                                        if (messageTemplateValid) {
                                            "变量：{{ message }}、{{ role }}、{{ date }}、{{ time }}"
                                        } else {
                                            "需要包含 {{ message }} 的有效 Pebble 模板"
                                        }
                                    )
                                },
                                isError = !messageTemplateValid,
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                                minLines = 3,
                                maxLines = 8
                            )
                            PreferenceSwitch(
                                "对话系统提示词",
                                "允许单个对话覆盖此助手的系统提示词",
                                draftAssistant.allowConversationSystemPrompt
                            ) {
                                draftAssistant = draftAssistant.copy(allowConversationSystemPrompt = it)
                            }
                            PreferenceSwitch(
                                "对话世界书",
                                "允许单个对话启用或禁用此助手的提示词注入",
                                draftAssistant.allowConversationPromptInjection
                            ) {
                                draftAssistant = draftAssistant.copy(allowConversationPromptInjection = it)
                            }
                            PreferenceSwitch(
                                "联网搜索",
                                "默认在新对话中启用兼容 OpenAI 的联网搜索",
                                draftAssistant.enableWebSearch
                            ) {
                                draftAssistant = draftAssistant.copy(enableWebSearch = it)
                            }
                            PreferenceSwitch(
                                "本地时间工具",
                                "允许模型按需读取此设备的本地时间和时区",
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
                                "流式输出",
                                "实时显示回复内容，而不是等待完整回复",
                                draftAssistant.streamOutput
                            ) {
                                draftAssistant = draftAssistant.copy(streamOutput = it)
                            }
                            PreferenceSwitch(
                                "记忆",
                                "允许模型记录、更新和使用跨对话的长期信息",
                                draftAssistant.enableMemory
                            ) {
                                draftAssistant = draftAssistant.copy(enableMemory = it)
                            }
                            if (draftAssistant.enableMemory) {
                                PreferenceSwitch(
                                    "全局共享记忆",
                                    "与其他启用共享记忆的助手共用记忆库",
                                    draftAssistant.useGlobalMemory
                                ) {
                                    draftAssistant = draftAssistant.copy(useGlobalMemory = it)
                                }
                                if (draftAssistant.useGlobalMemory) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("全局记忆", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                        IconButton(onClick = { onGlobalMemoriesChange(globalMemories + DesktopMemory()) }) {
                                            Icon(Lucide.Plus, "添加全局记忆", Modifier.size(18.dp))
                                        }
                                    }
                                    globalMemories.forEachIndexed { index, memory ->
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                            OutlinedTextField(memory.content, { value ->
                                                onGlobalMemoriesChange(globalMemories.mapIndexed { i, item -> if (i == index) item.copy(content = value) else item })
                                            }, Modifier.weight(1f), label = { Text("共享事实或偏好") }, minLines = 2)
                                            IconButton(onClick = { onGlobalMemoriesChange(globalMemories.filterIndexed { i, _ -> i != index }) }) {
                                                Icon(Lucide.Trash2, "删除全局记忆", Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                                if (!draftAssistant.useGlobalMemory) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("记忆条目", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                    IconButton(
                                        onClick = {
                                            draftAssistant = draftAssistant.copy(
                                                memories = draftAssistant.memories + DesktopMemory()
                                            )
                                        }
                                    ) { Icon(Lucide.Plus, "添加记忆", Modifier.size(18.dp)) }
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
                                            label = { Text("事实或偏好") },
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
                                        ) { Icon(Lucide.Trash2, "删除记忆", Modifier.size(18.dp)) }
                                    }
                                }
                                }
                            }
                            SettingsDivider()
                            DesktopMcpSettings(
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
                                    label = { Text("温度") },
                                    supportingText = { Text("留空继承服务商设置") },
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
                                    label = { Text("最大输出 Token") },
                                    supportingText = { Text("留空继承服务商设置") },
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
                                    label = { Text("上下文消息数") },
                                    supportingText = { Text("留空保留全部") },
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
                                    supportingText = { Text("留空继承服务商设置") },
                                    singleLine = true
                                )
                                Column(Modifier.weight(2f)) {
                                    Text("推理强度", fontSize = 12.sp)
                                    SingleChoiceSegmentedButtonRow {
                                        listOf("", "low", "medium", "high").forEachIndexed { index, effort ->
                                            SegmentedButton(
                                                selected = draftAssistant.reasoningEffort == effort,
                                                onClick = {
                                                    draftAssistant = draftAssistant.copy(reasoningEffort = effort)
                                                },
                                                shape = SegmentedButtonDefaults.itemShape(index, 4),
                                                label = { Text(effort.ifBlank { "默认" }) }
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
                                    Text("预设消息", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "在每个新对话中添加示例消息",
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
                                    Text("添加", Modifier.padding(start = 7.dp))
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
                                                label = { Text(role.replaceFirstChar { it.uppercase() }) }
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
                                        label = { Text("消息") },
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
                                        Icon(Lucide.Trash2, "删除预设消息", Modifier.size(18.dp))
                                    }
                                }
                            }
                            SettingsDivider()
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("快捷消息", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "从聊天输入框插入可复用提示词",
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
                                    Text("添加", Modifier.padding(start = 7.dp))
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
                                        label = { Text("标题") },
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
                                        label = { Text("内容") },
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
                                        Icon(Lucide.Trash2, "删除快捷消息", Modifier.size(18.dp))
                                    }
                                }
                            }
                            SettingsDivider()
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("正则转换", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "转换用户输入、助手输出或显示文本",
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
                                    Text("添加", Modifier.padding(start = 7.dp))
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
                                            label = { Text("规则名称") },
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
                                            Icon(Lucide.Trash2, "删除正则规则", Modifier.size(18.dp))
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
                                            label = { Text("匹配正则") },
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
                                            label = { Text("替换为") },
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
                                                label = { Text(role.replaceFirstChar { it.uppercase() }) }
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
                                            label = { Text("仅视觉显示") }
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
                                    Text("提示词注入 / 世界书", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "按关键词匹配上下文，或设为常驻注入",
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
                                    Text("添加", Modifier.padding(start = 7.dp))
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
                                            label = { Text("条目名称") },
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
                                        ) { Icon(Lucide.Trash2, "删除注入条目", Modifier.size(18.dp)) }
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
                                        label = { Text("注入内容") },
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
                                        label = { Text("关键词（逗号分隔）") },
                                        supportingText = { Text("留空时请开启常驻") },
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
                                            label = { Text("优先级") },
                                            supportingText = { Text("数值越大越靠前") },
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
                                            label = { Text("扫描消息数") },
                                            supportingText = { Text("用于关键词匹配") },
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
                                                label = { Text("注入深度") },
                                                supportingText = { Text("从末尾倒数") },
                                                singleLine = true
                                            )
                                        }
                                    }
                                    Text("注入角色", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                label = { Text(role) }
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
                                                            DesktopInjectionPosition.BEFORE_SYSTEM_PROMPT -> "系统前"
                                                            DesktopInjectionPosition.AFTER_SYSTEM_PROMPT -> "系统后"
                                                            DesktopInjectionPosition.TOP_OF_CHAT -> "聊天顶部"
                                                            DesktopInjectionPosition.BOTTOM_OF_CHAT -> "聊天底部"
                                                            DesktopInjectionPosition.AT_DEPTH -> "指定深度"
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
                                            label = { Text("常驻") }
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
                                            label = { Text("正则关键词") }
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
                                            label = { Text("区分大小写") }
                                        )
                                    }
                                }
                            }
                            SettingsDivider()
                            RequestOverridesEditor(
                                headers = draftAssistant.customHeaders,
                                bodies = draftAssistant.customBodies,
                                onHeadersChange = { draftAssistant = draftAssistant.copy(customHeaders = it) },
                                onBodiesChange = { draftAssistant = draftAssistant.copy(customBodies = it) }
                            )
                            Button(
                                onClick = {
                                    onAssistantSave(draftAssistant)
                                    scope.launch { feedbackHostState.showSnackbar("助手设置已保存") }
                                },
                                enabled = draftAssistant.name.isNotBlank() &&
                                    draftAssistant.presetMessages.all { it.content.isNotBlank() } &&
                                    draftAssistant.quickMessages.all { it.content.isNotBlank() } &&
                                    messageTemplateValid && regexRulesValid && assistantBodiesValid && memoriesValid && injectionsValid,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Lucide.Save, null, Modifier.size(17.dp))
                                Text("保存助手", Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }

                item("providers") {
                    SettingsSection("模型与服务", Lucide.ServerCog) {
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
                                    Text("应用预设")
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
                                            }
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = onProviderAdd, modifier = Modifier.size(40.dp)) {
                                Icon(Lucide.Plus, "添加服务商", Modifier.size(18.dp))
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
                                    label = { Text("服务商名称") },
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = { onProviderDelete(selectedProvider.id) },
                                    enabled = providers.size > 1,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(Lucide.Trash2, "删除服务商", Modifier.size(18.dp))
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
                                label = { Text("基础 URL") },
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
                                label = { Text("API 密钥") },
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
                                            if (apiKeyVisible) "隐藏 API 密钥" else "显示 API 密钥"
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
                                    label = { Text("模型") },
                                    leadingIcon = { Icon(Lucide.Bot, null) },
                                    trailingIcon = {
                                        if (draftProvider.discoveredModels.isNotEmpty()) {
                                            IconButton(onClick = { modelMenuOpen = true }) {
                                                Icon(Lucide.ChevronDown, "选择已发现的模型")
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
                                label = { Text("标题模型") },
                                supportingText = { Text("留空时使用聊天模型") },
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
                                label = { Text("标题提示词") },
                                supportingText = { Text("支持 {locale} 与 {content} 占位符") },
                                minLines = 4,
                                maxLines = 8
                            )
                            Text(
                                "温度  ${"%.1f".format(draftProvider.config.temperature)}",
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
                            Text("推理强度", fontSize = 13.sp)
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
                                        label = { Text(effort.ifBlank { "默认" }) }
                                    )
                                }
                            }
                            PreferenceSwitch(
                                "Token 用量",
                                "从兼容的流式 API 请求输入和输出 Token 计数",
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
                                label = { Text("最大输出 Token") },
                                supportingText = { Text("留空使用服务商默认值") },
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
                                label = { Text("系统提示词") },
                                minLines = 3,
                                maxLines = 7
                            )
                            SettingsDivider()
                            PreferenceSwitch(
                                "余额查询",
                                "通过服务商 API 查询账户余额或额度",
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
                                    label = { Text("余额 API 路径") },
                                    supportingText = { Text("可填写相对路径或完整 URL") },
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
                                    label = { Text("结果 JSON 路径") },
                                    supportingText = { Text("例如 data.balance 或 data.items[0].amount") },
                                    singleLine = true
                                )
                                OutlinedButton(
                                    onClick = {
                                        balanceStatus = "正在查询余额..."
                                        scope.launch {
                                            balanceStatus = runCatching { client.getBalance(draftProvider.config) }
                                                .fold(
                                                    onSuccess = { "当前余额：$it" },
                                                    onFailure = { "余额查询失败：${it.message}" }
                                                )
                                        }
                                    },
                                    enabled = draftProvider.config.balanceOptions.resultPath.isNotBlank()
                                ) { Text("查询余额") }
                                balanceStatus?.let { status ->
                                    Text(
                                        status,
                                        color = if (status.startsWith("余额查询失败")) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            RequestOverridesEditor(
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
                            ConnectionResult(connectionState)
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
                                    Text("测试并获取模型", Modifier.padding(start = 7.dp))
                                }
                                Button(
                                    onClick = {
                                        onProviderSave(draftProvider)
                                        scope.launch { feedbackHostState.showSnackbar("服务商设置已保存") }
                                    },
                                    enabled = draftProvider.name.isNotBlank() && providerBodiesValid &&
                                        draftProvider.config.baseUrl.isNotBlank() &&
                                        draftProvider.config.model.isNotBlank()
                                ) {
                                    Icon(Lucide.Save, null, Modifier.size(17.dp))
                                    Text("保存服务商", Modifier.padding(start = 7.dp))
                                }
                            }
                        }
                    }
                }

                item("back-to-chat") {
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Icon(Lucide.ChevronLeft, null, Modifier.size(17.dp))
                        Text("返回对话", Modifier.padding(start = 6.dp))
                    }
                }
                }
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
            title = { Text("重置全部数据？") },
            text = { Text("这会永久删除桌面端的服务商密钥、助手、设置和所有对话。") },
            confirmButton = {
                Button(onClick = {
                    resetConfirmationOpen = false
                    onResetData()
                }) { Text("确认重置") }
            },
            dismissButton = {
                TextButton(onClick = { resetConfirmationOpen = false }) { Text("取消") }
            }
        )
    }
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
    headers: List<DesktopCustomHeader>,
    bodies: List<DesktopCustomBody>,
    onHeadersChange: (List<DesktopCustomHeader>) -> Unit,
    onBodiesChange: (List<DesktopCustomBody>) -> Unit
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("自定义请求头", fontWeight = FontWeight.Medium)
                Text("添加到聊天和模型请求中", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            IconButton(onClick = { onHeadersChange(headers + DesktopCustomHeader()) }) {
                Icon(Lucide.Plus, "添加自定义请求头", Modifier.size(18.dp))
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
                    Modifier.weight(1f), label = { Text("请求头") }, singleLine = true
                )
                OutlinedTextField(
                    header.value,
                    { value ->
                        onHeadersChange(headers.mapIndexed { itemIndex, item ->
                            if (itemIndex == index) item.copy(value = value) else item
                        })
                    },
                    Modifier.weight(1f), label = { Text("值") }, singleLine = true
                )
                IconButton(
                    onClick = { onHeadersChange(headers.filterIndexed { itemIndex, _ -> itemIndex != index }) },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Icon(Lucide.Trash2, "删除自定义请求头", Modifier.size(18.dp)) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("自定义请求体", fontWeight = FontWeight.Medium)
                Text("将 JSON 合并到请求顶层字段", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            IconButton(onClick = { onBodiesChange(bodies + DesktopCustomBody()) }) {
                Icon(Lucide.Plus, "添加自定义请求体", Modifier.size(18.dp))
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
                    Modifier.weight(0.7f), label = { Text("键") }, isError = body.key.isBlank(), singleLine = true
                )
                OutlinedTextField(
                    body.value,
                    { value ->
                        onBodiesChange(bodies.mapIndexed { itemIndex, item ->
                            if (itemIndex == index) item.copy(value = value) else item
                        })
                    },
                    Modifier.weight(1.3f),
                    label = { Text("JSON 值") },
                    isError = !validJson,
                    supportingText = if (validJson) null else ({ Text("请输入有效 JSON") }),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                    minLines = 1,
                    maxLines = 5
                )
                IconButton(
                    onClick = { onBodiesChange(bodies.filterIndexed { itemIndex, _ -> itemIndex != index }) },
                    modifier = Modifier.padding(top = 8.dp)
                ) { Icon(Lucide.Trash2, "删除自定义请求体", Modifier.size(18.dp)) }
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
    onSectionClick: (DesktopSettingsSection) -> Unit
) {
    val sections = listOf(
        Triple(DesktopSettingsSection.GENERAL, "通用设置", Lucide.Palette),
        Triple(DesktopSettingsSection.ASSISTANTS, "助手", Lucide.Bot),
        Triple(DesktopSettingsSection.PROVIDERS, "模型与服务", Lucide.ServerCog),
        Triple(DesktopSettingsSection.DATA, "数据与备份", Lucide.Save)
    )

    Column(
        Modifier.widthIn(min = 196.dp, max = 196.dp).fillMaxHeight().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "设置分类",
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        sections.forEach { (section, label, icon) ->
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
                        icon,
                        null,
                        Modifier.size(18.dp),
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Text(
                        label,
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
                Text("MCP 服务器", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("同步远程工具后，为此助手启用所需服务器", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            IconButton(onClick = { onServersChange(servers + DesktopMcpServer()) }) {
                Icon(Lucide.Plus, "添加 MCP 服务器", Modifier.size(18.dp))
            }
        }
        servers.forEach { server ->
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
                    Text(server.name.ifBlank { "未命名 MCP 服务器" }, Modifier.padding(start = 10.dp).weight(1f))
                    IconButton(onClick = { onServersChange(servers.filterNot { it.id == server.id }) }) {
                        Icon(Lucide.Trash2, "删除 MCP 服务器", Modifier.size(18.dp))
                    }
                }
                OutlinedTextField(
                    value = server.name,
                    onValueChange = { value -> onServersChange(servers.replaceMcpServer(server.id) { it.copy(name = value) }) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("服务器名称") },
                    supportingText = { Text("仅允许字母和数字，用于生成稳定的工具名") },
                    singleLine = true,
                    isError = server.name.isNotBlank() && !server.name.matches(Regex("[A-Za-z0-9]+"))
                )
                OutlinedTextField(
                    value = server.url,
                    onValueChange = { value -> onServersChange(servers.replaceMcpServer(server.id) { it.copy(url = value) }) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("服务器 URL") },
                    placeholder = { Text("https://example.com/mcp") },
                    singleLine = true
                )
                SingleChoiceSegmentedButtonRow {
                    DesktopMcpTransport.entries.forEachIndexed { index, transport ->
                        SegmentedButton(
                            selected = server.transport == transport,
                            onClick = { onServersChange(servers.replaceMcpServer(server.id) { it.copy(transport = transport) }) },
                            shape = SegmentedButtonDefaults.itemShape(index, DesktopMcpTransport.entries.size),
                            label = { Text(if (transport == DesktopMcpTransport.STREAMABLE_HTTP) "Streamable HTTP" else "SSE") }
                        )
                    }
                }
                PreferenceSwitch("启用服务器", "关闭后不会向模型暴露其中的工具", server.enabled) { enabled ->
                    onServersChange(servers.replaceMcpServer(server.id) { it.copy(enabled = enabled) })
                }
                OutlinedButton(
                    enabled = syncingServerId == null && server.name.matches(Regex("[A-Za-z0-9]+")) && server.url.isNotBlank(),
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
                                .onFailure { syncError = it.message ?: "同步 MCP 工具失败" }
                            syncingServerId = null
                        }
                    }
                ) {
                    if (syncingServerId == server.id) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Lucide.Download, null, Modifier.size(17.dp))
                    Text("同步工具", Modifier.padding(start = 7.dp))
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
                                "收起工具"
                            } else {
                                "管理 ${server.tools.size} 个工具（已启用 $enabledToolCount 个）"
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
                            PreferenceSwitch(tool.name, tool.description.ifBlank { "MCP 工具" }, tool.enabled) { enabled ->
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
private fun ConnectionResult(state: ConnectionState) {
    when (state) {
        ConnectionState.Idle, ConnectionState.Testing -> Unit
        is ConnectionState.Success -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.CircleCheck, null, Modifier.size(17.dp), tint = Color(0xFF2E7D32))
            Text(
                "已连接 · 可用 ${state.models.size} 个模型",
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
