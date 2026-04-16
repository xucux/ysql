package com.github.xucux.ysql.models

import com.github.xucux.ysql.utils.I18nUtil

/**
 * StringBuffer结果模型
 * 用于存储StringBuffer代码生成的结果信息
 */
data class StringBufferResult(
    /**
     * 生成的代码
     */
    val generatedCode: String = "",
    
    /**
     * 代码行数
     */
    val lineCount: Int = 0,
    
    /**
     * 代码字符数
     */
    val charCount: Int = 0,
    
    /**
     * 使用的编程语言
     */
    val language: CodeLanguage = CodeLanguage.JAVA,
    
    /**
     * 变量名称
     */
    val variableName: String = "",
    
    /**
     * 生成时间
     */
    val generateTime: Long = System.currentTimeMillis(),
    
    /**
     * 是否生成成功
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
            appendLine(msg("string.buffer.result.statistics.header"))
            appendLine(msg("string.buffer.result.statistics.language", language.displayName))
            appendLine(msg("string.buffer.result.statistics.variable.name", variableName))
            appendLine(msg("string.buffer.result.statistics.line.count", lineCount))
            appendLine(msg("string.buffer.result.statistics.char.count", charCount))
            appendLine(msg("string.buffer.result.statistics.generate.time", formatTime(generateTime)))
            appendLine(msg("string.buffer.result.statistics.status", msg(if (success) "common.status.success" else "common.status.failed")))
            if (!success && errorMessage != null) {
                appendLine(msg("string.buffer.result.statistics.error.message", errorMessage))
            }
        }
    }
    
    /**
     * 获取代码预览（前10行）
     */
    fun getCodePreview(): String {
        val lines = generatedCode.split("\n")
        val previewLines = lines.take(10)
        
        return buildString {
            appendLine("// ${language.fileExtension}")
            previewLines.forEach { line ->
                appendLine(line)
            }
            appendLine("// end ")
        }
    }
    
    /**
     * 获取格式化的结果
     */
    fun getFormattedResult(): String {
        return buildString {
            appendLine(msg("string.buffer.result.formatted.status", msg(if (success) "common.status.success" else "common.status.failed")))
            appendLine(msg("string.buffer.result.formatted.generated.code"))
            // 为生成的代码添加缩进
            generatedCode.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    line.replace("\r", " ")
                    appendLine("$line ")
                } else {
                    appendLine()
                }
            }
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
