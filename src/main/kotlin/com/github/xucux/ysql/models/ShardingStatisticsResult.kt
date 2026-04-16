package com.github.xucux.ysql.models

import com.github.xucux.ysql.utils.I18nUtil

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
            return I18nUtil.getMessage("sharding.statistics.result.failed", errorMessage)
        }
        
        val sb = StringBuilder()
        sb.append(I18nUtil.getMessage("sharding.statistics.result.header")).append('\n')
        sb.append(I18nUtil.getMessage("sharding.statistics.result.shard.count", shardCount)).append('\n')
        sb.append(I18nUtil.getMessage("sharding.statistics.result.table.names", tableNames.joinToString(", "))).append('\n')
        sb.append(I18nUtil.getMessage("sharding.statistics.result.generate.time", java.time.LocalDateTime.now())).append('\n')
        sb.append('\n').append(I18nUtil.getMessage("sharding.statistics.result.sql.header")).append('\n')
        sb.append(statisticsSql)
        
        return sb.toString()
    }
}
