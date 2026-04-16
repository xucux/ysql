package com.github.xucux.ysql.models

import com.github.xucux.ysql.utils.I18nUtil

/**
 * 批量删除存储过程模板枚举
 * 提供预配置的常用场景模板
 */
enum class BatchDeleteTemplate(
    private val displayNameKey: String,
    private val descriptionKey: String,
    val config: BatchDeleteConfig
) {
    /**
     * 系统日志清理模板
     */
    SYSTEM_LOG_CLEANUP(
        displayNameKey = "batch.delete.template.system.log.cleanup.name",
        descriptionKey = "batch.delete.template.system.log.cleanup.description",
        config = BatchDeleteConfig(
            procedureName = "DropHistoryDataByLimit",
            mainTableName = "system_logs",
            primaryKeyField = "id",
            timeField = "log_time",
            limitSize = 1000,
            minId = 0,
            createTimeEnd = "2023-01-01 00:00:00",
            addLogTable = true,
            addTempTable = true,
            customWhereCondition = "log_type IN ('DEBUG', 'INFO')",
            procedureComment = I18nUtil.getMessage("batch.delete.template.system.log.cleanup.comment")
        )
    ),
    
    /**
     * 用户操作日志清理模板
     */
    USER_OPERATION_LOG_CLEANUP(
        displayNameKey = "batch.delete.template.user.operation.log.cleanup.name",
        descriptionKey = "batch.delete.template.user.operation.log.cleanup.description",
        config = BatchDeleteConfig(
            procedureName = "DropUserOperationLogHistory",
            mainTableName = "user_operation_logs",
            primaryKeyField = "id",
            timeField = "create_time",
            limitSize = 2000,
            minId = 0,
            createTimeEnd = "2023-01-01 00:00:00",
            addLogTable = true,
            addTempTable = true,
            customWhereCondition = "operation_type IN ('LOGIN', 'LOGOUT', 'VIEW')",
            procedureComment = I18nUtil.getMessage("batch.delete.template.user.operation.log.cleanup.comment")
        )
    ),
    
    /**
     * 业务数据清理模板
     */
    BUSINESS_DATA_CLEANUP(
        displayNameKey = "batch.delete.template.business.data.cleanup.name",
        descriptionKey = "batch.delete.template.business.data.cleanup.description",
        config = BatchDeleteConfig(
            procedureName = "DropBusinessDataHistory",
            mainTableName = "business_records",
            primaryKeyField = "id",
            timeField = "created_at",
            limitSize = 5000,
            minId = 0,
            createTimeEnd = "2022-01-01 00:00:00",
            addLogTable = true,
            addTempTable = true,
            customWhereCondition = "status = 'COMPLETED'",
            procedureComment = I18nUtil.getMessage("batch.delete.template.business.data.cleanup.comment")
        )
    ),
    
    /**
     * 临时数据清理模板
     */
    TEMP_DATA_CLEANUP(
        displayNameKey = "batch.delete.template.temp.data.cleanup.name",
        descriptionKey = "batch.delete.template.temp.data.cleanup.description",
        config = BatchDeleteConfig(
            procedureName = "DropTempDataHistory",
            mainTableName = "temp_data",
            primaryKeyField = "id",
            timeField = "expire_time",
            limitSize = 10000,
            minId = 0,
            createTimeEnd = "2023-01-01 00:00:00",
            addLogTable = false,
            addTempTable = false,
            customWhereCondition = "is_expired = 1",
            procedureComment = I18nUtil.getMessage("batch.delete.template.temp.data.cleanup.comment")
        )
    ),
    
    /**
     * 审计日志清理模板
     */
    AUDIT_LOG_CLEANUP(
        displayNameKey = "batch.delete.template.audit.log.cleanup.name",
        descriptionKey = "batch.delete.template.audit.log.cleanup.description",
        config = BatchDeleteConfig(
            procedureName = "DropAuditLogHistory",
            mainTableName = "audit_logs",
            primaryKeyField = "id",
            timeField = "audit_time",
            limitSize = 1000,
            minId = 0,
            createTimeEnd = "2022-01-01 00:00:00",
            addLogTable = true,
            addTempTable = true,
            customWhereCondition = "audit_level IN ('INFO', 'DEBUG')",
            procedureComment = I18nUtil.getMessage("batch.delete.template.audit.log.cleanup.comment")
        )
    ),
    
    /**
     * 自定义模板
     */
    CUSTOM(
        displayNameKey = "batch.delete.template.custom.name",
        descriptionKey = "batch.delete.template.custom.description",
        config = BatchDeleteConfig(
            procedureName = "DropHistoryDataByLimit",
            mainTableName = "your_table_name",
            primaryKeyField = "id",
            timeField = "create_time",
            limitSize = 1000,
            minId = 0,
            createTimeEnd = "2023-01-01 00:00:00",
            addLogTable = true,
            addTempTable = true,
            customWhereCondition = "",
            procedureComment = I18nUtil.getMessage("batch.delete.config.default.procedure.comment")
        )
    );

    val displayName: String
        get() = I18nUtil.getMessage(displayNameKey)

    val description: String
        get() = I18nUtil.getMessage(descriptionKey)

    /**
     * 返回当前对象的显示文本。
     */
    override fun toString(): String = displayName
    
    /**
     * 获取模板的详细说明
     */
    fun getDetailedDescription(): String {
        return buildString {
            appendLine(msg("batch.delete.template.detail.name", displayName))
            appendLine(msg("batch.delete.template.detail.description", description))
            appendLine()
            appendLine(msg("batch.delete.template.detail.preset.parameters"))
            appendLine(msg("batch.delete.template.detail.procedure.name", config.procedureName))
            appendLine(msg("batch.delete.template.detail.main.table.name", config.mainTableName))
            appendLine(msg("batch.delete.template.detail.primary.key.field", config.primaryKeyField))
            appendLine(msg("batch.delete.template.detail.time.field", config.timeField))
            appendLine(msg("batch.delete.template.detail.limit.size", config.limitSize))
            appendLine(msg("batch.delete.template.detail.min.id", config.minId))
            appendLine(msg("batch.delete.template.detail.create.time.end", config.createTimeEnd))
            appendLine(msg("batch.delete.template.detail.add.log.table", msg(if (config.addLogTable) "common.yes" else "common.no")))
            appendLine(msg("batch.delete.template.detail.add.temp.table", msg(if (config.addTempTable) "common.yes" else "common.no")))
            if (config.customWhereCondition.isNotBlank()) {
                appendLine(msg("batch.delete.template.detail.custom.where", config.customWhereCondition))
            }
            appendLine(msg("batch.delete.template.detail.procedure.comment", config.procedureComment))
        }
    }
    
    /**
     * 获取模板使用建议
     */
    fun getUsageSuggestion(): String {
        return when (this) {
            SYSTEM_LOG_CLEANUP -> msg("batch.delete.template.system.log.cleanup.suggestion")
            USER_OPERATION_LOG_CLEANUP -> msg("batch.delete.template.user.operation.log.cleanup.suggestion")
            BUSINESS_DATA_CLEANUP -> msg("batch.delete.template.business.data.cleanup.suggestion")
            TEMP_DATA_CLEANUP -> msg("batch.delete.template.temp.data.cleanup.suggestion")
            AUDIT_LOG_CLEANUP -> msg("batch.delete.template.audit.log.cleanup.suggestion")
            CUSTOM -> msg("batch.delete.template.custom.suggestion")
        }
    }

    /**
     * 返回国际化消息文本。
     */
    private fun msg(key: String, vararg args: Any): String = I18nUtil.getMessage(key, *args)
}
