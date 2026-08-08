package me.rerere.rikkahub.desktop

/**
 * 模型发现（从配置的 provider 实时获取模型列表）的状态机与探活逻辑。
 * 状态由 UI 持有；探活与写回是纯函数，经 fake fetch 可测。
 */
internal sealed interface DesktopModelDiscoveryState {
    data object Idle : DesktopModelDiscoveryState
    data object Testing : DesktopModelDiscoveryState
    data class Success(val listing: DesktopModelListing) : DesktopModelDiscoveryState
    data class Failure(val message: String) : DesktopModelDiscoveryState
}

/** 执行一次模型发现探活。fetch 经 lambda 注入以便测试。 */
internal suspend fun probeProviderModels(
    fetch: suspend () -> DesktopModelListing,
): DesktopModelDiscoveryState = runCatching { fetch() }.fold(
    onSuccess = { DesktopModelDiscoveryState.Success(it) },
    onFailure = { DesktopModelDiscoveryState.Failure(it.message ?: "Connection failed") }
)

/** 把发现结果写回 provider 配置：保留 live 模型 ID 与官方 display name。 */
internal fun DesktopProviderProfile.withDiscoveredModels(listing: DesktopModelListing): DesktopProviderProfile =
    copy(discoveredModels = listing.ids, discoveredModelNames = listing.displayNames)
