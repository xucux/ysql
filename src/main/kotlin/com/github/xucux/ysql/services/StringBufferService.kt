package com.github.xucux.ysql.services

import com.github.xucux.ysql.models.CodeLanguage
import com.github.xucux.ysql.models.SqlReverseResult
import com.github.xucux.ysql.models.StringBufferConfig
import com.github.xucux.ysql.models.StringBufferResult
import com.github.xucux.ysql.utils.CodeGenerator
import com.github.xucux.ysql.utils.I18nUtil
import com.github.xucux.ysql.utils.SqlReverseParser
import com.intellij.openapi.components.Service

/**
 * StringBuffer服务
 * 核心业务逻辑服务，负责StringBuffer代码的生成
 */
@Service
/**
 * 提供 `StringBufferService` 相关业务服务。
 */
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
            msg("message.preview.generate.failed", result.errorMessage ?: "")
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
            appendLine(msg("string.buffer.service.config.statistics.header"))
            appendLine(msg("string.buffer.service.config.statistics.language", config.language.displayName))
            appendLine(msg("string.buffer.service.config.statistics.variable.name", config.variableName))
            appendLine(msg("string.buffer.service.config.statistics.sql.lines", sqlLines.size))
            appendLine(msg("string.buffer.service.config.statistics.non.empty.lines", nonEmptyLines))
            appendLine(msg("string.buffer.service.config.statistics.char.count", totalChars))
            appendLine(msg("string.buffer.service.config.statistics.add.comments", msg(if (config.addComments) "common.yes" else "common.no")))
            appendLine(msg("string.buffer.service.config.statistics.format.code", msg(if (config.formatCode) "common.yes" else "common.no")))
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
            return ValidationResult(false, msg("validation.variable.name.required"))
        }
        
        if (!isValidVariableName(config.variableName)) {
            return ValidationResult(false, msg("validation.variable.name.invalid"))
        }
        
        // 检查SQL语句
        if (config.originalSql.isBlank()) {
            return ValidationResult(false, msg("validation.sql.required"))
        }
        
        // 检查SQL语句长度
        if (config.originalSql.length > 10000) {
            return ValidationResult(false, msg("validation.sql.too.long"))
        }
        
        return ValidationResult(true, msg("validation.config.ok"))
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
            suggestions.add(msg("string.buffer.suggestion.variable.name"))
            
        }
        
        // SQL长度建议
        val sqlLines = config.originalSql.split("\n")
        if (sqlLines.size > 20) {
            suggestions.add(msg("string.buffer.suggestion.long.sql"))
        }
        
        // 语言特定建议
        when (config.language) {
            com.github.xucux.ysql.models.CodeLanguage.KOTLIN -> {
                suggestions.add(msg("string.buffer.suggestion.kotlin.val"))
            }
            com.github.xucux.ysql.models.CodeLanguage.CSHARP -> {
                suggestions.add(msg("string.buffer.suggestion.csharp.var"))
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
            errors.add(msg("string.buffer.syntax.java.missing.builder"))
        }
        
        // 检查new关键字
        if (!code.contains("new ")) {
            errors.add(msg("string.buffer.syntax.java.missing.new"))
        }
        
        // 检查分号
        val lines = code.split("\n")
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.contains("StringBuffer") || trimmed.contains("StringBuilder") || 
                trimmed.contains("append") || trimmed.contains("String final")) {
                if (!trimmed.endsWith(";")) {
                    errors.add(msg("string.buffer.syntax.java.missing.semicolon", trimmed))
                }
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, msg("string.buffer.syntax.java.ok"))
        } else {
            ValidationResult(false, msg("string.buffer.syntax.java.error", errors.joinToString(", ")))
        }
    }
    
    /**
     * 验证C#语法
     */
    private fun validateCSharpSyntax(code: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查StringBuilder声明
        if (!code.contains("StringBuilder")) {
            errors.add(msg("string.buffer.syntax.csharp.missing.builder"))
        }
        
        // 检查Append方法（大写A）
        if (code.contains(".append(")) {
            errors.add(msg("string.buffer.syntax.csharp.use.append"))
        }
        
        // 检查string类型（小写s）
        if (code.contains("String final")) {
            errors.add(msg("string.buffer.syntax.csharp.use.string"))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, msg("string.buffer.syntax.csharp.ok"))
        } else {
            ValidationResult(false, msg("string.buffer.syntax.csharp.error", errors.joinToString(", ")))
        }
    }
    
    /**
     * 验证Kotlin语法
     */
    private fun validateKotlinSyntax(code: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查val关键字
        if (!code.contains("val ")) {
            errors.add(msg("string.buffer.syntax.kotlin.missing.val"))
        }
        
        // 检查不应该有new关键字
        if (code.contains("new ")) {
            errors.add(msg("string.buffer.syntax.kotlin.no.new"))
        }
        
        // 检查不应该有分号
        if (code.contains(";")) {
            errors.add(msg("string.buffer.syntax.kotlin.no.semicolon"))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, msg("string.buffer.syntax.kotlin.ok"))
        } else {
            ValidationResult(false, msg("string.buffer.syntax.kotlin.error", errors.joinToString(", ")))
        }
    }
    
    /**
     * 验证Scala语法
     */
    private fun validateScalaSyntax(code: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查val关键字
        if (!code.contains("val ")) {
            errors.add(msg("string.buffer.syntax.scala.missing.val"))
        }
        
        // 检查new关键字
        if (!code.contains("new ")) {
            errors.add(msg("string.buffer.syntax.scala.need.new"))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, msg("string.buffer.syntax.scala.ok"))
        } else {
            ValidationResult(false, msg("string.buffer.syntax.scala.error", errors.joinToString(", ")))
        }
    }
    
    /**
     * 验证Groovy语法
     */
    private fun validateGroovySyntax(code: String): ValidationResult {
        val errors = mutableListOf<String>()
        
        // 检查def关键字
        if (!code.contains("def ")) {
            errors.add(msg("string.buffer.syntax.groovy.missing.def"))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, msg("string.buffer.syntax.groovy.ok"))
        } else {
            ValidationResult(false, msg("string.buffer.syntax.groovy.error", errors.joinToString(", ")))
        }
    }
    
    /**
     * 验证结果数据类
     */
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )

    /**
     * 返回国际化消息文本。
     */
    private fun msg(key: String, vararg args: Any): String = I18nUtil.getMessage(key, *args)
}
