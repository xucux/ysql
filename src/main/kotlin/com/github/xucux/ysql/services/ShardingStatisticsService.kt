package com.github.xucux.ysql.services

import com.github.xucux.ysql.models.ShardingConfig
import com.github.xucux.ysql.models.ShardingStatisticsResult
import com.github.xucux.ysql.ui.ShardingStatisticsConfigDialog
import com.github.xucux.ysql.utils.SqlParser
import com.github.xucux.ysql.utils.SuffixGenerator
import com.github.xucux.ysql.utils.EncodingUtils
import com.intellij.openapi.components.Service

/**
 * 分表统计服务
 * 负责生成分表统计SQL，将多个分表的数据进行UNION ALL并统计
 */
@Service
class ShardingStatisticsService {
    
    /**
     * 生成分表统计SQL
     * @param config 分表配置
     * @param fieldStatisticsConfig 字段统计配置
     * @return 分表统计结果
     */
    fun generateShardingStatistics(
        config: ShardingConfig, 
        fieldStatisticsConfig: Map<String, ShardingStatisticsConfigDialog.StatisticsType>
    ): ShardingStatisticsResult {
        try {
            // 验证配置
            val validationResult = validateConfig(config)
            if (!validationResult.isValid) {
                return ShardingStatisticsResult(
                    success = false,
                    errorMessage = validationResult.message
                )
            }
            
            // 验证SQL格式
            val sqlValidation = SqlParser.validateSql(config.originalSql)
            if (!sqlValidation.isValid) {
                return ShardingStatisticsResult(
                    success = false,
                    errorMessage = sqlValidation.message
                )
            }
            
            // 生成分表统计SQL
            val statisticsSql = generateStatisticsSql(config, fieldStatisticsConfig)
            
            return ShardingStatisticsResult(
                statisticsSql = statisticsSql,
                shardCount = config.shardCount,
                tableNames = config.tableNames,
                success = true
            )
            
        } catch (e: Exception) {
            return ShardingStatisticsResult(
                success = false,
                errorMessage = EncodingUtils.formatChineseText("生成分表统计SQL时发生错误：${e.message}")
            )
        }
    }
    
    /**
     * 生成分表统计SQL
     * @param config 分表配置
     * @param fieldStatisticsConfig 字段统计配置
     * @return 分表统计SQL
     */
    private fun generateStatisticsSql(
        config: ShardingConfig, 
        fieldStatisticsConfig: Map<String, ShardingStatisticsConfigDialog.StatisticsType>
    ): String {
        // 生成后缀列表
        val suffixes = SuffixGenerator.generateSuffixList(
            count = config.shardCount,
            suffixType = config.suffixType,
            format = config.suffixFormat,
            startYear = config.startYear,
            startMonth = config.startMonth
        )
        
        // 解析原始SQL，提取SELECT字段
        val selectFields = extractSelectFields(config.originalSql)
        if (selectFields.isEmpty()) {
            throw IllegalArgumentException("无法从SQL中提取SELECT字段")
        }
        
        // 生成UNION ALL子查询
        val unionQueries = mutableListOf<String>()
        suffixes.forEach { suffix ->
            var shardingSql = config.originalSql
            
            // 替换所有表名
            config.tableNames.forEach { tableName ->
                val newTableName = "$tableName$suffix"
                shardingSql = SqlParser.replaceTableName(shardingSql, tableName, newTableName)
            }
            
            // 为没有别名的字段添加别名
            shardingSql = addAliasesToSelectFields(shardingSql, selectFields)
            
            unionQueries.add(shardingSql)
        }
        
        // 构建统计SQL
        val statisticsSql = buildStatisticsSql(selectFields, unionQueries, fieldStatisticsConfig)
        
        return statisticsSql
    }
    
    /**
     * 从SQL中提取SELECT字段
     * @param sql 原始SQL
     * @return SELECT字段列表，包含字段名和别名信息
     */
    private fun extractSelectFields(sql: String): List<SelectField> {
        val trimmedSql = sql.trim()
        val selectIndex = trimmedSql.indexOf("SELECT", ignoreCase = true)
        val fromIndex = trimmedSql.indexOf("FROM", ignoreCase = true)
        
        if (selectIndex == -1 || fromIndex == -1 || selectIndex >= fromIndex) {
            return emptyList()
        }
        
        val selectClause = trimmedSql.substring(selectIndex + 6, fromIndex).trim()
        if (selectClause == "*") {
            // 如果是SELECT *，需要特殊处理
            return listOf(SelectField("*", "*"))
        }
        
        // 分割字段，处理逗号分隔
        val fieldClauses = selectClause.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        // 用于跟踪已使用的别名，确保唯一性
        val usedAliases = mutableSetOf<String>()
        
        return fieldClauses.map { fieldClause ->
            parseSelectField(fieldClause, usedAliases)
        }
    }
    
    /**
     * 解析单个SELECT字段，提取字段名和别名
     * @param fieldClause 字段子句，如 "COUNT(1) AS qty" 或 "SUM(part_cost_amount)"
     * @param usedAliases 已使用的别名集合，用于确保别名唯一性
     * @return SelectField对象
     */
    private fun parseSelectField(fieldClause: String, usedAliases: MutableSet<String>): SelectField {
        // 检查是否有别名
        val asIndex = fieldClause.indexOf(" AS ", ignoreCase = true)
        if (asIndex != -1) {
            val fieldExpression = fieldClause.substring(0, asIndex).trim()
            val alias = fieldClause.substring(asIndex + 4).trim()
            // 如果别名已存在，生成唯一别名
            val uniqueAlias = generateUniqueAlias(alias, usedAliases)
            usedAliases.add(uniqueAlias)
            return SelectField(fieldExpression, uniqueAlias)
        } else {
            // 没有别名，生成一个唯一别名
            val alias = generateFieldAlias(fieldClause)
            val uniqueAlias = generateUniqueAlias(alias, usedAliases)
            usedAliases.add(uniqueAlias)
            return SelectField(fieldClause, uniqueAlias)
        }
    }
    
    /**
     * 为字段生成别名
     * @param fieldExpression 字段表达式
     * @return 生成的别名
     */
    private fun generateFieldAlias(fieldExpression: String): String {
        // 如果是函数调用，提取函数名和参数
        val trimmed = fieldExpression.trim()
        
        // 处理常见的聚合函数
        when {
            trimmed.startsWith("COUNT(", ignoreCase = true) -> return "count_result"
            trimmed.startsWith("SUM(", ignoreCase = true) -> return "sum_result"
            trimmed.startsWith("AVG(", ignoreCase = true) -> return "avg_result"
            trimmed.startsWith("MAX(", ignoreCase = true) -> return "max_result"
            trimmed.startsWith("MIN(", ignoreCase = true) -> return "min_result"
            else -> {
                // 对于普通字段，使用字段名作为别名
                // 如果包含括号，说明是函数调用，生成通用别名
                if (trimmed.contains("(")) {
                    return "field_${System.currentTimeMillis() % 10000}"
                } else {
                    return trimmed
                }
            }
        }
    }
    
    /**
     * 生成唯一的别名
     * @param baseAlias 基础别名
     * @param usedAliases 已使用的别名集合
     * @return 唯一的别名
     */
    private fun generateUniqueAlias(baseAlias: String, usedAliases: Set<String>): String {
        if (!usedAliases.contains(baseAlias)) {
            return baseAlias
        }
        
        // 如果基础别名已存在，添加数字后缀
        var counter = 1
        var uniqueAlias = "${baseAlias}_${counter}"
        
        while (usedAliases.contains(uniqueAlias)) {
            counter++
            uniqueAlias = "${baseAlias}_${counter}"
        }
        
        return uniqueAlias
    }
    
    /**
     * 为SQL中的SELECT字段添加别名
     * @param sql 原始SQL
     * @param selectFields 解析出的字段信息
     * @return 添加别名后的SQL
     */
    private fun addAliasesToSelectFields(sql: String, selectFields: List<SelectField>): String {
        val trimmedSql = sql.trim()
        val selectIndex = trimmedSql.indexOf("SELECT", ignoreCase = true)
        val fromIndex = trimmedSql.indexOf("FROM", ignoreCase = true)
        
        if (selectIndex == -1 || fromIndex == -1 || selectIndex >= fromIndex) {
            return sql
        }
        
        val beforeSelect = trimmedSql.substring(0, selectIndex + 6)
        val afterFrom = trimmedSql.substring(fromIndex)
        
        // 构建新的SELECT子句，为没有别名的字段添加别名
        val newSelectClause = selectFields.joinToString(", ") { selectField ->
            if (selectField.expression == selectField.alias) {
                // 没有别名，添加别名
                "${selectField.expression} AS ${selectField.alias}"
            } else {
                // 已经有别名，保持原样
                "${selectField.expression} AS ${selectField.alias}"
            }
        }
        
        return "$beforeSelect $newSelectClause $afterFrom"
    }
    
    /**
     * SELECT字段信息
     * @param expression 字段表达式，如 "COUNT(1)" 或 "SUM(part_cost_amount)"
     * @param alias 字段别名，用于在unionTable中引用
     */
    private data class SelectField(
        val expression: String,
        val alias: String
    )
    
    /**
     * 构建统计SQL
     * @param selectFields SELECT字段列表
     * @param unionQueries UNION ALL子查询列表
     * @param fieldStatisticsConfig 字段统计配置
     * @return 统计SQL
     */
    private fun buildStatisticsSql(
        selectFields: List<SelectField>, 
        unionQueries: List<String>,
        fieldStatisticsConfig: Map<String, ShardingStatisticsConfigDialog.StatisticsType>
    ): String {
        val sb = StringBuilder()
        
        // 构建SELECT子句 - 根据字段统计配置对每个字段进行统计
        val statisticsFields = selectFields.map { selectField ->
            if (selectField.expression == "*") {
                // 对于SELECT *的情况，需要特殊处理，这里暂时不支持
                throw IllegalArgumentException("SELECT * 暂不支持，请明确指定字段名")
            } else {
                // 使用字段表达式作为配置键，如果没有找到配置则使用别名
                val configKey = selectField.expression
                val statisticsType = fieldStatisticsConfig[configKey] ?: ShardingStatisticsConfigDialog.StatisticsType.SUM
                
                // 使用别名来引用unionTable中的字段
                when (statisticsType) {
                    ShardingStatisticsConfigDialog.StatisticsType.SUM -> "SUM(unionTable.${selectField.alias})"
                    ShardingStatisticsConfigDialog.StatisticsType.COUNT -> "COUNT(unionTable.${selectField.alias})"
                    ShardingStatisticsConfigDialog.StatisticsType.AVG -> "AVG(unionTable.${selectField.alias})"
                    ShardingStatisticsConfigDialog.StatisticsType.MAX -> "MAX(unionTable.${selectField.alias})"
                    ShardingStatisticsConfigDialog.StatisticsType.MIN -> "MIN(unionTable.${selectField.alias})"
                }
            }
        }
        
        sb.append("SELECT ${statisticsFields.joinToString(", ")} FROM (\n")
        
        // 添加UNION ALL子查询
        unionQueries.forEachIndexed { index, query ->
            sb.append(query)
            if (index < unionQueries.size - 1) {
                sb.append(" UNION ALL\n")
            } else {
                sb.append("\n")
            }
        }
        
        sb.append(") unionTable")
        
        return sb.toString()
    }
    
    /**
     * 验证分表配置
     * @param config 分表配置
     * @return 验证结果
     */
    private fun validateConfig(config: ShardingConfig): ValidationResult {
        if (config.tableNames.isEmpty()) {
            return ValidationResult(false, "表名列表不能为空")
        }
        
        if (config.shardCount <= 0) {
            return ValidationResult(false, "分表数量必须大于0")
        }
        
        if (config.originalSql.isBlank()) {
            return ValidationResult(false, "原始SQL语句不能为空")
        }
        
        // 检查SQL是否包含SELECT语句
        if (!config.originalSql.contains("SELECT", ignoreCase = true)) {
            return ValidationResult(false, "SQL语句必须包含SELECT子句")
        }
        
        return ValidationResult(true, "配置验证通过")
    }
    
    private data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
