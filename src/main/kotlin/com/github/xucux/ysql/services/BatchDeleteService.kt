package com.github.xucux.ysql.services

import com.github.xucux.ysql.models.BatchDeleteConfig
import com.github.xucux.ysql.models.BatchDeleteResult
import com.github.xucux.ysql.utils.BatchDeleteGenerator
import com.intellij.openapi.components.Service

/**
 * 批量删除存储过程服务
 * 核心业务逻辑服务，负责批量删除存储过程的生成
 */
@Service
class BatchDeleteService {
    
    /**
     * 生成批量删除存储过程
     * @param config 批量删除配置
     * @return 生成结果
     */
    fun generateBatchDeleteProcedure(config: BatchDeleteConfig): BatchDeleteResult {
        return BatchDeleteGenerator.generateProcedure(config)
    }
    
    /**
     * 验证存储过程名称格式
     * @param procedureName 存储过程名称
     * @return 验证结果
     */
    fun validateProcedureName(procedureName: String): ValidationResult {
        if (procedureName.isBlank()) {
            return ValidationResult(false, "存储过程名称不能为空")
        }
        
        // 检查是否包含非法字符
        val invalidChars = procedureName.filter { !it.isLetterOrDigit() && it != '_' }
        if (invalidChars.isNotEmpty()) {
            return ValidationResult(false, "存储过程名称包含非法字符：$invalidChars")
        }
        
        // 检查是否以数字开头
        if (procedureName.first().isDigit()) {
            return ValidationResult(false, "存储过程名称不能以数字开头")
        }
        
        return ValidationResult(true, "存储过程名称格式正确")
    }
    
    /**
     * 验证表名格式
     * @param tableName 表名
     * @return 验证结果
     */
    fun validateTableName(tableName: String): ValidationResult {
        if (tableName.isBlank()) {
            return ValidationResult(false, "表名不能为空")
        }
        
        // 检查是否包含非法字符
        val invalidChars = tableName.filter { !it.isLetterOrDigit() && it != '_' }
        if (invalidChars.isNotEmpty()) {
            return ValidationResult(false, "表名包含非法字符：$invalidChars")
        }
        
        // 检查是否以数字开头
        if (tableName.first().isDigit()) {
            return ValidationResult(false, "表名不能以数字开头")
        }
        
        return ValidationResult(true, "表名格式正确")
    }
    
    /**
     * 验证字段名格式
     * @param fieldName 字段名
     * @return 验证结果
     */
    fun validateFieldName(fieldName: String): ValidationResult {
        if (fieldName.isBlank()) {
            return ValidationResult(false, "字段名不能为空")
        }
        
        // 检查是否包含非法字符
        val invalidChars = fieldName.filter { !it.isLetterOrDigit() && it != '_' }
        if (invalidChars.isNotEmpty()) {
            return ValidationResult(false, "字段名包含非法字符：$invalidChars")
        }
        
        // 检查是否以数字开头
        if (fieldName.first().isDigit()) {
            return ValidationResult(false, "字段名不能以数字开头")
        }
        
        return ValidationResult(true, "字段名格式正确")
    }
    
    /**
     * 验证时间格式
     * @param timeString 时间字符串
     * @return 验证结果
     */
    fun validateTimeFormat(timeString: String): ValidationResult {
        if (timeString.isBlank()) {
            return ValidationResult(false, "时间不能为空")
        }
        
        // 简单的时间格式验证（支持多种格式）
        val timePatterns = listOf(
            "\\d{4}-\\d{2}-\\d{2}",  // YYYY-MM-DD
            "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}",  // YYYY-MM-DD HH:MM:SS
            "\\d{4}/\\d{2}/\\d{2}",  // YYYY/MM/DD
            "\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}"   // YYYY/MM/DD HH:MM:SS
        )
        
        val isValidFormat = timePatterns.any { pattern ->
            timeString.matches(Regex(pattern))
        }
        
        if (!isValidFormat) {
            return ValidationResult(false, "时间格式不正确，支持的格式：YYYY-MM-DD 或 YYYY-MM-DD HH:MM:SS")
        }
        
        return ValidationResult(true, "时间格式正确")
    }
    
    /**
     * 生成默认的存储过程名称
     * @param tableName 表名
     * @return 默认存储过程名称
     */
    fun generateDefaultProcedureName(tableName: String): String {
        return "DropHistoryDataByLimit_${tableName}"
    }
    
    /**
     * 获取存储过程模板示例
     * @return 模板示例
     */
    fun getProcedureTemplate(): String {
        return """
            -- 批量删除存储过程模板示例
            -- 基础配置：
            -- 存储过程名：DropHistoryDataByLimit
            -- 主表名：system_logs
            -- 主键字段名：id
            -- 时间字段名：log_time
            
            -- 存储过程参数：
            -- limit_size：每次删除行数（如：1000）
            -- create_time_end：删除截至时间（如：'2023-01-01 00:00:00'）
            -- min_id：起始主键值（如：0）
            
            -- 调用示例：
            -- CALL DropHistoryDataByLimit(1000, '2023-01-01 00:00:00', 0);
        """.trimIndent()
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
