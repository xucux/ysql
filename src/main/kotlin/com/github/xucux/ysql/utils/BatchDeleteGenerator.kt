package com.github.xucux.ysql.utils

import com.github.xucux.ysql.models.BatchDeleteConfig
import com.github.xucux.ysql.models.BatchDeleteResult

/**
 * 批量删除存储过程生成器
 * 负责根据配置生成批量删除存储过程SQL
 */
object BatchDeleteGenerator {
    
    /**
     * 生成批量删除存储过程
     * @param config 配置信息
     * @return 生成结果
     */
    fun generateProcedure(config: BatchDeleteConfig): BatchDeleteResult {
        try {
            // 验证配置
            val validationResult = validateConfig(config)
            if (!validationResult.isValid) {
                return BatchDeleteResult(
                    success = false,
                    errorMessage = validationResult.message
                )
            }
            
            // 生成存储过程SQL
            val generatedProcedure = generateProcedureSql(config)
            val configSummary = generateConfigSummary(config)
            
            return BatchDeleteResult(
                generatedProcedure = generatedProcedure,
                procedureName = config.procedureName,
                mainTableName = config.mainTableName,
                configSummary = configSummary,
                config = config,
                success = true
            )
            
        } catch (e: Exception) {
            return BatchDeleteResult(
                success = false,
                errorMessage = "生成批量删除存储过程时发生错误：${e.message}"
            )
        }
    }
    
    /**
     * 生成存储过程SQL的具体实现
     * @param config 配置信息
     * @return 生成的存储过程SQL
     */
    private fun generateProcedureSql(config: BatchDeleteConfig): String {
        val sb = StringBuilder()
        
        // 存储过程头部
        sb.appendLine("CREATE DEFINER=`root`@`%` PROCEDURE `${config.procedureName}`(")
        sb.appendLine("  IN limit_size INT, -- limit_size每次删除的行数")
        sb.appendLine("  IN create_time_end VARCHAR(50), -- create_time_end小于该创建时间的数据删除")
        sb.appendLine("  IN min_id BIGINT -- 初始化last_id")
        sb.appendLine(")")
        sb.appendLine("  COMMENT '${config.procedureComment}'")
        sb.appendLine("BEGIN")
        sb.appendLine()
        
        // 变量声明
        sb.appendLine("  DECLARE done INT DEFAULT 0;  -- 用于标记是否完成")
        sb.appendLine("  DECLARE last_id BIGINT DEFAULT 0; -- 起始主键")
        sb.appendLine()
        sb.appendLine("  SET last_id = min_id; -- 初始化last_id")
        sb.appendLine()
        
        // 创建临时日志表
        if (config.addLogTable) {
            sb.appendLine("  -- 创建一个临时日志表")
            sb.appendLine("  CREATE TEMPORARY TABLE IF NOT EXISTS drop_data_log (")
            sb.appendLine("    LogID INT AUTO_INCREMENT PRIMARY KEY,")
            sb.appendLine("    Message VARCHAR(2000),")
            sb.appendLine("    LogTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
            sb.appendLine("  );")
            sb.appendLine()
        }
        
        // 创建临时操作表
        if (config.addTempTable) {
            sb.appendLine("  -- 预先创建临时操作表，以便存储每次查询的更新或者删除主键")
            sb.appendLine("  CREATE TEMPORARY TABLE IF NOT EXISTS drop_data_action (")
            sb.appendLine("    temp_id BIGINT,")
            sb.appendLine("    create_time TIMESTAMP")
            sb.appendLine("  );")
            sb.appendLine()
        }
        
        // 主循环
        sb.appendLine("  -- 循环直到没有更多需要更新的记录")
        sb.appendLine("  WHILE done = 0 DO")
        
        if (config.addTempTable) {
            // 使用临时表的版本
            sb.appendLine("    -- 插入查询结果到临时表")
            sb.appendLine("    SET @sql_save_action = CONCAT(\"")
            sb.appendLine("      INSERT INTO drop_data_action (temp_id, create_time)")
            sb.appendLine("      SELECT main.${config.primaryKeyField}, main.${config.timeField}")
            sb.appendLine("      FROM ${config.mainTableName} main")
            sb.appendLine("      ")
            sb.appendLine("      WHERE main.${config.primaryKeyField} > \", last_id, \" ")
            sb.appendLine("        AND main.${config.timeField} < '\", create_time_end, \"'")
            
            // 添加自定义WHERE条件
            if (config.customWhereCondition.isNotBlank()) {
                sb.appendLine("        AND ${config.customWhereCondition}")
            }
            
            sb.appendLine("      ")
            sb.appendLine("      LIMIT \", limit_size);")
            sb.appendLine("    ")
            sb.appendLine("    -- 执行插入操作")
            sb.appendLine("    PREPARE stmt FROM @sql_save_action;")
            sb.appendLine("    EXECUTE stmt;")
            sb.appendLine("    DEALLOCATE PREPARE stmt;")
            sb.appendLine("    ")
            sb.appendLine("    -- 判断本次插入数量是否满足limit_size,如果不足，则拒绝下次循环")
            sb.appendLine("    SELECT COUNT(*) INTO @countData FROM drop_data_action;")
            sb.appendLine("    IF @countData = 0 THEN")
            sb.appendLine("      SET done = 1;")
            sb.appendLine("    ELSE")
            sb.appendLine("      -- 执行物理删除")
            sb.appendLine("      DELETE main FROM ${config.mainTableName} main ")
            sb.appendLine("      INNER JOIN drop_data_action a ON main.${config.primaryKeyField} = a.temp_id")
            sb.appendLine("      WHERE main.${config.timeField} <= create_time_end ")
            sb.appendLine("        AND main.${config.primaryKeyField} > last_id;")
            sb.appendLine("      ")
            sb.appendLine("      -- 缓存操作表中最大主键作为起始主键")
            sb.appendLine("      SET last_id = (SELECT MAX(temp_id) FROM drop_data_action);")
            
            if (config.addLogTable) {
                sb.appendLine("      ")
                sb.appendLine("      INSERT INTO drop_data_log(Message) VALUES ( ")
                sb.appendLine("        CONCAT(\"物理删除${config.mainTableName} last_id:\", last_id, \" 删除数量:\", @countData)")
                sb.appendLine("      );")
            }
            
            sb.appendLine("    END IF;")
            sb.appendLine("    ")
            sb.appendLine("    -- 清空临时操作表以备下一次插入")
            sb.appendLine("    TRUNCATE TABLE drop_data_action;")
        } else {
            // 直接删除的版本
            sb.appendLine("    -- 直接删除数据")
            sb.appendLine("    DELETE FROM ${config.mainTableName}")
            sb.appendLine("    WHERE ${config.primaryKeyField} > last_id")
            sb.appendLine("      AND ${config.timeField} < create_time_end")
            
            // 添加自定义WHERE条件
            if (config.customWhereCondition.isNotBlank()) {
                sb.appendLine("      AND ${config.customWhereCondition}")
            }
            
            sb.appendLine("    LIMIT limit_size;")
            sb.appendLine("    ")
            sb.appendLine("    -- 获取本次删除的行数")
            sb.appendLine("    SET @countData = ROW_COUNT();")
            sb.appendLine("    ")
            sb.appendLine("    -- 如果没有删除任何行，则结束循环")
            sb.appendLine("    IF @countData = 0 THEN")
            sb.appendLine("      SET done = 1;")
            sb.appendLine("    ELSE")
            sb.appendLine("      -- 更新last_id为当前最大主键")
            sb.appendLine("      SET last_id = (SELECT MAX(${config.primaryKeyField}) FROM ${config.mainTableName} WHERE ${config.primaryKeyField} <= last_id + limit_size);")
            
            if (config.addLogTable) {
                sb.appendLine("      ")
                sb.appendLine("      INSERT INTO drop_data_log(Message) VALUES ( ")
                sb.appendLine("        CONCAT(\"物理删除${config.mainTableName} last_id:\", last_id, \" 删除数量:\", @countData)")
                sb.appendLine("      );")
            }
            
            sb.appendLine("    END IF;")
        }
        
        sb.appendLine("  END WHILE;")
        sb.appendLine()
        
        // 返回结果
        if (config.addLogTable) {
            sb.appendLine("  SELECT * FROM drop_data_log;")
            sb.appendLine("  DROP TABLE IF EXISTS drop_data_log;")
        }
        
        if (config.addTempTable) {
            sb.appendLine("  DROP TABLE IF EXISTS drop_data_action;")
        }
        
        sb.appendLine("END")
        
        return sb.toString()
    }
    
    /**
     * 生成配置摘要
     * @param config 配置信息
     * @return 配置摘要
     */
    private fun generateConfigSummary(config: BatchDeleteConfig): String {
        return buildString {
            append("存储过程名: ${config.procedureName}, ")
            append("主表: ${config.mainTableName}, ")
            append("主键: ${config.primaryKeyField}, ")
            append("时间字段: ${config.timeField}, ")
            append("每次删除: ${config.limitSize}行, ")
            append("起始主键: ${config.minId}, ")
            append("截至时间: ${config.createTimeEnd}")
            if (config.customWhereCondition.isNotBlank()) {
                append(", 自定义条件: ${config.customWhereCondition}")
            }
        }
    }
    
    /**
     * 验证配置
     * @param config 配置信息
     * @return 验证结果
     */
    private fun validateConfig(config: BatchDeleteConfig): ValidationResult {
        if (config.procedureName.isBlank()) {
            return ValidationResult(false, "存储过程名称不能为空")
        }
        
        if (config.mainTableName.isBlank()) {
            return ValidationResult(false, "主表名不能为空")
        }
        
        if (config.primaryKeyField.isBlank()) {
            return ValidationResult(false, "主键字段名不能为空")
        }
        
        if (config.timeField.isBlank()) {
            return ValidationResult(false, "时间字段名不能为空")
        }
        
        if (config.limitSize <= 0) {
            return ValidationResult(false, "每次删除行数必须大于0")
        }
        
        if (config.createTimeEnd.isBlank()) {
            return ValidationResult(false, "删除截至时间不能为空")
        }
        
        return ValidationResult(true, "配置验证通过")
    }
    
    private data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
