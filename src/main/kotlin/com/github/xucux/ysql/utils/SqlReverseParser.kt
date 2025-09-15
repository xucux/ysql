package com.github.xucux.ysql.utils

import com.github.xucux.ysql.models.CodeLanguage
import com.github.xucux.ysql.models.SqlReverseResult

/**
 * SQL反向解析器
 * 负责从StringBuffer/StringBuilder代码中提取SQL语句
 */
object SqlReverseParser {
    
    /**
     * 从代码中反向解析SQL语句
     * @param code 包含StringBuffer/StringBuilder的代码
     * @param language 编程语言类型
     * @return 解析结果
     */
    fun parseSqlFromCode(code: String, language: CodeLanguage = CodeLanguage.JAVA): SqlReverseResult {
        try {
            // 验证输入
            if (code.isBlank()) {
                return SqlReverseResult(
                    success = false,
                    errorMessage = "代码不能为空"
                )
            }
            
            // 解析SQL语句
            val sqlStatements = extractSqlStatements(code, language)
            
            if (sqlStatements.isEmpty()) {
                return SqlReverseResult(
                    success = false,
                    errorMessage = "未找到有效的SQL语句"
                )
            }
            
            // 合并SQL语句
            val combinedSql = combineSqlStatements(sqlStatements)
            
            return SqlReverseResult(
                extractedSql = combinedSql,
                sqlStatements = sqlStatements,
                language = language,
                success = true
            )
            
        } catch (e: Exception) {
            return SqlReverseResult(
                success = false,
                errorMessage = "解析SQL时发生错误：${e.message}"
            )
        }
    }
    
    /**
     * 从代码中提取SQL语句
     * @param code 代码内容
     * @param language 编程语言
     * @return SQL语句列表
     */
    private fun extractSqlStatements(code: String, language: CodeLanguage): List<String> {
        val sqlStatements = mutableListOf<String>()
        val lines = code.split("\n")
        
        // 根据语言类型确定append方法名
        val appendMethod = when (language) {
            CodeLanguage.CSHARP -> "Append"
            else -> "append"
        }
        
        // 正则表达式匹配append语句
        val appendPattern = Regex("""\.$appendMethod\s*\(\s*["']([^"']*)["']\s*\)""", RegexOption.IGNORE_CASE)
        
        for (line in lines) {
            val match = appendPattern.find(line)
            if (match != null) {
                val sqlPart = match.groupValues[1]
                if (sqlPart.isNotBlank()) {
                    sqlStatements.add(sqlPart)
                }
            }
        }
        
        return sqlStatements
    }
    
    /**
     * 合并SQL语句片段
     * @param sqlStatements SQL语句片段列表
     * @return 合并后的完整SQL语句
     */
    private fun combineSqlStatements(sqlStatements: List<String>): String {
        return sqlStatements.joinToString(" ").trim()
    }
    
    /**
     * 检测代码中的编程语言类型
     * @param code 代码内容
     * @return 检测到的编程语言
     */
    fun detectLanguage(code: String): CodeLanguage {
        return when {
            code.contains("StringBuilder") && code.contains("Append(") -> CodeLanguage.CSHARP
            code.contains("StringBuilder") && code.contains("append(") -> CodeLanguage.KOTLIN
            code.contains("StringBuffer") -> CodeLanguage.JAVA
            code.contains("val ") && code.contains("StringBuilder") -> CodeLanguage.SCALA
            code.contains("def ") && code.contains("StringBuilder") -> CodeLanguage.GROOVY
            else -> CodeLanguage.JAVA // 默认Java
        }
    }
    
    /**
     * 验证代码是否包含StringBuffer/StringBuilder
     * @param code 代码内容
     * @return 是否包含StringBuffer/StringBuilder
     */
    fun containsStringBuffer(code: String): Boolean {
        val bufferClasses = listOf("StringBuffer", "StringBuilder")
        return bufferClasses.any { code.contains(it) }
    }
    
    /**
     * 获取代码中的变量名
     * @param code 代码内容
     * @param language 编程语言
     * @return 变量名列表
     */
    fun extractVariableNames(code: String, language: CodeLanguage): List<String> {
        val variableNames = mutableSetOf<String>()
        val lines = code.split("\n")
        
        // 根据语言类型确定变量声明模式
        val declarationPattern = when (language) {
            CodeLanguage.CSHARP -> Regex("""StringBuilder\s+(\w+)\s*=""")
            CodeLanguage.KOTLIN -> Regex("""val\s+(\w+)\s*=\s*StringBuilder""")
            CodeLanguage.SCALA -> Regex("""val\s+(\w+)\s*=\s*new\s+StringBuilder""")
            CodeLanguage.GROOVY -> Regex("""def\s+(\w+)\s*=\s*new\s+StringBuilder""")
            else -> Regex("""StringBuffer\s+(\w+)\s*=""") // Java
        }
        
        for (line in lines) {
            val match = declarationPattern.find(line)
            if (match != null) {
                variableNames.add(match.groupValues[1])
            }
        }
        
        return variableNames.toList()
    }
    
    /**
     * 格式化SQL语句
     * @param sql 原始SQL语句
     * @return 格式化后的SQL语句
     */
    fun formatSql(sql: String): String {
        return sql
            .replace(Regex("\\s+"), " ") // 合并多个空格
            .replace(Regex("\\s*,\\s*"), ", ") // 格式化逗号
            .replace(Regex("\\s*\\(\\s*"), " (") // 格式化左括号
            .replace(Regex("\\s*\\)\\s*"), ") ") // 格式化右括号
            .trim()
    }
    
    /**
     * 获取解析统计信息
     * @param result 解析结果
     * @return 统计信息字符串
     */
    fun getParseStatistics(result: SqlReverseResult): String {
        return buildString {
            appendLine("// SQL反向解析统计：")
            appendLine("// 编程语言：${result.language.displayName}")
            appendLine("// SQL片段数量：${result.sqlStatements.size}")
            appendLine("// 总字符数：${result.extractedSql.length}")
            appendLine("// 解析状态：${if (result.success) "成功" else "失败"}")
            if (!result.success && result.errorMessage != null) {
                appendLine("// 错误信息：${result.errorMessage}")
            }
        }
    }
}
