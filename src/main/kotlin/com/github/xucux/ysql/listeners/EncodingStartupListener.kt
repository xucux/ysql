package com.github.xucux.ysql.listeners

import com.github.xucux.ysql.config.EncodingConfig
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

/**
 * 编码启动监听器
 * 在插件启动时确保UTF-8编码正确配置
 */
class EncodingStartupListener : StartupActivity {
    
    override fun runActivity(project: Project) {
        // 在后台线程中初始化编码配置
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // 获取编码配置服务
                val encodingConfig = EncodingConfig.getInstance()
                
                // 验证编码配置
                if (encodingConfig.validateEncoding()) {
                    println("UTF-8编码配置验证成功")
                } else {
                    println("UTF-8编码配置验证失败")
                }
                
                // 输出编码信息（仅在调试模式下）
                if (isDebugMode()) {
                    println(encodingConfig.getEncodingInfo())
                }
                
            } catch (e: Exception) {
                println("初始化编码配置时发生错误：${e.message}")
            }
        }
    }
    
    /**
     * 检查是否为调试模式
     */
    private fun isDebugMode(): Boolean {
        return System.getProperty("ysql.debug", "false").toBoolean()
    }
}
