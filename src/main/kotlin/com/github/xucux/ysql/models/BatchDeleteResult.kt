package com.github.xucux.ysql.models

import com.github.xucux.ysql.utils.I18nUtil

/**
 * 批量删除存储过程结果模型
 * 用于存储批量删除存储过程生成的结果信息
 */
data class BatchDeleteResult(
    /**
     * 生成的存储过程SQL
     */
    val generatedProcedure: String = "",
    
    /**
     * 存储过程名称
     */
    val procedureName: String = "",
    
    /**
     * 主表名
     */
    val mainTableName: String = "",
    
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
    val config: BatchDeleteConfig? = null
) {
    /**
     * 获取格式化的结果
     */
    fun getFormattedResult(): String {
        return buildString {
            appendLine(msg("batch.delete.result.header"))
            appendLine(msg("batch.delete.result.procedure.name", procedureName))
            appendLine(msg("batch.delete.result.main.table.name", mainTableName))
            appendLine(msg("batch.delete.result.generate.time", formatTime(generateTime)))
            appendLine(msg("batch.delete.result.status", msg(if (success) "common.status.success" else "common.status.failed")))
            if (!success && errorMessage != null) {
                appendLine(msg("batch.delete.result.error.message", errorMessage))
            }
            if (configSummary.isNotBlank()) {
                appendLine(msg("batch.delete.result.config.summary", configSummary))
            }
            appendLine()
            appendLine(msg("batch.delete.result.call.example.header"))
            appendLine(getCallExample())
            appendLine()
            appendLine(msg("batch.delete.result.generated.sql.header"))
            appendLine("=".repeat(50))
            appendLine(generatedProcedure)
        }
    }
    
    /**
     * 获取调用示例
     */
    fun getCallExample(): String {
        return if (config != null) {
            buildString {
                appendLine("CALL $procedureName(${config.limitSize}, ${config.minId}, '${config.createTimeEnd}');")
                appendLine()
                appendLine(msg("batch.delete.result.call.example.parameter.header"))
                appendLine(msg("batch.delete.result.call.example.parameter.limit", config.limitSize))
                appendLine(msg("batch.delete.result.call.example.parameter.min.id", config.minId))
                appendLine(msg("batch.delete.result.call.example.parameter.end.time", config.createTimeEnd))
                if (config.customWhereCondition.isNotBlank()) {
                    appendLine(msg("batch.delete.result.call.example.parameter.custom.where", config.customWhereCondition))
                }
                appendLine()
                appendLine(msg("batch.delete.result.call.example.execution.header"))
                appendLine(msg("batch.delete.result.call.example.execution.delete.history", config.mainTableName))
                appendLine(msg("batch.delete.result.call.example.execution.limit", config.limitSize))
                appendLine(msg("batch.delete.result.call.example.execution.condition", config.primaryKeyField, config.minId, config.timeField, config.createTimeEnd))
                if (config.customWhereCondition.isNotBlank()) {
                    appendLine(msg("batch.delete.result.call.example.execution.extra.condition", config.customWhereCondition))
                }
            }
        } else {
            msg("batch.delete.result.call.example.unavailable")
        }
    }
    
    /**
     * 获取统计信息
     */
    fun getStatistics(): String {
        return buildString {
            appendLine(msg("batch.delete.result.statistics.header"))
            appendLine(msg("batch.delete.result.procedure.name", procedureName))
            appendLine(msg("batch.delete.result.main.table.name", mainTableName))
            appendLine(msg("batch.delete.result.generate.time", formatTime(generateTime)))
            appendLine(msg("batch.delete.result.status", msg(if (success) "common.status.success" else "common.status.failed")))
            if (!success && errorMessage != null) {
                appendLine(msg("batch.delete.result.error.message", errorMessage))
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
