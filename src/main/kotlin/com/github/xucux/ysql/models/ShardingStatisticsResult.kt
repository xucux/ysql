package com.github.xucux.ysql.models

/**
 * 分表统计结果模型
 * 用于存储分表统计SQL生成的结果
 */
data class ShardingStatisticsResult(
    /**
     * 是否成功
     */
    val success: Boolean = false,
    
    /**
     * 错误信息
     */
    val errorMessage: String = "",
    
    /**
     * 生成的统计SQL
     */
    val statisticsSql: String = "",
    
    /**
     * 分表数量
     */
    val shardCount: Int = 0,
    
    /**
     * 表名列表
     */
    val tableNames: List<String> = emptyList()
) {
    
    /**
     * 获取格式化的结果
     * @return 格式化的结果字符串
     */
    fun getFormattedResult(): String {
        if (!success) {
            return "生成分表统计SQL失败：$errorMessage"
        }
        
        val sb = StringBuilder()
        sb.append("=== 分表统计SQL生成结果 ===\n")
        sb.append("分表数量: $shardCount\n")
        sb.append("涉及表名: ${tableNames.joinToString(", ")}\n")
        sb.append("生成时间: ${java.time.LocalDateTime.now()}\n")
        sb.append("\n=== 统计SQL ===\n")
        sb.append(statisticsSql)
        
        return sb.toString()
    }
}
