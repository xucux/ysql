package com.github.xucux.ysql.models

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
            appendLine("- 批量删除存储过程生成结果：")
            appendLine("- 存储过程名称：$procedureName")
            appendLine("- 主表名：$mainTableName")
            appendLine("- 生成时间：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(generateTime))}")
            appendLine("- 状态：${if (success) "成功" else "失败"}")
            if (!success && errorMessage != null) {
                appendLine("- 错误信息：$errorMessage")
            }
            if (configSummary.isNotBlank()) {
                appendLine("- 配置摘要：$configSummary")
            }
            appendLine()
            appendLine("- 调用示例：")
            appendLine(getCallExample())
            appendLine()
            appendLine("- 生成的存储过程SQL：")
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
                appendLine("参数说明：")
                appendLine("- 参数1 (limitSize): ${config.limitSize} - 每次删除的行数")
                appendLine("- 参数2 (minId): ${config.minId} - 起始主键值")
                appendLine("- 参数3 (createTimeEnd): '${config.createTimeEnd}' - 删除截至时间")
                if (config.customWhereCondition.isNotBlank()) {
                    appendLine("- 自定义条件: ${config.customWhereCondition}")
                }
                appendLine()
                appendLine("执行说明：")
                appendLine("- 该存储过程会循环删除 ${config.mainTableName} 表中满足条件的历史数据")
                appendLine("- 每次删除 ${config.limitSize} 行，避免长时间锁表")
                appendLine("- 删除条件：${config.primaryKeyField} >= ${config.minId} AND ${config.timeField} <= '${config.createTimeEnd}'")
                if (config.customWhereCondition.isNotBlank()) {
                    appendLine("- 额外条件：${config.customWhereCondition}")
                }
            }
        } else {
            "配置信息不可用，无法生成调用示例"
        }
    }
    
    /**
     * 获取统计信息
     */
    fun getStatistics(): String {
        return buildString {
            appendLine("- 批量删除存储过程统计信息：")
            appendLine("- 存储过程名称：$procedureName")
            appendLine("- 主表名：$mainTableName")
            appendLine("- 生成时间：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(generateTime))}")
            appendLine("- 状态：${if (success) "成功" else "失败"}")
            if (!success && errorMessage != null) {
                appendLine("- 错误信息：$errorMessage")
            }
        }
    }
}
