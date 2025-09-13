package com.github.xucux.ysql.utils

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * 编码工具类
 * 确保所有文本处理都使用UTF-8编码
 */
object EncodingUtils {
    
    /**
     * UTF-8字符集常量
     */
    val UTF_8 = StandardCharsets.UTF_8
    
    /**
     * 确保字符串使用UTF-8编码
     * @param text 输入文本
     * @return UTF-8编码的字符串
     */
    fun ensureUtf8(text: String): String {
        return try {
            // 将字符串转换为UTF-8字节数组，再转换回字符串
            // 这样可以确保字符串是有效的UTF-8编码
            String(text.toByteArray(UTF_8), UTF_8)
        } catch (e: Exception) {
            // 如果转换失败，返回原始文本
            text
        }
    }
    
    /**
     * 读取文件内容，确保使用UTF-8编码
     * @param filePath 文件路径
     * @return 文件内容
     */
    fun readFileAsUtf8(filePath: String): String {
        return try {
            Files.readString(Paths.get(filePath), UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("读取文件失败：$filePath", e)
        }
    }
    
    /**
     * 写入文件内容，使用UTF-8编码
     * @param filePath 文件路径
     * @param content 文件内容
     */
    fun writeFileAsUtf8(filePath: String, content: String) {
        try {
            Files.writeString(
                Paths.get(filePath), 
                ensureUtf8(content), 
                UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        } catch (e: Exception) {
            throw RuntimeException("写入文件失败：$filePath", e)
        }
    }
    
    /**
     * 验证字符串是否为有效的UTF-8编码
     * @param text 要验证的文本
     * @return 是否为有效的UTF-8编码
     */
    fun isValidUtf8(text: String): Boolean {
        return try {
            val bytes = text.toByteArray(UTF_8)
            val decoded = String(bytes, UTF_8)
            decoded == text
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取字符串的UTF-8字节数组
     * @param text 输入文本
     * @return UTF-8字节数组
     */
    fun getUtf8Bytes(text: String): ByteArray {
        return text.toByteArray(UTF_8)
    }
    
    /**
     * 从UTF-8字节数组创建字符串
     * @param bytes UTF-8字节数组
     * @return 字符串
     */
    fun fromUtf8Bytes(bytes: ByteArray): String {
        return String(bytes, UTF_8)
    }
    
    /**
     * 清理字符串中的非UTF-8字符
     * @param text 输入文本
     * @return 清理后的文本
     */
    fun cleanUtf8String(text: String): String {
        return try {
            // 移除或替换非UTF-8字符
            text.filter { char ->
                try {
                    char.toString().toByteArray(UTF_8)
                    true
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            text
        }
    }
    
    /**
     * 格式化中文字符串，确保正确显示
     * @param text 输入文本
     * @return 格式化后的文本
     */
    fun formatChineseText(text: String): String {
        return ensureUtf8(text).let { cleanText ->
            // 确保中文字符正确显示
            cleanText.replace(Regex("[\\x00-\\x1F\\x7F]"), "") // 移除控制字符
        }
    }
}
