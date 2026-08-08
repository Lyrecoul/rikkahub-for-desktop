package me.rerere.rikkahub.desktop

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.Job
import kotlin.coroutines.coroutineContext

/**
 * 对话级生成占位注册表：每个对话同一时间只允许一个生成（主执行或后台模型任务）。
 * 占位、身份保护清理与取消集中在此，Compose 可直接观察 [runningIds]。
 */
internal class DesktopGenerationRegistry {
    private val jobs = mutableStateMapOf<String, Job>()

    /**
     * 原子占位。返回 false 表示该对话已有生成在运行，调用方应放弃启动。
     * [job] 必须是随后真正启动的协程自身，[finish] 依赖身份匹配。
     */
    fun begin(id: String, job: Job): Boolean {
        if (jobs.containsKey(id)) return false
        jobs[id] = job
        return true
    }

    /**
     * 由运行中的协程在 finally 中调用。仅当 [id] 上注册的正是调用协程自身时才清理，
     * 避免被取消的旧协程误清新协程的占位。返回是否清理成功。
     */
    suspend fun finish(id: String): Boolean {
        val current = coroutineContext[Job] ?: return false
        if (jobs[id] !== current) return false
        jobs.remove(id)
        return true
    }

    /** 取消并移除占位（删除对话、退出应用等场景）。 */
    fun cancel(id: String) {
        jobs.remove(id)?.cancel()
    }

    /** 取消并清空全部占位。 */
    fun cancelAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    fun isRunning(id: String): Boolean = jobs.containsKey(id)

    val runningIds: Set<String> get() = jobs.keys
}
