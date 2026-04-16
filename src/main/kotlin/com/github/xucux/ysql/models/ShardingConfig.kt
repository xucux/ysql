package com.github.xucux.ysql.models

import com.github.xucux.ysql.utils.I18nUtil

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
enum class SuffixType(private val displayNameKey: String, private val descriptionKey: String) {
    SEQUENCE("suffix.type.sequence", "suffix.type.sequence.description"),
    YEAR("suffix.type.year", "suffix.type.year.description"),
    YEAR_MONTH("suffix.type.year_month", "suffix.type.year_month.description"),
    CUSTOM("suffix.type.custom", "suffix.type.custom.description");

    val displayName: String
        get() = I18nUtil.getMessage(displayNameKey)

    val description: String
        get() = I18nUtil.getMessage(descriptionKey)

    /**
     * 返回当前对象的显示文本。
     */
    override fun toString(): String = displayName
}
