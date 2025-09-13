package com.github.xucux.ysql.services

import com.github.xucux.ysql.utils.SqlParser
import com.github.xucux.ysql.utils.EncodingUtils
import com.intellij.openapi.components.Service

/**
 * 表名提取服务
 * 提供智能表名识别和提取功能
 */
@Service
class TableNameExtractorService {
    
    /**
     * 从SQL语句中提取表名
     * @param sql SQL语句
     * @return 表名列表
     */
    fun extractTableNames(sql: String): List<String> {
        return SqlParser.extractTableNames(sql)
    }
    
    /**
     * 验证并提取表名
     * @param sql SQL语句
     * @return 提取结果
     */
    fun validateAndExtractTableNames(sql: String): TableNameExtractionResult {
        // 首先验证SQL格式
        val validationResult = SqlParser.validateSql(sql)
        if (!validationResult.isValid) {
            return TableNameExtractionResult(
                success = false,
                tableNames = emptyList(),
                errorMessage = validationResult.message
            )
        }
        
        // 提取表名
        val tableNames = extractTableNames(sql)
        
        return if (tableNames.isEmpty()) {
            TableNameExtractionResult(
                success = false,
                tableNames = emptyList(),
                errorMessage = EncodingUtils.formatChineseText("未找到有效的表名，请检查SQL语句是否正确")
            )
        } else {
            TableNameExtractionResult(
                success = true,
                tableNames = tableNames,
                errorMessage = null
            )
        }
    }
    
    /**
     * 获取表名提取的统计信息
     * @param sql SQL语句
     * @return 统计信息
     */
    fun getExtractionStatistics(sql: String): String {
        val tableNames = extractTableNames(sql)
        val uniqueTableNames = tableNames.distinct()
        
        return buildString {
            appendLine(EncodingUtils.formatChineseText("表名提取统计："))
            appendLine(EncodingUtils.formatChineseText("• 总表名数量：${tableNames.size}"))
            appendLine(EncodingUtils.formatChineseText("• 唯一表名数量：${uniqueTableNames.size}"))
            appendLine(EncodingUtils.formatChineseText("• 表名列表：${uniqueTableNames.joinToString(", ")}"))
            
            if (tableNames.size != uniqueTableNames.size) {
                appendLine(EncodingUtils.formatChineseText("• 注意：存在重复表名"))
            }
        }
    }
    
    /**
     * 表名提取结果数据类
     */
    data class TableNameExtractionResult(
        val success: Boolean,
        val tableNames: List<String>,
        val errorMessage: String?
    )
}
