package com.github.xucux.ysql.services

import com.github.xucux.ysql.models.StringBufferConfig
import com.github.xucux.ysql.models.StringBufferResult
import com.github.xucux.ysql.utils.CodeGenerator
import com.intellij.openapi.components.Service

/**
 * StringBuffer服务
 * 核心业务逻辑服务，负责StringBuffer代码的生成
 */
@Service
class StringBufferService {
    
    /**
     * 生成StringBuffer代码
     * @param config 配置信息
     * @return 生成结果
     */
    fun generateStringBufferCode(config: StringBufferConfig): StringBufferResult {
        return CodeGenerator.generateCode(config)
    }
    
    /**
     * 获取代码预览
     * @param config 配置信息
     * @return 预览信息
     */
    fun getCodePreview(config: StringBufferConfig): String {
        val result = generateStringBufferCode(config)
        return if (result.success) {
            result.getCodePreview()
        } else {
            "预览生成失败：${result.errorMessage}"
        }
    }
    
    /**
     * 获取配置统计信息
     * @param config 配置信息
     * @return 统计信息
     */
    fun getConfigStatistics(config: StringBufferConfig): String {
        val sqlLines = config.originalSql.split("\n")
        val nonEmptyLines = sqlLines.count { it.trim().isNotEmpty() }
        val totalChars = config.originalSql.length
        
        return buildString {
            appendLine("配置统计信息：")
            appendLine("• 编程语言：${config.language.displayName}")
            appendLine("• 变量名称：${config.variableName}")
            appendLine("• SQL行数：${sqlLines.size}")
            appendLine("• 非空行数：$nonEmptyLines")
            appendLine("• 字符数量：$totalChars")
            appendLine("• 添加注释：${if (config.addComments) "是" else "否"}")
            appendLine("• 格式化代码：${if (config.formatCode) "是" else "否"}")
        }
    }
    
    /**
     * 验证配置
     * @param config 配置信息
     * @return 验证结果
     */
    fun validateConfig(config: StringBufferConfig): ValidationResult {
        // 检查变量名
        if (config.variableName.isBlank()) {
            return ValidationResult(false, "变量名不能为空")
        }
        
        if (!isValidVariableName(config.variableName)) {
            return ValidationResult(false, "变量名格式不正确，只能包含字母、数字和下划线，且不能以数字开头")
        }
        
        // 检查SQL语句
        if (config.originalSql.isBlank()) {
            return ValidationResult(false, "SQL语句不能为空")
        }
        
        // 检查SQL语句长度
        if (config.originalSql.length > 10000) {
            return ValidationResult(false, "SQL语句过长，请控制在10000字符以内")
        }
        
        return ValidationResult(true, "配置验证通过")
    }
    
    /**
     * 获取语言特定的代码模板
     * @param language 编程语言
     * @return 代码模板
     */
    fun getCodeTemplate(language: com.github.xucux.ysql.models.CodeLanguage): String {
        return CodeGenerator.getCodeTemplate(language)
    }
    
    /**
     * 获取支持的语言列表
     * @return 语言列表
     */
    fun getSupportedLanguages(): List<com.github.xucux.ysql.models.CodeLanguage> {
        return com.github.xucux.ysql.models.CodeLanguage.values().toList()
    }
    
    /**
     * 验证变量名是否有效
     * @param variableName 变量名
     * @return 是否有效
     */
    private fun isValidVariableName(variableName: String): Boolean {
        if (variableName.isEmpty()) return false
        
        // 检查第一个字符
        val firstChar = variableName[0]
        if (!firstChar.isLetter() && firstChar != '_') {
            return false
        }
        
        // 检查其余字符
        for (char in variableName.substring(1)) {
            if (!char.isLetterOrDigit() && char != '_') {
                return false
            }
        }
        
        return true
    }
    
    /**
     * 获取代码生成建议
     * @param config 配置信息
     * @return 建议列表
     */
    fun getCodeGenerationSuggestions(config: StringBufferConfig): List<String> {
        val suggestions = mutableListOf<String>()
        
        // 变量名建议
        if (config.variableName.length < 3) {
            suggestions.add("建议使用更具描述性的变量名，如 'sqlBuilder' 或 'queryBuilder'")
        }
        
        // SQL长度建议
        val sqlLines = config.originalSql.split("\n")
        if (sqlLines.size > 20) {
            suggestions.add("SQL语句较长，建议考虑拆分为多个方法或使用配置文件")
        }
        
        // 语言特定建议
        when (config.language) {
            com.github.xucux.ysql.models.CodeLanguage.KOTLIN -> {
                suggestions.add("Kotlin中建议使用 'val' 关键字声明不可变变量")
            }
            com.github.xucux.ysql.models.CodeLanguage.CSHARP -> {
                suggestions.add("C#中建议使用 'var' 关键字进行类型推断")
            }
            else -> {
                // 其他语言暂无特殊建议
            }
        }
        
        return suggestions
    }
    
    /**
     * 验证结果数据类
     */
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
