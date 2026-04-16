package com.github.xucux.ysql.models

import com.github.xucux.ysql.utils.I18nUtil

/**
 * 分表结果模型
 * 用于存储分表SQL生成的结果信息
 */
data class ShardingResult(
    /**
     * 生成的分表SQL列表
     */
    val shardingSqls: List<String> = emptyList(),
    
    /**
     * 分表数量
     */
    val shardCount: Int = 0,
    
    /**
     * 涉及的表名列表
     */
    val tableNames: List<String> = emptyList(),
    
    /**
     * 生成时间
     */
    val generateTime: Long = System.currentTimeMillis(),
    
    /**
     * 是否生成成功
     */
    val success: Boolean = false,
    
    /**
     * 错误信息（如果有）
     */
    val errorMessage: String? = null
) {
    /**
     * 获取所有分表SQL的合并字符串
     */
    fun getCombinedSqls(): String {
        return shardingSqls.joinToString("\n\n")
    }
    
    /**
     * 获取统计信息
     */
    fun getStatistics(): String {
        return buildString {
            appendLine(msg("sharding.result.statistics.header"))
            appendLine(msg("sharding.result.statistics.shard.count", shardCount))
            appendLine(msg("sharding.result.statistics.table.names", tableNames.joinToString(", ")))
            appendLine(msg("sharding.result.statistics.generate.time", formatTime(generateTime)))
            appendLine(msg("sharding.result.statistics.status", msg(if (success) "common.status.success" else "common.status.failed")))
            if (!success && errorMessage != null) {
                appendLine(msg("sharding.result.statistics.error.message", errorMessage))
            }
        }
    }
    
    /**
     * 获取格式化的结果
     */
    fun getFormattedResult(): String {
        return buildString {
            appendLine(getStatistics())
            appendLine()
            appendLine(msg("sharding.result.formatted.generated.sql"))
            appendLine("=".repeat(50))
            appendLine(getCombinedSqls())
        }
    }

    /**
     * 格式化 `time`。
     */
    private fun formatTime(time: Long): String {
        return java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(time),
            java.time.ZoneId.systemDefault()
        ).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    /**
     * 返回国际化消息文本。
     */
    private fun msg(key: String, vararg args: Any): String = I18nUtil.getMessage(key, *args)
}
