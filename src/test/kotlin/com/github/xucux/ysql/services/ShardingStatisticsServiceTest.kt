package com.github.xucux.ysql.services

import com.github.xucux.ysql.models.ShardingConfig
import com.github.xucux.ysql.models.SuffixType
import com.github.xucux.ysql.ui.ShardingStatisticsConfigDialog
import org.junit.Test
import org.junit.Assert.*

/**
 * 分表统计服务测试
 */
class ShardingStatisticsServiceTest {
    
    private val service = ShardingStatisticsService()
    
    @Test
    fun testGenerateShardingStatistics() {
        // 准备测试数据
        val config = ShardingConfig(
            tableNames = listOf("t_main"),
            suffixType = SuffixType.SEQUENCE,
            suffixFormat = "_",
            shardCount = 3,
            startYear = 2020,
            startMonth = 1,
            originalSql = "SELECT qty, amount FROM t_main WHERE order_time >= '2024-01-01 00:00:00' AND order_time <= '2024-12-31 23:59:59' AND order_type = 4 AND delete_status = 0"
        )
        
        val fieldStatisticsConfig = mapOf(
            "qty" to ShardingStatisticsConfigDialog.StatisticsType.SUM,
            "amount" to ShardingStatisticsConfigDialog.StatisticsType.SUM
        )
        
        // 执行测试
        val result = service.generateShardingStatistics(config, fieldStatisticsConfig)
        
        // 验证结果
        assertTrue("生成应该成功", result.success)
        assertNotNull("统计SQL不应为空", result.statisticsSql)
        assertTrue("统计SQL应包含SUM", result.statisticsSql.contains("SUM"))
        assertTrue("统计SQL应包含UNION ALL", result.statisticsSql.contains("UNION ALL"))
        assertTrue("统计SQL应包含t_main_0", result.statisticsSql.contains("t_main_0"))
        assertTrue("统计SQL应包含t_main_1", result.statisticsSql.contains("t_main_1"))
        assertTrue("统计SQL应包含t_main_2", result.statisticsSql.contains("t_main_2"))
    }
    
    @Test
    fun testGenerateShardingStatisticsWithDifferentTypes() {
        // 准备测试数据
        val config = ShardingConfig(
            tableNames = listOf("t_main"),
            suffixType = SuffixType.SEQUENCE,
            suffixFormat = "_",
            shardCount = 2,
            startYear = 2020,
            startMonth = 1,
            originalSql = "SELECT qty, amount FROM t_main WHERE order_type = 4"
        )
        
        val fieldStatisticsConfig = mapOf(
            "qty" to ShardingStatisticsConfigDialog.StatisticsType.COUNT,
            "amount" to ShardingStatisticsConfigDialog.StatisticsType.SUM
        )
        
        // 执行测试
        val result = service.generateShardingStatistics(config, fieldStatisticsConfig)
        
        // 验证结果
        assertTrue("生成应该成功", result.success)
        assertNotNull("统计SQL不应为空", result.statisticsSql)
        assertTrue("统计SQL应包含COUNT", result.statisticsSql.contains("COUNT"))
        assertTrue("统计SQL应包含SUM", result.statisticsSql.contains("SUM"))
    }
    
    @Test
    fun testGenerateShardingStatisticsWithAliases() {
        // 测试你提到的具体问题：COUNT(1) AS qty, SUM(part_cost_amount)
        val config = ShardingConfig(
            tableNames = listOf("kc_repair_out_main"),
            suffixType = SuffixType.SEQUENCE,
            suffixFormat = "_",
            shardCount = 5,
            startYear = 2024,
            startMonth = 1,
            originalSql = "SELECT COUNT(1) AS qty,SUM(part_cost_amount) FROM kc_repair_out_main WHERE outin_time >= '2024-01-01 00:00:00' AND outin_time <= '2024-12-31 23:59:59' AND order_type = 4 AND delete_status = 0"
        )
        
        val fieldStatisticsConfig = mapOf(
            "COUNT(1)" to ShardingStatisticsConfigDialog.StatisticsType.SUM,
            "SUM(part_cost_amount)" to ShardingStatisticsConfigDialog.StatisticsType.SUM
        )
        
        // 执行测试
        val result = service.generateShardingStatistics(config, fieldStatisticsConfig)
        
        // 验证结果
        assertTrue("生成应该成功", result.success)
        assertNotNull("统计SQL不应为空", result.statisticsSql)
        
        // 验证修复后的SQL格式
        val sql = result.statisticsSql
        println("生成的SQL: $sql")
        
        // 应该使用别名而不是原始字段表达式
        assertTrue("应该使用别名qty", sql.contains("SUM(unionTable.qty)"))
        assertTrue("应该使用别名sum_result", sql.contains("SUM(unionTable.sum_result)"))
        
        // 不应该包含错误的语法
        assertFalse("不应该包含错误的语法", sql.contains("SUM(unionTable.COUNT(1) AS qty)"))
        assertFalse("不应该包含错误的语法", sql.contains("SUM(unionTable.SUM(part_cost_amount))"))
        
        // 验证UNION ALL子查询中的别名
        assertTrue("子查询应该包含别名", sql.contains("COUNT(1) AS qty"))
        assertTrue("子查询应该包含别名", sql.contains("SUM(part_cost_amount) AS sum_result"))
    }
    
    @Test
    fun testGenerateShardingStatisticsWithDuplicateFields() {
        // 测试重复字段的问题：COUNT(1) AS qty, SUM(part_cost_amount), SUM(part_cost_amount)
        val config = ShardingConfig(
            tableNames = listOf("kc_repair_out_main"),
            suffixType = SuffixType.SEQUENCE,
            suffixFormat = "_",
            shardCount = 3,
            startYear = 2024,
            startMonth = 1,
            originalSql = "SELECT COUNT(1) AS qty,SUM(part_cost_amount),SUM(part_cost_amount) FROM kc_repair_out_main WHERE outin_time >= '2024-01-01 00:00:00' AND outin_time <= '2024-12-31 23:59:59' AND order_type = 4 AND delete_status = 0"
        )
        
        val fieldStatisticsConfig = mapOf(
            "COUNT(1)" to ShardingStatisticsConfigDialog.StatisticsType.SUM,
            "SUM(part_cost_amount)" to ShardingStatisticsConfigDialog.StatisticsType.SUM
        )
        
        // 执行测试
        val result = service.generateShardingStatistics(config, fieldStatisticsConfig)
        
        // 验证结果
        assertTrue("生成应该成功", result.success)
        assertNotNull("统计SQL不应为空", result.statisticsSql)
        
        // 验证修复后的SQL格式
        val sql = result.statisticsSql
        println("生成的SQL: $sql")
        
        // 应该使用不同的别名
        assertTrue("应该使用别名qty", sql.contains("SUM(unionTable.qty)"))
        assertTrue("应该使用别名sum_result", sql.contains("SUM(unionTable.sum_result)"))
        assertTrue("应该使用别名sum_result_1", sql.contains("SUM(unionTable.sum_result_1)"))
        
        // 不应该包含重复的别名
        assertFalse("不应该包含重复的别名", sql.contains("SUM(unionTable.sum_result), SUM(unionTable.sum_result)"))
        
        // 验证UNION ALL子查询中的唯一别名
        assertTrue("子查询应该包含别名", sql.contains("COUNT(1) AS qty"))
        assertTrue("子查询应该包含别名", sql.contains("SUM(part_cost_amount) AS sum_result"))
        assertTrue("子查询应该包含别名", sql.contains("SUM(part_cost_amount) AS sum_result_1"))
    }
}
