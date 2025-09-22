package com.github.xucux.ysql.utils

import com.github.xucux.ysql.models.DynamicSqlConfig
import com.github.xucux.ysql.models.DynamicSqlResult
import com.github.xucux.ysql.models.SqlVariable
import com.github.xucux.ysql.models.VariableType
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.expression.*
import net.sf.jsqlparser.expression.operators.conditional.AndExpression
import net.sf.jsqlparser.expression.operators.conditional.OrExpression
import net.sf.jsqlparser.expression.operators.relational.*
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.schema.Column
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import java.util.regex.Pattern


/**
 * 动态SQL生成器
 * 负责解析SQL语句并生成动态SQL代码
 */
object DynamicSqlGenerator {
    
    /**
     * 数字匹配的正则表达式
     */
    private val NUMBER_PATTERN = Pattern.compile("\\b\\d+\\b")
    
    /**
     * 字符串匹配的正则表达式（单引号或双引号）
     */
    private val STRING_PATTERN = Pattern.compile("['\"]([^'\"]*)['\"]")
    
    /**
     * 表名匹配的正则表达式（用于分片后缀替换）
     */
    private val TABLE_PATTERN = Pattern.compile(
        "\\b(?:FROM|JOIN|UPDATE|INSERT\\s+INTO|DELETE\\s+FROM)\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)\\b",
        Pattern.CASE_INSENSITIVE
    )
    
    /**
     * 生成动态SQL语句
     * @param config 配置信息
     * @return 生成结果
     */
    fun generateDynamicSql(config: DynamicSqlConfig): DynamicSqlResult {
        try {
            // 验证输入
            if (config.originalSql.isBlank()) {
                return DynamicSqlResult(
                    success = false,
                    errorMessage = "原始SQL语句不能为空"
                )
            }
            
            // 验证是否为单条语句
            if (!validateSingleSqlStatement(config.originalSql)) {
                return DynamicSqlResult(
                    success = false,
                    errorMessage = "仅支持单条SQL语句，不支持多条语句"
                )
            }
            
            // 提取变量
            val extractedVariables = extractVariablesFromSql(config.originalSql, config.getAllIgnoredVariables())
            
            if (extractedVariables.isEmpty()) {
                return DynamicSqlResult(
                    success = false,
                    errorMessage = "未找到需要动态化的变量，请检查SQL语句"
                )
            }
            
            // 生成动态SQL
            val generatedSql = buildDynamicSql(config, extractedVariables)
            
            // 生成配置摘要
            val configSummary = buildConfigSummary(config, extractedVariables)
            
            return DynamicSqlResult(
                generatedDynamicSql = generatedSql,
                extractedVariables = extractedVariables,
                originalSql = config.originalSql,
                success = true,
                configSummary = configSummary,
                config = config
            )
            
        } catch (e: Exception) {
            return DynamicSqlResult(
                success = false,
                errorMessage = "生成动态SQL时发生错误：${e.message}"
            )
        }
    }
    
    /**
     * 从SQL语句中提取变量
     * @param sql SQL语句
     * @param ignoredVariables 需要忽略的变量列表
     * @return 提取的变量列表
     */
    fun extractVariablesFromSql(sql: String, ignoredVariables: List<String> = emptyList()): List<SqlVariable> {
        val variables = mutableListOf<SqlVariable>()
        val ignoredSet = ignoredVariables.map { it.lowercase() }.toSet()
        
        try {
            // 使用JSQLParser解析SQL
            val statement = CCJSqlParserUtil.parse(sql)
            
            if (statement is Select) {
                val plainSelect = statement.plainSelect
                val whereExpression = plainSelect.where
                if (whereExpression != null) {
                    extractVariablesFromExpression(whereExpression, variables, ignoredSet, 0)
                }
            }
        } catch (e: JSQLParserException) {
            // 如果JSQLParser解析失败，回退到正则表达式方法
            return extractVariablesFromSqlWithRegex(sql, ignoredVariables)
        }
        
        // 按位置排序，避免重复替换
        return variables.sortedBy { it.position }
    }
    
    /**
     * 使用正则表达式提取变量
     */
    private fun extractVariablesFromSqlWithRegex(sql: String, ignoredVariables: List<String> = emptyList()): List<SqlVariable> {
        val variables = mutableListOf<SqlVariable>()
        val ignoredSet = ignoredVariables.map { it.lowercase() }.toSet()
        
        // 1. 提取等号条件的变量 (field = value)
        val equalsPattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)\\s*=\\s*([^\\s,)]+)", Pattern.CASE_INSENSITIVE)
        val equalsMatcher = equalsPattern.matcher(sql)
        while (equalsMatcher.find()) {
            val fieldName = equalsMatcher.group(1)
            val value = equalsMatcher.group(2)
            val position = equalsMatcher.start()
            
            if (!isInsideInCondition(sql, position)) {
                val variableName = generateVariableNameFromField(fieldName, value)
                if (!ignoredSet.contains(variableName.lowercase()) && !isIgnoredField(fieldName)) {
                    val cleanValue = value.removeSurrounding("'", "'").removeSurrounding("\"", "\"")
                    val variableType = determineVariableType(cleanValue, fieldName)
                    variables.add(SqlVariable(
                        name = variableName,
                        value = cleanValue,
                        type = variableType,
                        position = position
                    ))
                }
            }
        }
        
        // 2. 提取范围条件的变量
        val rangePattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)\\s*>=\\s*([^\\s,)]+)\\s*AND\\s*\\1\\s*<=\\s*([^\\s,)]+)", Pattern.CASE_INSENSITIVE)
        val rangeMatcher = rangePattern.matcher(sql)
        while (rangeMatcher.find()) {
            val fieldName = rangeMatcher.group(1)
            val startValue = rangeMatcher.group(2)
            val endValue = rangeMatcher.group(3)
            val position = rangeMatcher.start()
            
            if (!ignoredSet.contains("${fieldName}_start".lowercase()) && !isIgnoredField(fieldName)) {
                val startVariableName = generateVariableNameFromField(fieldName, startValue, "_start")
                val endVariableName = generateVariableNameFromField(fieldName, endValue, "_end")
                
                val cleanStartValue = startValue.removeSurrounding("'", "'").removeSurrounding("\"", "\"")
                val cleanEndValue = endValue.removeSurrounding("'", "'").removeSurrounding("\"", "\"")
                
                variables.add(SqlVariable(
                    name = startVariableName,
                    value = cleanStartValue,
                    type = determineVariableType(cleanStartValue, fieldName),
                    position = position
                ))
                
                variables.add(SqlVariable(
                    name = endVariableName,
                    value = cleanEndValue,
                    type = determineVariableType(cleanEndValue, fieldName),
                    position = position + startValue.length + 10
                ))
            }
        }
        
        return variables.sortedBy { it.position }
    }
    
    /**
     * 从表达式中提取变量
     */
    private fun extractVariablesFromExpression(
        expression: Expression,
        variables: MutableList<SqlVariable>,
        ignoredSet: Set<String>,
        position: Int
    ) {
        when (expression) {
            is AndExpression -> {
                extractVariablesFromExpression(expression.leftExpression, variables, ignoredSet, position)
                extractVariablesFromExpression(expression.rightExpression, variables, ignoredSet, position)
            }
            is OrExpression -> {
                extractVariablesFromExpression(expression.leftExpression, variables, ignoredSet, position)
                extractVariablesFromExpression(expression.rightExpression, variables, ignoredSet, position)
            }
            is EqualsTo -> {
                extractVariableFromComparison(expression, variables, ignoredSet, position)
            }
            is GreaterThanEquals -> {
                extractVariableFromRangeComparison(expression, variables, ignoredSet, position, "_start")
            }
            is MinorThanEquals -> {
                extractVariableFromRangeComparison(expression, variables, ignoredSet, position, "_end")
            }
            is InExpression -> {
                // IN条件通常不需要动态化，跳过
            }
            else -> {
                // 其他表达式类型暂时不处理
            }
        }
    }
    
    /**
     * 从等号比较中提取变量
     */
    private fun extractVariableFromComparison(
        comparison: EqualsTo,
        variables: MutableList<SqlVariable>,
        ignoredSet: Set<String>,
        position: Int
    ) {
        val leftExpression = comparison.leftExpression
        val rightExpression = comparison.rightExpression
        
        if (leftExpression is Column && rightExpression is StringValue) {
            val fieldName = leftExpression.columnName
            val value = rightExpression.value
            
            if (!isIgnoredField(fieldName)) {
                val variableName = generateVariableNameFromField(fieldName, value)
                if (!ignoredSet.contains(variableName.lowercase())) {
                    val variableType = determineVariableType(value, fieldName)
                    variables.add(SqlVariable(
                        name = variableName,
                        value = value,
                        type = variableType,
                        position = position
                    ))
                }
            }
        } else if (leftExpression is Column && rightExpression is LongValue) {
            val fieldName = leftExpression.columnName
            val value = rightExpression.value.toString()
            
            if (!isIgnoredField(fieldName)) {
                val variableName = generateVariableNameFromField(fieldName, value)
                if (!ignoredSet.contains(variableName.lowercase())) {
                    variables.add(SqlVariable(
                        name = variableName,
                        value = value,
                        type = VariableType.NUMBER,
                        position = position
                    ))
                }
            }
        }
    }
    
    /**
     * 从范围比较中提取变量
     */
    private fun extractVariableFromRangeComparison(
        comparison: BinaryExpression,
        variables: MutableList<SqlVariable>,
        ignoredSet: Set<String>,
        position: Int,
        suffix: String
    ) {
        val leftExpression = comparison.leftExpression
        val rightExpression = comparison.rightExpression
        
        if (leftExpression is Column) {
            val fieldName = leftExpression.columnName
            val value = when (rightExpression) {
                is StringValue -> rightExpression.value
                is LongValue -> rightExpression.value.toString()
                is DoubleValue -> rightExpression.value.toString()
                else -> return
            }
            
            if (!isIgnoredField(fieldName)) {
                val variableName = generateVariableNameFromField(fieldName, value, suffix)
                if (!ignoredSet.contains(variableName.lowercase())) {
                    val variableType = determineVariableType(value, fieldName)
                    variables.add(SqlVariable(
                        name = variableName,
                        value = value,
                        type = variableType,
                        position = position
                    ))
                }
            }
        }
    }
    
    /**
     * 构建动态SQL语句
     * @param config 配置信息
     * @param variables 提取的变量列表
     * @return 生成的动态SQL
     */
    private fun buildDynamicSql(config: DynamicSqlConfig, variables: List<SqlVariable>): String {
        val result = StringBuilder()
        
        // 生成变量设置部分
        result.appendLine("-- 设置变量")
        variables.forEach { variable ->
            val value = when (variable.type) {
                VariableType.STRING, VariableType.DATE -> "\"${variable.value}\""
                else -> variable.value
            }
            result.appendLine("SET @${variable.name} = $value;")
        }
        
        // 如果启用分片后缀，添加分片后缀变量
        if (config.enableShardingSuffix) {
            result.appendLine("SET @${config.shardingSuffixVariableName} = 0;")
        }
        result.appendLine()
        
        // 生成SQL语句部分
        val sqlVariableName = "${config.sqlVariablePrefix}_${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMddHHmm"))}"
        result.appendLine("-- sql语句")
        result.append("SET @$sqlVariableName = CONCAT(\"")
        result.appendLine()
        
        // 处理原始SQL，替换变量和表名
        val processedSql = processSqlForDynamicGeneration(config, variables)
        result.append(processedSql)
        
        result.appendLine("\");")
        result.appendLine()
        
        // 生成执行部分
        val statementName = "${config.statementVariablePrefix}_${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMddHHmm"))}"
        result.appendLine("-- 执行sql")
        result.appendLine("PREPARE $statementName FROM @$sqlVariableName;")
        result.appendLine("EXECUTE $statementName;")
        result.appendLine("DEALLOCATE PREPARE $statementName;")
        
        return result.toString()
    }
    
    /**
     * 处理SQL语句，替换变量和表名
     * @param config 配置信息
     * @param variables 变量列表
     * @return 处理后的SQL
     */
    private fun processSqlForDynamicGeneration(config: DynamicSqlConfig, variables: List<SqlVariable>): String {
        var processedSql = config.originalSql
        
        // 按位置从后往前替换，避免位置偏移
        val sortedVariables = variables.sortedByDescending { it.position }
        
        sortedVariables.forEach { variable ->
            val originalValue = variable.value
            
            when (variable.type) {
                VariableType.NUMBER -> {
                    // 数字类型：在CONCAT中直接使用 ",@variable,"
                    val replacement = "\",@${variable.name},\""
                    processedSql = replaceExactValue(processedSql, originalValue, replacement)
                }
                VariableType.STRING, VariableType.DATE -> {
                    // 字符串/日期类型：在CONCAT中使用 '",@variable,"'
                    val singleQuoteReplacement = "\'\",@${variable.name},\"\'"
                    val doubleQuoteReplacement = "\'\",@${variable.name},\"\'"
                    
                    // 替换单引号包裹的值
                    processedSql = replaceExactValueByStr(processedSql, "'$originalValue'", singleQuoteReplacement)
                    // 替换双引号包裹的值
                    processedSql = replaceExactValueByStr(processedSql, "\"$originalValue\"", doubleQuoteReplacement)
                }
                else -> {
                    // 其他类型暂时不处理
                }
            }
        }
        
        // 如果启用分片后缀，替换表名
        if (config.enableShardingSuffix) {
            processedSql = addShardingSuffixToTables(processedSql, config.shardingSuffixFormat, config.shardingSuffixVariableName)
        }
        
        return processedSql
    }
    
    /**
     * 精确替换值，避免重复替换
     * @param sql 原始SQL
     * @param oldValue 要替换的值
     * @param newValue 新值
     * @return 替换后的SQL
     */
    private fun replaceExactValue(sql: String, oldValue: String, newValue: String): String {
        // 使用正则表达式进行精确替换，避免误替换
        val escapedOldValue = Pattern.quote(oldValue)
        val pattern = Pattern.compile("\\b$escapedOldValue\\b", Pattern.CASE_INSENSITIVE)
        return pattern.matcher(sql).replaceAll(newValue)
    }

    private fun replaceExactValueByStr(sql: String, oldValue: String, newValue: String): String {
        return sql.replace(oldValue, newValue);
    }
    
    /**
     * 为表名添加分片后缀
     * @param sql 原始SQL
     * @param suffixFormat 后缀格式
     * @param suffixVariableName 后缀变量名
     * @return 处理后的SQL
     */
    private fun addShardingSuffixToTables(sql: String, suffixFormat: String, suffixVariableName: String): String {
        val matcher = TABLE_PATTERN.matcher(sql)
        val result = StringBuilder()
        var lastEnd = 0
        
        while (matcher.find()) {
            val fullMatch = matcher.group()
            val tableName = matcher.group(1)
            
            // 添加匹配前的部分
            result.append(sql.substring(lastEnd, matcher.start()))
            
            // 处理表名，添加分片后缀
            val newTableName = if (tableName.contains(".")) {
                val schema = tableName.substringBeforeLast(".")
                val table = tableName.substringAfterLast(".")
                "$schema.${table}${suffixFormat}\",@$suffixVariableName,\""
            } else {
                "${tableName}${suffixFormat}\",@$suffixVariableName,\""
            }
            
            // 替换表名
            val newMatch = fullMatch.replace(tableName, newTableName)
            result.append(newMatch)
            
            lastEnd = matcher.end()
        }
        
        // 添加剩余部分
        result.append(sql.substring(lastEnd))
        
        return result.toString()
    }
    
    /**
     * 根据字段名生成变量名
     * @param fieldName 字段名
     * @param value 变量值
     * @param suffix 后缀（可选）
     * @return 变量名
     */
    private fun generateVariableNameFromField(fieldName: String, value: String, suffix: String = ""): String {
        // 提取字段名，去除表别名前缀
        val actualFieldName = if (fieldName.contains(".")) {
            fieldName.substringAfterLast(".")
        } else {
            fieldName
        }
        
        val cleanFieldName = actualFieldName.lowercase()
        
        // 处理常见的字段名模式
        return when {
            cleanFieldName.contains("delete_status") -> "delete_status$suffix"
            cleanFieldName.contains("create_time") -> "create_time$suffix"
            cleanFieldName.contains("update_time") -> "update_time$suffix"
            cleanFieldName.contains("start_time") -> "start_time$suffix"
            cleanFieldName.contains("end_time") -> "end_time$suffix"
            cleanFieldName.contains("time") -> "${cleanFieldName}$suffix"
            cleanFieldName.contains("date") -> "${cleanFieldName}$suffix"
            cleanFieldName.contains("year") -> "${cleanFieldName}$suffix"
            cleanFieldName.contains("month") -> "${cleanFieldName}$suffix"
            cleanFieldName.contains("data") -> "${cleanFieldName}$suffix"
            cleanFieldName.endsWith("_id") -> cleanFieldName + suffix
            cleanFieldName.endsWith("_type") -> cleanFieldName + suffix
            cleanFieldName.endsWith("_status") -> cleanFieldName + suffix
            else -> cleanFieldName + suffix
        }
    }
    
    /**
     * 判断变量类型
     * @param value 变量值
     * @param fieldName 字段名
     * @return 变量类型
     */
    private fun determineVariableType(value: String, fieldName: String): VariableType {
        val cleanValue = value.trim().removeSurrounding("'", "'").removeSurrounding("\"", "\"")
        val cleanFieldName = fieldName.lowercase()
        
        return when {
            // 日期时间类型
            cleanFieldName.contains("time") || cleanFieldName.contains("date") || cleanFieldName.contains("day") ||
                    cleanFieldName.contains("hour") || cleanFieldName.contains("minute") || cleanFieldName.contains("second") ||
            cleanFieldName.contains("year") || cleanFieldName.contains("month") -> VariableType.DATE
            
            // 数字类型
            cleanValue.matches(Regex("\\d+")) -> VariableType.NUMBER
            
            // 布尔类型
            cleanValue.lowercase() in listOf("true", "false", "0", "1") -> VariableType.BOOLEAN
            
            // 字符串类型
            else -> VariableType.STRING
        }
    }
    
    /**
     * 检查字段是否应该被忽略
     * @param fieldName 字段名
     * @return 是否应该忽略
     */
    private fun isIgnoredField(fieldName: String): Boolean {
        val cleanFieldName = fieldName.lowercase()
        val defaultIgnored = listOf("delete_status", "is_delete", "is_deleted", "deleted")
        return defaultIgnored.any { cleanFieldName.contains(it) }
    }
    
    /**
     * 检查位置是否在IN条件内部
     * @param sql SQL语句
     * @param position 位置
     * @return 是否在IN条件内部
     */
    private fun isInsideInCondition(sql: String, position: Int): Boolean {
        val beforePosition = sql.substring(0, position).lowercase()
        val afterPosition = sql.substring(position).lowercase()
        
        // 查找最近的IN关键字
        val inIndex = beforePosition.lastIndexOf(" in ")
        if (inIndex == -1) return false
        
        // 查找IN条件结束的括号
        val inConditionStart = inIndex + 4
        val inConditionEnd = findMatchingParenthesis(sql, inConditionStart)
        
        return position >= inConditionStart && position <= inConditionEnd
    }
    
    /**
     * 查找匹配的括号位置
     * @param sql SQL语句
     * @param startPos 开始位置
     * @return 匹配的括号位置
     */
    private fun findMatchingParenthesis(sql: String, startPos: Int): Int {
        var count = 0
        var pos = startPos
        
        while (pos < sql.length) {
            when (sql[pos]) {
                '(' -> count++
                ')' -> {
                    count--
                    if (count == 0) return pos
                }
            }
            pos++
        }
        
        return sql.length - 1
    }
    
    /**
     * 检查位置是否在字符串内部
     * @param sql SQL语句
     * @param position 位置
     * @return 是否在字符串内部
     */
    private fun isInsideString(sql: String, position: Int): Boolean {
        var inString = false
        var quoteChar: Char? = null
        
        for (i in 0 until position) {
            val char = sql[i]
            when {
                !inString && (char == '\'' || char == '"') -> {
                    inString = true
                    quoteChar = char
                }
                inString && char == quoteChar && (i == 0 || sql[i-1] != '\\') -> {
                    inString = false
                    quoteChar = null
                }
            }
        }
        
        return inString
    }
    
    /**
     * 验证SQL语句是否为单条语句
     * @param sql SQL语句
     * @return 是否为单条语句
     */
    fun validateSingleSqlStatement(sql: String): Boolean {
        val trimmedSql = sql.trim()
        
        // 检查是否包含多个分号（排除字符串中的分号）
        var semicolonCount = 0
        var inString = false
        var quoteChar: Char? = null
        
        for (i in trimmedSql.indices) {
            val char = trimmedSql[i]
            when {
                !inString && (char == '\'' || char == '"') -> {
                    inString = true
                    quoteChar = char
                }
                inString && char == quoteChar && (i == 0 || trimmedSql[i-1] != '\\') -> {
                    inString = false
                    quoteChar = null
                }
                !inString && char == ';' -> {
                    semicolonCount++
                }
            }
        }
        
        return semicolonCount <= 1
    }
    
    /**
     * 检查SQL语句是否包含需要动态化的内容
     * @param sql SQL语句
     * @return 是否包含动态化内容
     */
    fun containsDynamicContent(sql: String): Boolean {
        // 检查是否包含数字或字符串字面量
        return NUMBER_PATTERN.matcher(sql).find() || STRING_PATTERN.matcher(sql).find()
    }
    
    /**
     * 构建配置摘要
     * @param config 配置信息
     * @param variables 变量列表
     * @return 配置摘要
     */
    private fun buildConfigSummary(config: DynamicSqlConfig, variables: List<SqlVariable>): String {
        val summary = StringBuilder()
        summary.append("提取变量${variables.size}个")
        
        if (config.enableShardingSuffix) {
            summary.append("，启用分片后缀")
        }
        
        if (config.ignoredVariables.isNotEmpty()) {
            summary.append("，忽略变量${config.ignoredVariables.size}个")
        }
        
        return summary.toString()
    }
}
