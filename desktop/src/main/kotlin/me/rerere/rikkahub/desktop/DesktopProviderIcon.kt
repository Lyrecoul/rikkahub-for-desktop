package me.rerere.rikkahub.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private fun providerIconPath(name: String): String? {
    val value = name.lowercase()
    return when {
        "openai" in value -> "openai.svg"
        "deepseek" in value -> "deepseek-color.svg"
        "硅基" in value || "siliconflow" in value -> "siliconflow.svg"
        "openrouter" in value -> "openrouter.svg"
        "月之暗面" in value || "moonshot" in value || "kimi" in value -> "kimi-color.svg"
        "阿里" in value || "qwen" in value || "百炼" in value -> "qwen-color.svg"
        "火山" in value || "字节" in value || "doubao" in value -> "doubao-color.svg"
        "智谱" in value || "zhipu" in value -> "zhipu-color.svg"
        "阶跃" in value || "stepfun" in value -> "stepfun-color.svg"
        "gemini" in value || "google" in value -> "gemini-color.svg"
        "claude" in value || "anthropic" in value -> "claude-color.svg"
        "brave" in value -> "brave.svg"
        "tavily" in value -> "tavily.png"
        "exa" in value -> "exa.png"
        "rikkahub" in value -> "rikkahub.svg"
        else -> null
    }
}

@Composable
internal fun DesktopProviderIcon(name: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(24.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        val path = providerIconPath(name)
        if (path != null) {
            Image(painterResource("icons/$path"), name, Modifier.padding(4.dp))
        } else {
            Text(
                name.trim().firstOrNull()?.uppercase().orEmpty(),
                Modifier.padding(top = 3.dp),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
