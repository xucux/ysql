package com.github.xucux.ysql.models

/**
 * 动态语句结果模型
 * 用于存储动态SQL语句生成的结果信息
 */
data class DynamicSqlResult(
    /**
     * 生成的动态SQL语句
     */
    val generatedDynamicSql: String = "",
    
    /**
     * 提取的变量列表
     */
    val extractedVariables: List<SqlVariable> = emptyList(),
    
    /**
     * 原始SQL语句
     */
    val originalSql: String = "",
    
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
    val errorMessage: String? = null,
    
    /**
     * 配置信息摘要
     */
    val configSummary: String = "",
    
    /**
     * 配置信息（用于生成调用示例）
     */
    val config: DynamicSqlConfig? = null
) {
    /**
     * 获取格式化的结果
     */
    fun getFormattedResult(): String {
        if (!success) {
            return "生成失败：${errorMessage ?: "未知错误"}"
        }
        
        val result = StringBuilder()
        result.appendLine("-- 动态SQL语句生成结果")
        result.appendLine("-- 生成时间: ${java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(generateTime), java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
        result.appendLine("-- 配置摘要: $configSummary")
        result.appendLine()
        
        if (extractedVariables.isNotEmpty()) {
            result.appendLine("-- 提取的变量信息:")
            extractedVariables.forEach { variable ->
                result.appendLine("-- 变量名: ${variable.name}, 值: ${variable.value}, 类型: ${variable.type}")
            }
            result.appendLine()
        }
        
        result.appendLine(generatedDynamicSql)
        
        return result.toString()
    }
    
    /**
     * 获取代码预览（简化版本）
     */
    fun getCodePreview(): String {
        if (!success) {
            return "预览生成失败：${errorMessage ?: "未知错误"}"
        }
        
        return generatedDynamicSql
    }
}

/**
 * SQL变量信息
 */
data class SqlVariable(
    /**
     * 变量名
     */
    val name: String,
    
    /**
     * 变量值
     */
    val value: String,
    
    /**
     * 变量类型（如数字、字符串等）
     */
    val type: VariableType = VariableType.STRING,
    
    /**
     * 在SQL中的位置
     */
    val position: Int = 0
)

/**
 * 变量类型枚举
 */
enum class VariableType(val displayName: String) {
    STRING("字符串"),
    NUMBER("数字"),
    BOOLEAN("布尔值"),
    DATE("日期"),
    UNKNOWN("未知")
}
