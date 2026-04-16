package com.github.xucux.ysql.services

import com.github.xucux.ysql.utils.SqlParser
import com.github.xucux.ysql.utils.EncodingUtils
import com.github.xucux.ysql.utils.I18nUtil
import com.intellij.openapi.components.Service

/**
 * 表名提取服务
 * 提供智能表名识别和提取功能
 */
@Service
/**
 * 提供 `TableNameExtractorService` 相关业务服务。
 */
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
                errorMessage = EncodingUtils.formatChineseText(msg("table.name.extractor.no.valid.table"))
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
            appendLine(EncodingUtils.formatChineseText(msg("table.name.extractor.statistics.header")))
            appendLine(EncodingUtils.formatChineseText(msg("table.name.extractor.statistics.total.count", tableNames.size)))
            appendLine(EncodingUtils.formatChineseText(msg("table.name.extractor.statistics.unique.count", uniqueTableNames.size)))
            appendLine(EncodingUtils.formatChineseText(msg("table.name.extractor.statistics.table.names", uniqueTableNames.joinToString(", "))))
            
            if (tableNames.size != uniqueTableNames.size) {
                appendLine(EncodingUtils.formatChineseText(msg("table.name.extractor.statistics.duplicate.notice")))
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

    /**
     * 返回国际化消息文本。
     */
    private fun msg(key: String, vararg args: Any): String = I18nUtil.getMessage(key, *args)
}
