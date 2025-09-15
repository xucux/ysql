package com.github.xucux.ysql.models

/**
 * SQL反向解析结果模型
 * 用于存储从StringBuffer/StringBuilder代码中解析出的SQL语句信息
 */
data class SqlReverseResult(
    /**
     * 提取的完整SQL语句
     */
    val extractedSql: String = "",
    
    /**
     * 解析出的SQL语句片段列表
     */
    val sqlStatements: List<String> = emptyList(),
    
    /**
     * 使用的编程语言
     */
    val language: CodeLanguage = CodeLanguage.JAVA,
    
    /**
     * 解析时间
     */
    val parseTime: Long = System.currentTimeMillis(),
    
    /**
     * 是否解析成功
     */
    val success: Boolean = false,
    
    /**
     * 错误信息（如果有）
     */
    val errorMessage: String? = null
) {
    /**
     * 获取统计信息
     */
    fun getStatistics(): String {
        return buildString {
            appendLine("// SQL反向解析统计：")
            appendLine("// 编程语言：${language.displayName}")
            appendLine("// SQL片段数量：${sqlStatements.size}")
            appendLine("// 总字符数：${extractedSql.length}")
            appendLine("// 解析时间：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(parseTime))}")
            appendLine("// 状态：${if (success) "成功" else "失败"}")
            if (!success && errorMessage != null) {
                appendLine("// 错误信息：$errorMessage")
            }
        }
    }
    
    /**
     * 获取SQL预览（前5行）
     */
    fun getSqlPreview(): String {
        val lines = extractedSql.split("\n")
        val previewLines = lines.take(5)
        
        return buildString {
            appendLine("-- 提取的SQL语句预览：")
            previewLines.forEach { line ->
                appendLine(line)
            }
            if (lines.size > 5) {
                appendLine("-- ... (还有 ${lines.size - 5} 行)")
            }
        }
    }
    
    /**
     * 获取格式化的结果
     */
    fun getFormattedResult(): String {
        return buildString {
            appendLine("-- 状态：${if (success) "成功" else "失败"}")
            appendLine("-- 提取的SQL语句：")
            appendLine(extractedSql)
            if (sqlStatements.size > 1) {
                appendLine()
                appendLine("-- SQL片段详情：")
                sqlStatements.forEachIndexed { index, fragment ->
                    appendLine("-- 片段 ${index + 1}: $fragment")
                }
            }
        }
    }
    
    /**
     * 获取SQL语句片段详情
     */
    fun getSqlFragmentsDetail(): String {
        return buildString {
            appendLine("-- SQL语句片段详情：")
            sqlStatements.forEachIndexed { index, fragment ->
                appendLine("-- 片段 ${index + 1}: \"$fragment\"")
            }
        }
    }
    
    /**
     * 检查是否包含特定SQL关键字
     */
    fun containsKeyword(keyword: String): Boolean {
        return extractedSql.contains(keyword, ignoreCase = true)
    }
    
    /**
     * 获取SQL语句类型（SELECT, INSERT, UPDATE, DELETE等）
     */
    fun getSqlType(): String {
        val upperSql = extractedSql.uppercase().trim()
        return when {
            upperSql.startsWith("SELECT") -> "SELECT"
            upperSql.startsWith("INSERT") -> "INSERT"
            upperSql.startsWith("UPDATE") -> "UPDATE"
            upperSql.startsWith("DELETE") -> "DELETE"
            upperSql.startsWith("CREATE") -> "CREATE"
            upperSql.startsWith("DROP") -> "DROP"
            upperSql.startsWith("ALTER") -> "ALTER"
            else -> "UNKNOWN"
        }
    }
}
