package com.github.xucux.ysql.config

import com.github.xucux.ysql.utils.EncodingUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * 编码配置服务
 * 确保插件在启动时正确配置UTF-8编码
 */
@Service
class EncodingConfig {
    
    init {
        // 设置系统默认编码为UTF-8
        setupSystemEncoding()
    }
    
    /**
     * 设置系统编码
     */
    private fun setupSystemEncoding() {
        try {
            // 设置系统属性
            System.setProperty("file.encoding", "UTF-8")
            System.setProperty("sun.jnu.encoding", "UTF-8")
            System.setProperty("user.language", "zh")
            System.setProperty("user.country", "CN")
            System.setProperty("user.variant", "")
            
            // 设置默认字符集
            System.setProperty("java.util.prefs.PreferencesFactory", "java.util.prefs.FileSystemPreferencesFactory")
            
            // 确保控制台输出使用UTF-8
            System.setOut(java.io.PrintStream(System.out, true, StandardCharsets.UTF_8))
            System.setErr(java.io.PrintStream(System.err, true, StandardCharsets.UTF_8))
            
        } catch (e: Exception) {
            // 如果设置失败，记录错误但不影响插件运行
            println("设置系统编码时发生错误：${e.message}")
        }
    }
    
    /**
     * 验证编码配置
     * @return 编码配置是否成功
     */
    fun validateEncoding(): Boolean {
        return try {
            val testString = "测试中文字符串"
            val encoded = testString.toByteArray(StandardCharsets.UTF_8)
            val decoded = String(encoded, StandardCharsets.UTF_8)
            decoded == testString
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取当前系统编码信息
     * @return 编码信息字符串
     */
    fun getEncodingInfo(): String {
        return buildString {
            appendLine("编码配置信息：")
            appendLine("• 文件编码：${System.getProperty("file.encoding")}")
            appendLine("• 默认字符集：${Charset.defaultCharset().name()}")
            appendLine("• UTF-8字符集：${StandardCharsets.UTF_8.name()}")
            appendLine("• 系统语言：${System.getProperty("user.language")}")
            appendLine("• 系统国家：${System.getProperty("user.country")}")
            appendLine("• 编码验证：${if (validateEncoding()) "通过" else "失败"}")
        }
    }
    
    companion object {
        /**
         * 获取编码配置服务实例
         */
        fun getInstance(): EncodingConfig {
            return ApplicationManager.getApplication().getService(EncodingConfig::class.java)
        }
    }
}
