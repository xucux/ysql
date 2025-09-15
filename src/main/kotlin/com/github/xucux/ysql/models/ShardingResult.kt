package com.github.xucux.ysql.models

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
            appendLine("- 分表统计信息：")
            appendLine("- 分表数量：$shardCount")
            appendLine("- 涉及表名：${tableNames.joinToString(", ")}")
            appendLine("- 生成时间：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(generateTime))}")
            appendLine("- 状态：${if (success) "成功" else "失败"}")
            if (!success && errorMessage != null) {
                appendLine("- 错误信息：$errorMessage")
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
            appendLine("- 生成的分表SQL：")
            appendLine("=".repeat(50))
            appendLine(getCombinedSqls())
        }
    }
}
