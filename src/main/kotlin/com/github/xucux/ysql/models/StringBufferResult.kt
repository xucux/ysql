package com.github.xucux.ysql.models

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
            appendLine("// 代码生成统计：")
            appendLine("// 编程语言：${language.displayName}")
            appendLine("// 变量名称：$variableName")
            appendLine("// 代码行数：$lineCount")
            appendLine("// 字符数量：$charCount")
            appendLine("// 生成时间：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(generateTime))}")
            appendLine("// 状态：${if (success) "成功" else "失败"}")
            if (!success && errorMessage != null) {
                appendLine("// 错误信息：$errorMessage")
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
            appendLine("// 状态：${if (success) "成功" else "失败"}")
            appendLine("// 生成的代码：")
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
}
