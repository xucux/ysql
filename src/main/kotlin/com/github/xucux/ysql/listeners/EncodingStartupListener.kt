package com.github.xucux.ysql.listeners

import com.github.xucux.ysql.config.EncodingConfig
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

/**
 * 编码启动监听器
 * 在插件启动时确保UTF-8编码正确配置
 */
class EncodingStartupListener : StartupActivity {
    private val logger = Logger.getInstance(EncodingStartupListener::class.java)
    
    /**
     * 处理 `runActivity` 逻辑。
     */
    override fun runActivity(project: Project) {
        // 在后台线程中初始化编码配置
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // 获取编码配置服务
                val encodingConfig = EncodingConfig.getInstance()
                
                // 验证编码配置
                if (encodingConfig.validateEncoding()) {
                    logger.info("UTF-8 encoding configuration validated successfully")
                } else {
                    logger.warn("UTF-8 encoding configuration validation failed")
                }
                
                // 输出编码信息（仅在调试模式下）
                if (isDebugMode()) {
                    logger.info(encodingConfig.getEncodingInfo())
                }
                
            } catch (e: Exception) {
                logger.warn("Failed to initialize encoding configuration", e)
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
