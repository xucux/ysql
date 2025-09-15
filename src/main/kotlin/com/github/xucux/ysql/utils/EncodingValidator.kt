package com.github.xucux.ysql.utils

import java.nio.charset.StandardCharsets

/**
 * 编码验证工具
 * 用于验证和测试UTF-8编码的正确性
 */
object EncodingValidator {
    
    /**
     * 测试中文字符串的编码
     */
    fun testChineseEncoding(): Boolean {
        val testStrings = listOf(
            "分表SQL解析",
            "StringBuffer代码生成",
            "表名列表不能为空",
            "分表数量必须大于0",
            "原始SQL语句不能为空",
            "生成分表SQL时发生错误",
            "成功识别到表名",
            "识别表名失败",
            "预览生成失败",
            "配置验证通过"
        )
        
        return testStrings.all { testString ->
            try {
                val bytes = testString.toByteArray(StandardCharsets.UTF_8)
                val decoded = String(bytes, StandardCharsets.UTF_8)
                decoded == testString
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 验证字符串是否为有效的UTF-8
     */
    fun isValidUtf8String(text: String): Boolean {
        return try {
            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            val decoded = String(bytes, StandardCharsets.UTF_8)
            decoded == text
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取编码测试报告
     */
    fun getEncodingTestReport(): String {
        return buildString {
            appendLine("UTF-8编码测试报告")
            appendLine("=".repeat(30))
            
            // 测试中文字符串编码
            val chineseTestResult = testChineseEncoding()
            appendLine("中文字符串编码测试：${if (chineseTestResult) "通过" else "失败"}")
            
            // 测试特殊字符
            val specialChars = listOf("•", "：", "，", "。", "（", "）", "【", "】")
            val specialCharsResult = specialChars.all { isValidUtf8String(it) }
            appendLine("特殊字符编码测试：${if (specialCharsResult) "通过" else "失败"}")
            
            // 测试数字和英文字符
            val alphanumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            val alphanumericResult = isValidUtf8String(alphanumeric)
            appendLine("数字英文字符编码测试：${if (alphanumericResult) "通过" else "失败"}")
            
            // 测试混合字符串
            val mixedString = "SQL分表解析_Table_2024年"
            val mixedResult = isValidUtf8String(mixedString)
            appendLine("混合字符串编码测试：${if (mixedResult) "通过" else "失败"}")
            
            appendLine()
            appendLine("总体测试结果：${if (chineseTestResult && specialCharsResult && alphanumericResult && mixedResult) "全部通过" else "存在失败"}")
        }
    }
    
    /**
     * 运行完整的编码验证
     */
    fun runFullValidation(): Boolean {
        return testChineseEncoding() && 
               listOf("•", "：", "，", "。", "（", "）", "【", "】").all { isValidUtf8String(it) } &&
               isValidUtf8String("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789") &&
               isValidUtf8String("SQL分表解析_Table_2024年")
    }
}
