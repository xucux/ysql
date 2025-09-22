package com.github.xucux.ysql.models

/**
 * 动态语句配置模型
 * 用于存储动态SQL语句生成的相关配置信息
 */
data class DynamicSqlConfig(
    /**
     * 原始SQL语句
     */
    val originalSql: String = "",
    
    /**
     * 需要忽略的变量名列表（除了默认忽略的delete_status,is_delete,is_deleted,deleted）
     */
    val ignoredVariables: List<String> = emptyList(),
    
    /**
     * 是否启用分片后缀
     */
    val enableShardingSuffix: Boolean = false,
    
    /**
     * 分片后缀格式（如 "_", "-", "" 等）
     */
    val shardingSuffixFormat: String = "_",
    
    /**
     * 分片后缀变量名
     */
    val shardingSuffixVariableName: String = "sharding_suffix",
    
    /**
     * 生成的SQL变量名前缀
     */
    val sqlVariablePrefix: String = "query_sql",
    
    /**
     * 生成的语句变量名前缀
     */
    val statementVariablePrefix: String = "stmt"
) {
    /**
     * 获取所有需要忽略的变量名（包括默认忽略的）
     */
    fun getAllIgnoredVariables(): List<String> {
        val defaultIgnored = listOf("delete_status", "is_delete", "is_deleted", "deleted")
        return (defaultIgnored + ignoredVariables).distinct()
    }
}
