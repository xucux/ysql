package com.github.xucux.ysql.utils

import java.util.regex.Pattern

/**
 * SQL解析工具类
 * 提供SQL语句解析和表名替换功能
 */
object SqlParser {
    
    /**
     * 表名匹配的正则表达式
     * 匹配 FROM, JOIN, UPDATE, INSERT INTO, DELETE FROM 后的表名
     */
    private val TABLE_PATTERN = Pattern.compile(
        "\\b(?:FROM|JOIN|UPDATE|INSERT\\s+INTO|DELETE\\s+FROM)\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)?)\\b",
        Pattern.CASE_INSENSITIVE
    )
    
    /**
     * SQL关键字集合，用于过滤非表名
     */
    private val SQL_KEYWORDS = setOf(
        "select", "from", "where", "join", "left", "right", "inner", "outer",
        "group", "by", "order", "having", "limit", "offset", "union", "all",
        "insert", "update", "delete", "into", "set", "values", "create", "drop",
        "table", "index", "view", "database", "schema", "user", "grant", "revoke",
        "alter", "truncate", "explain", "describe", "show", "use", "commit", "rollback"
    )
    
    /**
     * 从SQL语句中提取表名
     * @param sql SQL语句
     * @return 表名列表
     */
    fun extractTableNames(sql: String): List<String> {
        val tableNames = mutableSetOf<String>()
        val matcher = TABLE_PATTERN.matcher(sql)
        
        while (matcher.find()) {
            val tableName = matcher.group(1)?.trim()
            if (tableName != null && !isSqlKeyword(tableName)) {
                // 处理带schema的表名，如 schema.table
                val actualTableName = if (tableName.contains(".")) {
                    tableName.substringAfterLast(".")
                } else {
                    tableName
                }
                tableNames.add(actualTableName)
            }
        }
        
        return tableNames.toList()
    }
    
    /**
     * 替换SQL中的表名
     * @param sql 原始SQL
     * @param oldTableName 原表名
     * @param newTableName 新表名
     * @return 替换后的SQL
     */
    fun replaceTableName(sql: String, oldTableName: String, newTableName: String): String {
        // 使用正则表达式进行精确替换，避免误替换
        val pattern = Pattern.compile(
            "\\b(?:FROM|JOIN|UPDATE|INSERT\\s+INTO|DELETE\\s+FROM)\\s+\\b$oldTableName\\b",
            Pattern.CASE_INSENSITIVE
        )
        
        return pattern.matcher(sql).replaceAll { matchResult ->
            val prefix = matchResult.group().substringBefore(oldTableName)
            "$prefix$newTableName"
        }
    }
    
    /**
     * 验证SQL语句格式
     * @param sql SQL语句
     * @return 验证结果
     */
    fun validateSql(sql: String): ValidationResult {
        if (sql.isBlank()) {
            return ValidationResult(false, "SQL语句不能为空")
        }
        
        val trimmedSql = sql.trim()
        
        // 检查是否包含基本的SQL关键字
        val hasValidKeyword = listOf("SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER")
            .any { trimmedSql.uppercase().contains(it) }
        
        if (!hasValidKeyword) {
            return ValidationResult(false, "SQL语句格式不正确，请检查是否包含有效的SQL关键字")
        }
        
        // 检查括号匹配
        val openParens = trimmedSql.count { it == '(' }
        val closeParens = trimmedSql.count { it == ')' }
        
        if (openParens != closeParens) {
            return ValidationResult(false, "SQL语句中括号不匹配")
        }
        
        return ValidationResult(true, "SQL语句格式正确")
    }
    
    /**
     * 检查字符串是否为SQL关键字
     */
    private fun isSqlKeyword(word: String): Boolean {
        return SQL_KEYWORDS.contains(word.lowercase())
    }
    
    /**
     * 验证结果数据类
     */
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
