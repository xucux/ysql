package com.github.xucux.ysql.services

import com.github.xucux.ysql.models.ShardingConfig
import com.github.xucux.ysql.models.ShardingResult
import com.github.xucux.ysql.utils.SqlParser
import com.github.xucux.ysql.utils.SuffixGenerator
import com.github.xucux.ysql.utils.EncodingUtils
import com.intellij.openapi.components.Service

/**
 * SQL分表服务
 * 核心业务逻辑服务，负责分表SQL的生成
 */
@Service
class SqlShardingService {
    
    /**
     * 生成分表SQL
     * @param config 分表配置
     * @return 分表结果
     */
    fun generateShardingSql(config: ShardingConfig): ShardingResult {
        try {
            // 验证配置
            val validationResult = validateConfig(config)
            if (!validationResult.isValid) {
                return ShardingResult(
                    success = false,
                    errorMessage = validationResult.message
                )
            }
            
            // 验证SQL格式
            val sqlValidation = SqlParser.validateSql(config.originalSql)
            if (!sqlValidation.isValid) {
                return ShardingResult(
                    success = false,
                    errorMessage = sqlValidation.message
                )
            }
            
            // 生成分表SQL
            val shardingSqls = generateShardingSqls(config)
            
            return ShardingResult(
                shardingSqls = shardingSqls,
                shardCount = config.shardCount,
                tableNames = config.tableNames,
                success = true
            )
            
        } catch (e: Exception) {
            return ShardingResult(
                success = false,
                errorMessage = EncodingUtils.formatChineseText("生成分表SQL时发生错误：${e.message}")
            )
        }
    }
    
    /**
     * 生成分表SQL列表
     * @param config 分表配置
     * @return 分表SQL列表
     */
    private fun generateShardingSqls(config: ShardingConfig): List<String> {
        val shardingSqls = mutableListOf<String>()
        
        // 生成后缀列表
        val suffixes = SuffixGenerator.generateSuffixList(
            count = config.shardCount,
            suffixType = config.suffixType,
            format = config.suffixFormat,
            startYear = config.startYear,
            startMonth = config.startMonth
        )
        
        // 为每个后缀生成对应的SQL
        suffixes.forEachIndexed { index, suffix ->
            var shardingSql = config.originalSql
            
            // 替换所有表名
            config.tableNames.forEach { tableName ->
                val newTableName = "$tableName$suffix"
                shardingSql = SqlParser.replaceTableName(shardingSql, tableName, newTableName)
            }
            
            shardingSqls.add(shardingSql)
        }
        
        return shardingSqls
    }
    
    /**
     * 验证分表配置
     * @param config 分表配置
     * @return 验证结果
     */
    private fun validateConfig(config: ShardingConfig): ValidationResult {
        // 检查表名列表
        if (config.tableNames.isEmpty()) {
            return ValidationResult(false, "表名列表不能为空")
        }
        
        // 检查分表数量
        if (config.shardCount <= 0) {
            return ValidationResult(false, "分表数量必须大于0")
        }
        
        if (config.shardCount > 1000) {
            return ValidationResult(false, "分表数量不能超过1000")
        }
        
        // 检查原始SQL
        if (config.originalSql.isBlank()) {
            return ValidationResult(false, "原始SQL语句不能为空")
        }
        
        // 检查后缀格式
        val formatValidation = SuffixGenerator.validateFormat(config.suffixFormat, config.suffixType)
        if (!formatValidation.isValid) {
            return ValidationResult(false, formatValidation.message)
        }
        
        // 检查年份和月份
        if (config.suffixType == com.github.xucux.ysql.models.SuffixType.YEAR || 
            config.suffixType == com.github.xucux.ysql.models.SuffixType.YEAR_MONTH) {
            if (config.startYear < 1900 || config.startYear > 2100) {
                return ValidationResult(false, "起始年份必须在1900-2100之间")
            }
        }
        
        if (config.suffixType == com.github.xucux.ysql.models.SuffixType.YEAR_MONTH) {
            if (config.startMonth < 1 || config.startMonth > 12) {
                return ValidationResult(false, "起始月份必须在1-12之间")
            }
        }
        
        return ValidationResult(true, "配置验证通过")
    }
    
    /**
     * 获取分表预览信息
     * @param config 分表配置
     * @return 预览信息
     */
    fun getShardingPreview(config: ShardingConfig): String {
        return buildString {
            appendLine(EncodingUtils.formatChineseText("分表配置预览："))
            appendLine(EncodingUtils.formatChineseText("• 表名：${config.tableNames.joinToString(", ")}"))
            appendLine(EncodingUtils.formatChineseText("• 分表数量：${config.shardCount}"))
            appendLine(EncodingUtils.formatChineseText("• 后缀类型：${config.suffixType.displayName}"))
            appendLine(EncodingUtils.formatChineseText("• 后缀格式：${config.suffixFormat}"))
            
            if (config.suffixType == com.github.xucux.ysql.models.SuffixType.YEAR || 
                config.suffixType == com.github.xucux.ysql.models.SuffixType.YEAR_MONTH) {
                appendLine(EncodingUtils.formatChineseText("• 起始年份：${config.startYear}"))
            }
            
            if (config.suffixType == com.github.xucux.ysql.models.SuffixType.YEAR_MONTH) {
                appendLine(EncodingUtils.formatChineseText("• 起始月份：${config.startMonth}"))
            }
            
            appendLine()
            appendLine(EncodingUtils.formatChineseText("生成的分表名称示例："))
            
            val suffixes = SuffixGenerator.generateSuffixList(
                count = minOf(config.shardCount, 5), // 只显示前5个
                suffixType = config.suffixType,
                format = config.suffixFormat,
                startYear = config.startYear,
                startMonth = config.startMonth
            )
            
            config.tableNames.forEach { tableName ->
                suffixes.forEach { suffix ->
                    appendLine(EncodingUtils.formatChineseText("  • $tableName$suffix"))
                }
            }
            
            if (config.shardCount > 5) {
                appendLine(EncodingUtils.formatChineseText("  • ... (还有 ${config.shardCount - 5} 个)"))
            }
        }
    }
    
    /**
     * 验证结果数据类
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
