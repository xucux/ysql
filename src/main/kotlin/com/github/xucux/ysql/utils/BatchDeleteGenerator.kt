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
                errorMessage = I18nUtil.getMessage("message.batch.delete.generate.exception", e.message ?: "")
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
        sb.appendLine("  IN limit_size INT, ${I18nUtil.getMessage("batch.delete.generator.comment.limit.size")}")
        sb.appendLine("  IN create_time_end VARCHAR(50), ${I18nUtil.getMessage("batch.delete.generator.comment.create.time.end")}")
        sb.appendLine("  IN min_id BIGINT ${I18nUtil.getMessage("batch.delete.generator.comment.min.id")}")
        sb.appendLine(")")
        sb.appendLine("  COMMENT '${config.procedureComment}'")
        sb.appendLine("BEGIN")
        sb.appendLine()
        
        // 变量声明
        sb.appendLine("  DECLARE done INT DEFAULT 0;  ${I18nUtil.getMessage("batch.delete.generator.comment.done")}")
        sb.appendLine("  DECLARE last_id BIGINT DEFAULT 0; ${I18nUtil.getMessage("batch.delete.generator.comment.last.id")}")
        sb.appendLine()
        sb.appendLine("  SET last_id = min_id; ${I18nUtil.getMessage("batch.delete.generator.comment.init.last.id")}")
        sb.appendLine()
        
        // 创建临时日志表
        if (config.addLogTable) {
            sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.create.log.table"))
            sb.appendLine("  CREATE TEMPORARY TABLE IF NOT EXISTS drop_data_log (")
            sb.appendLine("    LogID INT AUTO_INCREMENT PRIMARY KEY,")
            sb.appendLine("    Message VARCHAR(2000),")
            sb.appendLine("    LogTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
            sb.appendLine("  );")
            sb.appendLine()
        }
        
        // 创建临时操作表
        if (config.addTempTable) {
            sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.create.action.table"))
            sb.appendLine("  CREATE TEMPORARY TABLE IF NOT EXISTS drop_data_action (")
            sb.appendLine("    temp_id BIGINT,")
            sb.appendLine("    create_time TIMESTAMP")
            sb.appendLine("  );")
            sb.appendLine()
        }
        
        // 主循环
        sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.loop"))
        sb.appendLine("  WHILE done = 0 DO")
        
        if (config.addTempTable) {
            // 使用临时表的版本
            sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.insert.to.temp"))
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
            sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.execute.insert"))
            sb.appendLine("    PREPARE stmt FROM @sql_save_action;")
            sb.appendLine("    EXECUTE stmt;")
            sb.appendLine("    DEALLOCATE PREPARE stmt;")
            sb.appendLine("    ")
            sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.check.insert.count"))
            sb.appendLine("    SELECT COUNT(*) INTO @countData FROM drop_data_action;")
            sb.appendLine("    IF @countData = 0 THEN")
            sb.appendLine("      SET done = 1;")
            sb.appendLine("    ELSE")
            sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.physical.delete"))
            sb.appendLine("      DELETE main FROM ${config.mainTableName} main ")
            sb.appendLine("      INNER JOIN drop_data_action a ON main.${config.primaryKeyField} = a.temp_id")
            sb.appendLine("      WHERE main.${config.timeField} <= create_time_end ")
            sb.appendLine("        AND main.${config.primaryKeyField} > last_id;")
            sb.appendLine("      ")
            sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.cache.max.id"))
            sb.appendLine("      SET last_id = (SELECT MAX(temp_id) FROM drop_data_action);")
            
            if (config.addLogTable) {
                sb.appendLine("      ")
                sb.appendLine("      INSERT INTO drop_data_log(Message) VALUES ( ")
                sb.appendLine("        CONCAT(\"${I18nUtil.getMessage("batch.delete.generator.log.physical.delete", config.mainTableName)} last_id:\", last_id, \" ${I18nUtil.getMessage("batch.delete.generator.log.delete.count")}\", @countData)")
                sb.appendLine("      );")
            }
            
            sb.appendLine("    END IF;")
            sb.appendLine("    ")
            sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.truncate.action.table"))
            sb.appendLine("    TRUNCATE TABLE drop_data_action;")
        } else {
            // 直接删除的版本
            sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.delete.directly"))
            sb.appendLine("    DELETE FROM ${config.mainTableName}")
            sb.appendLine("    WHERE ${config.primaryKeyField} > last_id")
            sb.appendLine("      AND ${config.timeField} < create_time_end")
            
            // 添加自定义WHERE条件
            if (config.customWhereCondition.isNotBlank()) {
                sb.appendLine("      AND ${config.customWhereCondition}")
            }
            
            sb.appendLine("    LIMIT limit_size;")
            sb.appendLine("    ")
            sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.get.row.count"))
            sb.appendLine("    SET @countData = ROW_COUNT();")
            sb.appendLine("    ")
            sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.end.if.none"))
            sb.appendLine("    IF @countData = 0 THEN")
            sb.appendLine("      SET done = 1;")
            sb.appendLine("    ELSE")
            sb.appendLine(I18nUtil.getMessage("batch.delete.generator.comment.update.last.id"))
            sb.appendLine("      SET last_id = (SELECT MAX(${config.primaryKeyField}) FROM ${config.mainTableName} WHERE ${config.primaryKeyField} <= last_id + limit_size);")
            
            if (config.addLogTable) {
                sb.appendLine("      ")
                sb.appendLine("      INSERT INTO drop_data_log(Message) VALUES ( ")
                sb.appendLine("        CONCAT(\"${I18nUtil.getMessage("batch.delete.generator.log.physical.delete", config.mainTableName)} last_id:\", last_id, \" ${I18nUtil.getMessage("batch.delete.generator.log.delete.count")}\", @countData)")
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
            append(I18nUtil.getMessage("batch.delete.generator.summary.procedure.name", config.procedureName))
            append(I18nUtil.getMessage("batch.delete.generator.summary.main.table", config.mainTableName))
            append(I18nUtil.getMessage("batch.delete.generator.summary.primary.key", config.primaryKeyField))
            append(I18nUtil.getMessage("batch.delete.generator.summary.time.field", config.timeField))
            append(I18nUtil.getMessage("batch.delete.generator.summary.limit.size", config.limitSize))
            append(I18nUtil.getMessage("batch.delete.generator.summary.min.id", config.minId))
            append(I18nUtil.getMessage("batch.delete.generator.summary.create.time.end", config.createTimeEnd))
            if (config.customWhereCondition.isNotBlank()) {
                append(I18nUtil.getMessage("batch.delete.generator.summary.custom.where", config.customWhereCondition))
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
            return ValidationResult(false, I18nUtil.getMessage("validation.procedure.name.required"))
        }
        
        if (config.mainTableName.isBlank()) {
            return ValidationResult(false, I18nUtil.getMessage("validation.main.table.name.required"))
        }
        
        if (config.primaryKeyField.isBlank()) {
            return ValidationResult(false, I18nUtil.getMessage("validation.primary.key.field.required"))
        }
        
        if (config.timeField.isBlank()) {
            return ValidationResult(false, I18nUtil.getMessage("validation.time.field.required"))
        }
        
        if (config.limitSize <= 0) {
            return ValidationResult(false, I18nUtil.getMessage("validation.delete.limit.gt.zero"))
        }
        
        if (config.createTimeEnd.isBlank()) {
            return ValidationResult(false, I18nUtil.getMessage("validation.delete.end.time.required"))
        }
        
        return ValidationResult(true, I18nUtil.getMessage("validation.config.ok"))
    }
    
    /**
     * 表示 `ValidationResult` 的结果数据。
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
