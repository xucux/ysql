package com.github.xucux.ysql.models

/**
 * StringBuffer配置模型
 * 用于存储StringBuffer代码生成的相关配置信息
 */
data class StringBufferConfig(
    /**
     * 变量名称
     */
    val variableName: String = "sql",
    
    /**
     * 编程语言
     */
    val language: CodeLanguage = CodeLanguage.JAVA,
    
    /**
     * 原始SQL语句
     */
    val originalSql: String = "",
    
    /**
     * 是否添加注释
     */
    val addComments: Boolean = true,
    
    /**
     * 是否格式化代码
     */
    val formatCode: Boolean = true
)

/**
 * 支持的编程语言枚举
 */
enum class CodeLanguage(
    val displayName: String,
    val bufferClass: String,
    val toStringMethod: String,
    val fileExtension: String
) {
    JAVA("Java", "StringBuffer", "toString()", "java"),
    CSHARP("C#", "StringBuilder", "ToString()", "cs"),
    KOTLIN("Kotlin", "StringBuilder", "toString()", "kt"),
    SCALA("Scala", "StringBuilder", "toString()", "scala"),
    GROOVY("Groovy", "StringBuilder", "toString()", "groovy");
    
    /**
     * 获取语言特定的注释符号
     */
    fun getCommentSymbol(): String {
        return when (this) {
            CSHARP -> "//"
            else -> "//"
        }
    }
    
    /**
     * 获取语言特定的字符串转义方法
     */
    fun getStringEscapeMethod(): (String) -> String {
        return when (this) {
            CSHARP -> { str -> str.replace("\"", "\\\"") }
            else -> { str -> str.replace("\"", "\\\"") }
        }
    }
}
