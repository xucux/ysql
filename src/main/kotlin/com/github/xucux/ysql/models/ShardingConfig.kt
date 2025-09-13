package com.github.xucux.ysql.models

/**
 * 分表配置模型
 * 用于存储分表生成的相关配置信息
 */
data class ShardingConfig(
    /**
     * 表名列表
     */
    val tableNames: List<String> = emptyList(),
    
    /**
     * 分表后缀类型
     */
    val suffixType: SuffixType = SuffixType.SEQUENCE,
    
    /**
     * 后缀格式（如 "_", "-", "" 等）
     */
    val suffixFormat: String = "_",
    
    /**
     * 分表数量
     */
    val shardCount: Int = 4,
    
    /**
     * 起始年份（用于年份类型后缀）
     */
    val startYear: Int = 2020,
    
    /**
     * 起始月份（用于年月类型后缀）
     */
    val startMonth: Int = 1,
    
    /**
     * 原始SQL语句
     */
    val originalSql: String = ""
)

/**
 * 分表后缀类型枚举
 */
enum class SuffixType(val displayName: String, val description: String) {
    SEQUENCE("数字序列", "生成 _0, _1, _2... 格式的后缀"),
    YEAR("年份", "生成 _2020, _2021... 格式的后缀"),
    YEAR_MONTH("年月", "生成 _202001, _202002... 格式的后缀"),
    CUSTOM("自定义", "使用自定义格式生成后缀")
}
