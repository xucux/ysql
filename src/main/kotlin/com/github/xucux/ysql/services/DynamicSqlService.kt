package com.github.xucux.ysql.services

import com.github.xucux.ysql.models.DynamicSqlConfig
import com.github.xucux.ysql.models.DynamicSqlResult
import com.github.xucux.ysql.models.SqlVariable
import com.github.xucux.ysql.models.VariableType
import com.github.xucux.ysql.utils.DynamicSqlGenerator
import com.intellij.openapi.components.Service

/**
 * 动态SQL服务
 * 核心业务逻辑服务，负责动态SQL语句的生成
 */
@Service
class DynamicSqlService {
    
    /**
     * 生成动态SQL语句
     * @param config 配置信息
     * @return 生成结果
     */
    fun generateDynamicSql(config: DynamicSqlConfig): DynamicSqlResult {
        return DynamicSqlGenerator.generateDynamicSql(config)
    }
    
    /**
     * 获取动态SQL预览
     * @param config 配置信息
     * @return 预览信息
     */
    fun getDynamicSqlPreview(config: DynamicSqlConfig): String {
        val result = generateDynamicSql(config)
        return if (result.success) {
            result.getCodePreview()
        } else {
            "预览生成失败：${result.errorMessage}"
        }
    }
    
    /**
     * 从SQL语句中提取变量
     * @param sql SQL语句
     * @param ignoredVariables 需要忽略的变量列表
     * @return 提取的变量列表
     */
    fun extractVariablesFromSql(sql: String, ignoredVariables: List<String> = emptyList()): List<SqlVariable> {
        return DynamicSqlGenerator.extractVariablesFromSql(sql, ignoredVariables)
    }
    
    /**
     * 验证SQL语句是否为单条语句
     * @param sql SQL语句
     * @return 验证结果
     */
    fun validateSingleSqlStatement(sql: String): Boolean {
        return DynamicSqlGenerator.validateSingleSqlStatement(sql)
    }
    
    /**
     * 检查SQL语句是否包含需要动态化的内容
     * @param sql SQL语句
     * @return 是否包含动态化内容
     */
    fun containsDynamicContent(sql: String): Boolean {
        return DynamicSqlGenerator.containsDynamicContent(sql)
    }
}
