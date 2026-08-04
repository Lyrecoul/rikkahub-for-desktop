package me.rerere.rikkahub.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun providerIconPath(name: String): String? {
    val value = name.trim().lowercase()
    return when {
        "deepseek" in value -> "deepseek-color.svg"
        "硅基" in value || "siliconflow" in value -> "siliconflow.svg"
        "openrouter" in value -> "openrouter.svg"
        "月之暗面" in value || "moonshot" in value || "kimi" in value -> "kimi-color.svg"
        "阿里" in value || "qwen" in value || "百炼" in value -> "qwen-color.svg"
        "火山" in value || "字节" in value || "doubao" in value -> "doubao-color.svg"
        "智谱" in value || "zhipu" in value || "glm" in value -> "zhipu-color.svg"
        "阶跃" in value || "stepfun" in value -> "stepfun-color.svg"
        "gemini" in value || "google" in value -> "gemini-color.svg"
        "gemma" in value -> "gemma-color.svg"
        "claude" in value || "anthropic" in value -> "claude-color.svg"
        "openai" in value || "chatgpt" in value || value.startsWith("gpt") ||
            Regex("^o[134](?:[-._].*)?$").matches(value) -> "openai.svg"
        "aihubmix" in value -> "aihubmix-color.svg"
        "xiaomi" in value || "mimo" in value -> "xiaomimimo.svg"
        "nvidia" in value -> "nvidia-color.svg"
        "302.ai" in value || "302ai" in value -> "302ai.svg"
        "小马" in value || "tokenpony" in value -> "tokenpony.svg"
        "grok" in value || value == "xai" -> "grok.svg"
        "groq" in value -> "groq.svg"
        "cloudflare" in value -> "cloudflare-color.svg"
        "minimax" in value -> "minimax-color.svg"
        "perplexity" in value -> "perplexity-color.svg"
        "internlm" in value -> "internlm-color.svg"
        "ollama" in value -> "ollama.svg"
        "longcat" in value -> "longcat-color.svg"
        "mistral" in value || "mixtral" in value -> "mistral-color.svg"
        "cerebras" in value -> "cerebras-color.svg"
        "ppio" in value -> "ppio-color.svg"
        "vercel" in value -> "vercel.svg"
        "llama" in value || value == "meta" -> "meta-color.svg"
        "hunyuan" in value || "混元" in value -> "hunyuan-color.svg"
        "cohere" in value || "command-r" in value -> "cohere-color.svg"
        "brave" in value -> "brave.svg"
        "tavily" in value -> "tavily.png"
        "exa" in value -> "exa.png"
        "rikkahub" in value -> "rikkahub.svg"
        else -> null
    }
}

@Composable
internal fun DesktopProviderIcon(
    name: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    val compact = iconSize <= 16.dp
    val labelSize = if (compact) 12.sp else 16.sp
    Box(
        modifier = modifier
            .size(iconSize)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val path = providerIconPath(name)
        if (path != null) {
            Image(
                painterResource("icons/$path"),
                name,
                Modifier.fillMaxSize().padding(if (compact) 2.dp else 4.dp)
            )
        } else {
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = labelSize,
                lineHeight = labelSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
