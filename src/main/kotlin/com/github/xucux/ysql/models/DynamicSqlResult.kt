package com.github.xucux.ysql.models

import com.github.xucux.ysql.utils.I18nUtil

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
            return msg("dynamic.sql.result.generate.failed", errorMessage ?: msg("common.unknown.error"))
        }
        
        val result = StringBuilder()
        result.appendLine(msg("dynamic.sql.result.header"))
        result.appendLine(msg("dynamic.sql.result.generate.time", formatTime(generateTime)))
        result.appendLine(msg("dynamic.sql.result.config.summary", configSummary))
        result.appendLine()
        
        if (extractedVariables.isNotEmpty()) {
            result.appendLine(msg("dynamic.sql.result.variables.header"))
            extractedVariables.forEach { variable ->
                result.appendLine(msg("dynamic.sql.result.variable.item", variable.name, variable.value, variable.type.displayName))
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
            return msg("dynamic.sql.result.preview.failed", errorMessage ?: msg("common.unknown.error"))
        }
        
        return generatedDynamicSql
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
enum class VariableType(private val displayNameKey: String) {
    STRING("variable.type.string"),
    NUMBER("variable.type.number"),
    BOOLEAN("variable.type.boolean"),
    DATE("variable.type.date"),
    UNKNOWN("variable.type.unknown");

    val displayName: String
        get() = I18nUtil.getMessage(displayNameKey)

    /**
     * 返回当前对象的显示文本。
     */
    override fun toString(): String = displayName
}
