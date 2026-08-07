package me.rerere.rikkahub.desktop

/** Converts provider model identifiers into names that are easier to scan in the UI. */
internal fun displayModelName(modelId: String): String {
    val identifier = modelId.trim().removePrefix("models/")
    if (identifier.isBlank()) return modelId

    val name = identifier
        .replace('/', ' ')
        .replace(Regex("[-_]+"), " ")
        .replace(Regex("(?<=\\d) (?=\\d(?:\\D|$))"), ".")
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .joinToString(" ", transform = ::displayModelToken)

    return name.replace(Regex("\\b(GPT|GLM) (?=\\d)"), "$1-")
}

private fun displayModelToken(token: String): String = when {
    token.matches(Regex("\\d+b", RegexOption.IGNORE_CASE)) -> token.dropLast(1) + "B"
    else -> when (token.lowercase()) {
        "ai" -> "AI"
        "api" -> "API"
        "chatgpt" -> "ChatGPT"
        "codex" -> "Codex"
        "deepseek" -> "DeepSeek"
        "gemini" -> "Gemini"
        "glm" -> "GLM"
        "gpt" -> "GPT"
        "grok" -> "Grok"
        "internlm" -> "InternLM"
        "llama" -> "Llama"
        "minimax" -> "MiniMax"
        "mistral" -> "Mistral"
        "openai" -> "OpenAI"
        "oss" -> "OSS"
        "qwen" -> "Qwen"
        "vl" -> "VL"
        else -> token.replaceFirstChar { character -> character.uppercase() }
    }
}
