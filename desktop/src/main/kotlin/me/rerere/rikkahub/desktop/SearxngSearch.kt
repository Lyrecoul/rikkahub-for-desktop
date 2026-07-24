package me.rerere.rikkahub.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

private val SearxngJson = Json { ignoreUnknownKeys = true }

internal suspend fun searchSearxng(
    httpClient: OkHttpClient,
    settings: DesktopWebSearchSettings,
    query: String
): ChatMessage = withContext(Dispatchers.IO) {
    require(query.isNotBlank()) { "无法搜索空白消息" }
    val baseUrl = settings.searxngUrl.trimEnd('/').toHttpUrlOrNull()
        ?: error("SearXNG 地址无效")
    val url = baseUrl.newBuilder()
        .addPathSegment("search")
        .addQueryParameter("q", query)
        .addQueryParameter("format", "json")
        .build()
    val response = httpClient.newCall(Request.Builder().url(url).get().build()).execute()
    response.use {
        if (!it.isSuccessful) error("SearXNG 搜索失败（${it.code}）")
        val results = SearxngJson.parseToJsonElement(it.body.string())
            .jsonObject["results"]?.jsonArray.orEmpty()
            .take(settings.resultCount.coerceIn(1, 10))
            .mapNotNull { item ->
                runCatching {
                    val result = item.jsonObject
                    val title = result["title"]?.jsonPrimitive?.content.orEmpty()
                    val link = result["url"]?.jsonPrimitive?.content.orEmpty()
                    val summary = result["content"]?.jsonPrimitive?.content.orEmpty()
                    if (link.isBlank()) null else "- $title\n  $link\n  $summary"
                }.getOrNull()
            }
        ChatMessage(
            role = "system",
            content = buildString {
                append("以下是对“").append(query).append("”的网络搜索结果。将其作为参考，并在回答中标明来源：\n")
                append(results.ifEmpty { listOf("未找到搜索结果。") }.joinToString("\n"))
            }
        )
    }
}

internal suspend fun searchWeb(
    httpClient: OkHttpClient,
    settings: DesktopWebSearchSettings,
    query: String
): ChatMessage = when (settings.providerType) {
    DesktopSearchProviderType.SEARXNG -> searchSearxng(httpClient, settings, query)
    DesktopSearchProviderType.BRAVE -> searchBrave(httpClient, settings, query)
}

private suspend fun searchBrave(
    httpClient: OkHttpClient,
    settings: DesktopWebSearchSettings,
    query: String
): ChatMessage = withContext(Dispatchers.IO) {
    require(query.isNotBlank()) { "无法搜索空白消息" }
    val url = "https://api.search.brave.com/res/v1/web/search".toHttpUrlOrNull()!!.newBuilder()
        .addQueryParameter("q", query)
        .addQueryParameter("count", settings.resultCount.coerceIn(1, 10).toString())
        .build()
    val request = Request.Builder().url(url)
        .header("Accept", "application/json")
        .header("X-Subscription-Token", settings.apiKey)
        .get().build()
    httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) error("Brave 搜索失败（${response.code}）")
        val results = SearxngJson.parseToJsonElement(response.body.string()).jsonObject["web"]?.jsonObject
            ?.get("results")?.jsonArray.orEmpty().mapNotNull { item ->
                runCatching {
                    val result = item.jsonObject
                    val title = result["title"]?.jsonPrimitive?.content.orEmpty()
                    val link = result["url"]?.jsonPrimitive?.content.orEmpty()
                    val summary = result["description"]?.jsonPrimitive?.content.orEmpty()
                    if (link.isBlank()) null else "- $title\n  $link\n  $summary"
                }.getOrNull()
            }
        ChatMessage(
            role = "system",
            content = "以下是对“$query”的网络搜索结果。将其作为参考，并在回答中标明来源：\n" +
                results.ifEmpty { listOf("未找到搜索结果。") }.joinToString("\n")
        )
    }
}
