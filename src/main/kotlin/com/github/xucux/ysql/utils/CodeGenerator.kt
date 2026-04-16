package com.github.xucux.ysql.utils

import com.github.xucux.ysql.models.CodeLanguage
import com.github.xucux.ysql.models.StringBufferConfig
import com.github.xucux.ysql.models.StringBufferResult

/**
 * 代码生成器
 * 负责将SQL语句转换为StringBuffer/StringBuilder代码
 */
object CodeGenerator {
    
    /**
     * 生成StringBuffer代码
     * @param config 配置信息
     * @return 生成结果
     */
    fun generateCode(config: StringBufferConfig): StringBufferResult {
        try {
            // 验证配置
            val validationResult = validateConfig(config)
            if (!validationResult.isValid) {
                return StringBufferResult(
                    success = false,
                    errorMessage = validationResult.message
                )
            }
            
            // 生成代码
            val generatedCode = generateStringBufferCode(config)
            val lines = generatedCode.split("\n")
            
            return StringBufferResult(
                generatedCode = generatedCode,
                lineCount = lines.size,
                charCount = generatedCode.length,
                language = config.language,
                variableName = config.variableName,
                success = true
            )
            
        } catch (e: Exception) {
            return StringBufferResult(
                success = false,
                errorMessage = I18nUtil.getMessage("message.string.buffer.generate.exception", e.message ?: "")
            )
        }
    }
    
    /**
     * 生成StringBuffer代码的具体实现
     * @param config 配置信息
     * @return 生成的代码
     */
    private fun generateStringBufferCode(config: StringBufferConfig): String {
        val result = StringBuilder()
        val sqlLines = config.originalSql.split("\n")
        val commentSymbol = config.language.getCommentSymbol()
        val escapeMethod = config.language.getStringEscapeMethod()
        
        // 添加注释（如果启用）
        if (config.addComments) {
            result.appendLine("$commentSymbol ${I18nUtil.getMessage("code.generator.comment.build.sql", config.language.bufferClass)}")
            result.appendLine("$commentSymbol ${I18nUtil.getMessage("code.generator.comment.variable.name", config.variableName)}")
            result.appendLine("$commentSymbol ${I18nUtil.getMessage("code.generator.comment.original.sql", config.originalSql.replace("\n", " ").trim())}")
            result.appendLine()
        }
        
        // 根据语言生成不同的变量声明
        when (config.language) {
            CodeLanguage.JAVA -> {
                result.appendLine("${config.language.bufferClass} ${config.variableName} = new ${config.language.bufferClass}();")
            }
            CodeLanguage.CSHARP -> {
                result.appendLine("${config.language.bufferClass} ${config.variableName} = new ${config.language.bufferClass}();")
            }
            CodeLanguage.KOTLIN -> {
                result.appendLine("val ${config.variableName} = ${config.language.bufferClass}()")
            }
            CodeLanguage.SCALA -> {
                result.appendLine("val ${config.variableName} = new ${config.language.bufferClass}()")
            }
            CodeLanguage.GROOVY -> {
                result.appendLine("def ${config.variableName} = new ${config.language.bufferClass}()")
            }
        }
        
        // 添加空行（如果启用格式化）
        if (config.formatCode) {
            result.appendLine()
        }
        
        // 生成append语句
        sqlLines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            if (trimmedLine.isNotEmpty()) {
                // 添加注释（如果启用）
                if (config.addComments) {
                    result.appendLine("$commentSymbol ${I18nUtil.getMessage("code.generator.comment.add.sql.fragment", trimmedLine)}")
                }
                
                // 转义字符串并添加空格
                val escapedLine = escapeMethod(trimmedLine)
                when (config.language) {
                    CodeLanguage.CSHARP -> {
                        result.appendLine("${config.variableName}.Append(\"$escapedLine \");")
                    }
                    else -> {
                        result.appendLine("${config.variableName}.append(\"$escapedLine \");")
                    }
                }
                
                // 添加空行（如果启用格式化且不是最后一行）
                if (config.formatCode && index < sqlLines.size - 1) {
                    result.appendLine()
                }
            }
        }
        
        // 添加空行（如果启用格式化）
        if (config.formatCode) {
            result.appendLine()
        }
        
        // 添加注释（如果启用）
        if (config.addComments) {
            result.appendLine("$commentSymbol ${I18nUtil.getMessage("code.generator.comment.final.sql")}")
        }
        
        // 生成最终字符串
        val finalVariableName = capitalize(config.variableName)
        when (config.language) {
            CodeLanguage.JAVA -> {
                result.appendLine("String final$finalVariableName = ${config.variableName}.${config.language.toStringMethod};")
            }
            CodeLanguage.CSHARP -> {
                result.appendLine("string final$finalVariableName = ${config.variableName}.${config.language.toStringMethod};")
            }
            CodeLanguage.KOTLIN -> {
                result.appendLine("val final$finalVariableName = ${config.variableName}.${config.language.toStringMethod}")
            }
            CodeLanguage.SCALA -> {
                result.appendLine("val final$finalVariableName = ${config.variableName}.${config.language.toStringMethod}")
            }
            CodeLanguage.GROOVY -> {
                result.appendLine("def final$finalVariableName = ${config.variableName}.${config.language.toStringMethod}")
            }
        }
        
        return result.toString()
    }
    
    /**
     * 验证配置
     * @param config 配置信息
     * @return 验证结果
     */
    private fun validateConfig(config: StringBufferConfig): ValidationResult {
        // 检查变量名
        if (config.variableName.isBlank()) {
            return ValidationResult(false, I18nUtil.getMessage("validation.variable.name.required"))
        }
        
        if (!isValidVariableName(config.variableName)) {
            return ValidationResult(false, I18nUtil.getMessage("validation.variable.name.invalid"))
        }
        
        // 检查SQL语句
        if (config.originalSql.isBlank()) {
            return ValidationResult(false, I18nUtil.getMessage("validation.sql.required"))
        }
        
        return ValidationResult(true, I18nUtil.getMessage("validation.config.ok"))
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
     * 首字母大写
     * @param str 字符串
     * @return 首字母大写的字符串
     */
    private fun capitalize(str: String): String {
        if (str.isEmpty()) return str
        return str[0].uppercaseChar() + str.substring(1)
    }
    
    /**
     * 获取语言特定的代码模板
     * @param language 编程语言
     * @return 代码模板
     */
    fun getCodeTemplate(language: CodeLanguage): String {
        return when (language) {
            CodeLanguage.JAVA -> """
                // ${I18nUtil.getMessage("code.generator.template.java.example")}
                StringBuffer sql = new StringBuffer();
                sql.append("SELECT * FROM users");
                sql.append(" WHERE id = ?");
                String finalSql = sql.toString();
            """.trimIndent()
            
            CodeLanguage.CSHARP -> """
                // ${I18nUtil.getMessage("code.generator.template.csharp.example")}
                StringBuilder sql = new StringBuilder();
                sql.Append("SELECT * FROM users");
                sql.Append(" WHERE id = ?");
                string finalSql = sql.ToString();
            """.trimIndent()
            
            CodeLanguage.KOTLIN -> """
                // ${I18nUtil.getMessage("code.generator.template.kotlin.example")}
                val sql = StringBuilder()
                sql.append("SELECT * FROM users")
                sql.append(" WHERE id = ?")
                val finalSql = sql.toString()
            """.trimIndent()
            
            CodeLanguage.SCALA -> """
                // ${I18nUtil.getMessage("code.generator.template.scala.example")}
                val sql = new StringBuilder()
                sql.append("SELECT * FROM users")
                sql.append(" WHERE id = ?")
                val finalSql = sql.toString()
            """.trimIndent()
            
            CodeLanguage.GROOVY -> """
                // ${I18nUtil.getMessage("code.generator.template.groovy.example")}
                def sql = new StringBuilder()
                sql.append("SELECT * FROM users")
                sql.append(" WHERE id = ?")
                def finalSql = sql.toString()
            """.trimIndent()
        }
    }
    
    /**
     * 验证结果数据类
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
