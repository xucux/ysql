package com.github.xucux.ysql.services

import com.github.xucux.ysql.models.CodeLanguage
import com.github.xucux.ysql.models.SqlReverseResult
import com.github.xucux.ysql.models.StringBufferConfig
import com.github.xucux.ysql.models.StringBufferResult
import com.github.xucux.ysql.utils.CodeGenerator
import com.github.xucux.ysql.utils.SqlReverseParser
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
     * 反向解析SQL语句
     * 从StringBuffer/StringBuilder代码中提取SQL语句
     * @param code 包含StringBuffer/StringBuilder的代码
     * @param language 编程语言类型（可选，会自动检测）
     * @return 解析结果
     */
    fun reverseParseSql(code: String, language: CodeLanguage? = null): SqlReverseResult {
        val detectedLanguage = language ?: SqlReverseParser.detectLanguage(code)
        return SqlReverseParser.parseSqlFromCode(code, detectedLanguage)
    }
    
    /**
     * 检测代码中的编程语言类型
     * @param code 代码内容
     * @return 检测到的编程语言
     */
    fun detectCodeLanguage(code: String): CodeLanguage {
        return SqlReverseParser.detectLanguage(code)
    }
    
    /**
     * 验证代码是否包含StringBuffer/StringBuilder
     * @param code 代码内容
     * @return 是否包含StringBuffer/StringBuilder
     */
    fun containsStringBuffer(code: String): Boolean {
        return SqlReverseParser.containsStringBuffer(code)
    }
    
    /**
     * 获取代码中的变量名
     * @param code 代码内容
     * @param language 编程语言
     * @return 变量名列表
     */
    fun extractVariableNames(code: String, language: CodeLanguage): List<String> {
        return SqlReverseParser.extractVariableNames(code, language)
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
     * 验证生成的代码语法
     * @param code 生成的代码
     * @param language 编程语言
     * @return 验证结果
     */
    fun validateGeneratedCode(code: String, language: CodeLanguage): ValidationResult {
        return when (language) {
            CodeLanguage.JAVA -> validateJavaSyntax(code)
            CodeLanguage.CSHARP -> validateCSharpSyntax(code)
            CodeLanguage.KOTLIN -> validateKotlinSyntax(code)
            CodeLanguage.SCALA -> validateScalaSyntax(code)
            CodeLanguage.GROOVY -> validateGroovySyntax(code)
        }
    }
    
    /**
     * 验证Java语法
     */
    private fun validateJavaSyntax(code: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查StringBuffer声明
        if (!code.contains("StringBuffer") && !code.contains("StringBuilder")) {
            errors.add("缺少StringBuffer或StringBuilder声明")
        }
        
        // 检查new关键字
        if (!code.contains("new ")) {
            errors.add("Java中缺少new关键字")
        }
        
        // 检查分号
        val lines = code.split("\n")
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.contains("StringBuffer") || trimmed.contains("StringBuilder") || 
                trimmed.contains("append") || trimmed.contains("String final")) {
                if (!trimmed.endsWith(";")) {
                    errors.add("Java语句缺少分号: $trimmed")
                }
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, "Java语法验证通过")
        } else {
            ValidationResult(false, "Java语法错误: ${errors.joinToString(", ")}")
        }
    }
    
    /**
     * 验证C#语法
     */
    private fun validateCSharpSyntax(code: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查StringBuilder声明
        if (!code.contains("StringBuilder")) {
            errors.add("缺少StringBuilder声明")
        }
        
        // 检查Append方法（大写A）
        if (code.contains(".append(")) {
            errors.add("C#中应该使用Append而不是append")
        }
        
        // 检查string类型（小写s）
        if (code.contains("String final")) {
            errors.add("C#中应该使用string而不是String")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, "C#语法验证通过")
        } else {
            ValidationResult(false, "C#语法错误: ${errors.joinToString(", ")}")
        }
    }
    
    /**
     * 验证Kotlin语法
     */
    private fun validateKotlinSyntax(code: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查val关键字
        if (!code.contains("val ")) {
            errors.add("Kotlin中缺少val关键字")
        }
        
        // 检查不应该有new关键字
        if (code.contains("new ")) {
            errors.add("Kotlin中不应该使用new关键字")
        }
        
        // 检查不应该有分号
        if (code.contains(";")) {
            errors.add("Kotlin中通常不使用分号")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, "Kotlin语法验证通过")
        } else {
            ValidationResult(false, "Kotlin语法错误: ${errors.joinToString(", ")}")
        }
    }
    
    /**
     * 验证Scala语法
     */
    private fun validateScalaSyntax(code: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查val关键字
        if (!code.contains("val ")) {
            errors.add("Scala中缺少val关键字")
        }
        
        // 检查new关键字
        if (!code.contains("new ")) {
            errors.add("Scala中需要new关键字")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, "Scala语法验证通过")
        } else {
            ValidationResult(false, "Scala语法错误: ${errors.joinToString(", ")}")
        }
    }
    
    /**
     * 验证Groovy语法
     */
    private fun validateGroovySyntax(code: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查def关键字
        if (!code.contains("def ")) {
            errors.add("Groovy中缺少def关键字")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, "Groovy语法验证通过")
        } else {
            ValidationResult(false, "Groovy语法错误: ${errors.joinToString(", ")}")
        }
    }
    
    /**
     * 验证结果数据类
     */
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
