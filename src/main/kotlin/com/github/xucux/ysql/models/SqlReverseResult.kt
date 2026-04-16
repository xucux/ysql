package com.github.xucux.ysql.models

import com.github.xucux.ysql.utils.I18nUtil

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
            appendLine(msg("sql.reverse.result.statistics.header"))
            appendLine(msg("sql.reverse.result.statistics.language", language.displayName))
            appendLine(msg("sql.reverse.result.statistics.fragment.count", sqlStatements.size))
            appendLine(msg("sql.reverse.result.statistics.char.count", extractedSql.length))
            appendLine(msg("sql.reverse.result.statistics.parse.time", formatTime(parseTime)))
            appendLine(msg("sql.reverse.result.statistics.status", msg(if (success) "common.status.success" else "common.status.failed")))
            if (!success && errorMessage != null) {
                appendLine(msg("sql.reverse.result.statistics.error.message", errorMessage))
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
            appendLine(msg("sql.reverse.result.preview.header"))
            previewLines.forEach { line ->
                appendLine(line)
            }
            if (lines.size > 5) {
                appendLine(msg("sql.reverse.result.preview.more.lines", lines.size - 5))
            }
        }
    }
    
    /**
     * 获取格式化的结果
     */
    fun getFormattedResult(): String {
        return buildString {
            appendLine(msg("sql.reverse.result.formatted.status", msg(if (success) "common.status.success" else "common.status.failed")))
            appendLine(msg("sql.reverse.result.formatted.extracted.sql"))
            appendLine(extractedSql)
            if (sqlStatements.size > 1) {
                appendLine()
                appendLine(msg("sql.reverse.result.formatted.fragments.header"))
                sqlStatements.forEachIndexed { index, fragment ->
                    appendLine(msg("sql.reverse.result.formatted.fragment.item", index + 1, fragment))
                }
            }
        }
    }
    
    /**
     * 获取SQL语句片段详情
     */
    fun getSqlFragmentsDetail(): String {
        return buildString {
            appendLine(msg("sql.reverse.result.fragments.detail.header"))
            sqlStatements.forEachIndexed { index, fragment ->
                appendLine(msg("sql.reverse.result.fragments.detail.item", index + 1, fragment))
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

    /**
     * 格式化 `time`。
     */
    private fun formatTime(time: Long): String {
        return java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(time),
            java.time.ZoneId.systemDefault()
        ).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    /**
     * 返回国际化消息文本。
     */
    private fun msg(key: String, vararg args: Any): String = I18nUtil.getMessage(key, *args)
}
