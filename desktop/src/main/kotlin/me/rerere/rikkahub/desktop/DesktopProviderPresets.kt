package me.rerere.rikkahub.desktop

internal data class DesktopProviderPreset(
    val name: String,
    val baseUrl: String,
    val balanceOptions: DesktopBalanceOptions = DesktopBalanceOptions(),
    val protocol: DesktopProviderProtocol = DesktopProviderProtocol.OPENAI_CHAT_COMPLETIONS
)

internal val DesktopProviderPresets = listOf(
    DesktopProviderPreset("RikkaHub", "https://api.rikka-ai.com/v1"),
    DesktopProviderPreset("OpenAI", "https://api.openai.com/v1"),
    DesktopProviderPreset(
        "Google Gemini",
        "https://generativelanguage.googleapis.com/v1beta",
        protocol = DesktopProviderProtocol.GEMINI_GENERATE_CONTENT
    ),
    DesktopProviderPreset(
        "Anthropic",
        "https://api.anthropic.com/v1",
        protocol = DesktopProviderProtocol.ANTHROPIC_MESSAGES
    ),
    DesktopProviderPreset("AiHubMix", "https://aihubmix.com/v1"),
    DesktopProviderPreset(
        "硅基流动", "https://api.siliconflow.cn/v1",
        DesktopBalanceOptions(true, "/user/info", "data.totalBalance")
    ),
    DesktopProviderPreset(
        "DeepSeek", "https://api.deepseek.com/v1",
        DesktopBalanceOptions(true, "/user/balance", "balance_infos[0].total_balance")
    ),
    DesktopProviderPreset(
        "OpenRouter", "https://openrouter.ai/api/v1",
        DesktopBalanceOptions(true, "/credits", "data.total_credits - data.total_usage")
    ),
    DesktopProviderPreset(
        "Vercel AI Gateway", "https://ai-gateway.vercel.sh/v1",
        DesktopBalanceOptions(true, "/credits", "balance")
    ),
    DesktopProviderPreset("小马算力", "https://api.tokenpony.cn/v1"),
    DesktopProviderPreset(
        "月之暗面", "https://api.moonshot.cn/v1",
        DesktopBalanceOptions(true, "/users/me/balance", "data.available_balance")
    ),
    DesktopProviderPreset("阿里云百炼", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    DesktopProviderPreset("火山引擎", "https://ark.cn-beijing.volces.com/api/v3"),
    DesktopProviderPreset("智谱AI开放平台", "https://open.bigmodel.cn/api/paas/v4"),
    DesktopProviderPreset("阶跃星辰", "https://api.stepfun.com/v1"),
    DesktopProviderPreset("302.AI", "https://api.302.ai/v1"),
    DesktopProviderPreset("腾讯Hunyuan", "https://api.hunyuan.cloud.tencent.com/v1"),
    DesktopProviderPreset("xAI", "https://api.x.ai/v1"),
    DesktopProviderPreset("随想AI网关", "https://sui-xiang.com/v1"),
    DesktopProviderPreset("MIMO", "https://api.xiaomimimo.com/v1"),
    DesktopProviderPreset("AckAI", "https://ackai.fun/v1")
)
