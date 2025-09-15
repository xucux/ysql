package com.github.xucux.ysql.models

/**
 * 批量删除存储过程模板枚举
 * 提供预配置的常用场景模板
 */
enum class BatchDeleteTemplate(
    val displayName: String,
    val description: String,
    val config: BatchDeleteConfig
) {
    /**
     * 系统日志清理模板
     */
    SYSTEM_LOG_CLEANUP(
        displayName = "系统日志清理",
        description = "适用于清理系统日志表的历史数据，支持按日志类型过滤",
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
            procedureComment = "循环删除system_logs表中的历史数据-根据limit查询主键防止临时表，再联表删除"
        )
    ),
    
    /**
     * 用户操作日志清理模板
     */
    USER_OPERATION_LOG_CLEANUP(
        displayName = "用户操作日志清理",
        description = "适用于清理用户操作日志表的历史数据",
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
            procedureComment = "循环删除user_operation_logs表中的历史数据"
        )
    ),
    
    /**
     * 业务数据清理模板
     */
    BUSINESS_DATA_CLEANUP(
        displayName = "业务数据清理",
        description = "适用于清理业务相关表的历史数据，如订单、交易记录等",
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
            procedureComment = "循环删除业务数据表中的历史数据"
        )
    ),
    
    /**
     * 临时数据清理模板
     */
    TEMP_DATA_CLEANUP(
        displayName = "临时数据清理",
        description = "适用于清理临时表、缓存表等不需要长期保存的数据",
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
            procedureComment = "清理临时数据表中的过期数据"
        )
    ),
    
    /**
     * 审计日志清理模板
     */
    AUDIT_LOG_CLEANUP(
        displayName = "审计日志清理",
        description = "适用于清理审计日志表的历史数据，保留重要审计记录",
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
            procedureComment = "循环删除审计日志表中的历史数据"
        )
    ),
    
    /**
     * 自定义模板
     */
    CUSTOM(
        displayName = "自定义配置",
        description = "手动配置所有参数，适用于特殊场景",
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
            procedureComment = "批量删除历史数据存储过程"
        )
    );
    
    /**
     * 获取模板的详细说明
     */
    fun getDetailedDescription(): String {
        return buildString {
            appendLine("模板名称：$displayName")
            appendLine("适用场景：$description")
            appendLine()
            appendLine("预配置参数：")
            appendLine("- 存储过程名：${config.procedureName}")
            appendLine("- 主表名：${config.mainTableName}")
            appendLine("- 主键字段：${config.primaryKeyField}")
            appendLine("- 时间字段：${config.timeField}")
            appendLine("- 每次删除行数：${config.limitSize}")
            appendLine("- 起始主键值：${config.minId}")
            appendLine("- 删除截至时间：${config.createTimeEnd}")
            appendLine("- 添加日志表：${if (config.addLogTable) "是" else "否"}")
            appendLine("- 添加临时表：${if (config.addTempTable) "是" else "否"}")
            if (config.customWhereCondition.isNotBlank()) {
                appendLine("- 自定义条件：${config.customWhereCondition}")
            }
            appendLine("- 存储过程注释：${config.procedureComment}")
        }
    }
    
    /**
     * 获取模板使用建议
     */
    fun getUsageSuggestion(): String {
        return when (this) {
            SYSTEM_LOG_CLEANUP -> """
                使用建议：
                1. 建议在系统低峰期执行
                2. 可以根据日志量调整每次删除的行数
                3. 保留ERROR和WARN级别的日志
                4. 执行前建议备份重要日志
            """.trimIndent()
            
            USER_OPERATION_LOG_CLEANUP -> """
                使用建议：
                1. 建议保留登录、登出等重要操作记录
                2. 可以根据用户量调整删除频率
                3. 考虑按用户ID分批次删除
                4. 执行前确认业务需求
            """.trimIndent()
            
            BUSINESS_DATA_CLEANUP -> """
                使用建议：
                1. 只删除已完成状态的业务数据
                2. 建议分批执行，避免影响业务
                3. 执行前必须备份数据
                4. 考虑数据归档而非直接删除
            """.trimIndent()
            
            TEMP_DATA_CLEANUP -> """
                使用建议：
                1. 可以设置定时任务自动执行
                2. 删除频率可以较高
                3. 确认数据确实不需要保留
                4. 监控删除对性能的影响
            """.trimIndent()
            
            AUDIT_LOG_CLEANUP -> """
                使用建议：
                1. 保留重要的审计记录
                2. 遵守合规要求
                3. 建议先归档再删除
                4. 记录删除操作本身
            """.trimIndent()
            
            CUSTOM -> """
                使用建议：
                1. 根据具体业务场景调整参数
                2. 测试环境先验证
                3. 小批量测试后再大批量执行
                4. 监控执行过程和结果
            """.trimIndent()
        }
    }
}
