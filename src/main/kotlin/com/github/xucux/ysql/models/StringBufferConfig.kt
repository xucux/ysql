package com.github.xucux.ysql.models

import com.github.xucux.ysql.utils.I18nUtil

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
    val addComments: Boolean = false,
    
    /**
     * 是否格式化代码
     */
    val formatCode: Boolean = false
)

/**
 * 支持的编程语言枚举
 */
enum class CodeLanguage(
    private val displayNameKey: String,
    val bufferClass: String,
    val toStringMethod: String,
    val fileExtension: String
) {
    JAVA("code.language.java", "StringBuffer", "toString()", "java"),
    CSHARP("code.language.csharp", "StringBuilder", "ToString()", "cs"),
    KOTLIN("code.language.kotlin", "StringBuilder", "toString()", "kt"),
    SCALA("code.language.scala", "StringBuilder", "toString()", "scala"),
    GROOVY("code.language.groovy", "StringBuilder", "toString()", "groovy");

    val displayName: String
        get() = I18nUtil.getMessage(displayNameKey)

    /**
     * 返回当前对象的显示文本。
     */
    override fun toString(): String = displayName
    
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
            CSHARP -> { str -> 
                str.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\t", "\\t")
                   .replace("\r", "\\r")
                   .replace("\b", "\\b")
            }
            else -> { str -> 
                str.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\t", "\\t")
                   .replace("\r", "\\r")
                   .replace("\b", "\\b")
            }
        }
    }
}
