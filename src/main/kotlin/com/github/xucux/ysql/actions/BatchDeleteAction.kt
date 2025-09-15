package com.github.xucux.ysql.actions

import com.github.xucux.ysql.models.BatchDeleteConfig
import com.github.xucux.ysql.services.BatchDeleteService
import com.github.xucux.ysql.ui.BatchDeleteConfigDialog
import com.github.xucux.ysql.ui.BatchDeleteResultDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange

/**
 * 批量删除存储过程生成Action
 * 主入口Action，处理用户的批量删除存储过程生成请求
 */
class BatchDeleteAction : AnAction("生成批量删除存储过程", "根据配置生成批量删除历史数据的存储过程", null), DumbAware {
    
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        
        // 创建配置对话框
        val configDialog = BatchDeleteConfigDialog(project)
        
        if (configDialog.showAndGet()) {
            val config = configDialog.getConfig()
            
            // 在后台线程中生成存储过程
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    val batchDeleteService = ServiceManager.getService(BatchDeleteService::class.java)
                    val result = batchDeleteService.generateBatchDeleteProcedure(config)
                    
                    // 在UI线程中显示结果
                    ApplicationManager.getApplication().invokeLater {
                        if (result.success) {
                            val resultDialog = BatchDeleteResultDialog(project, result)
                            resultDialog.show()
                        } else {
                            Messages.showErrorDialog(
                                project,
                                "生成批量删除存储过程失败：${result.errorMessage}",
                                "错误"
                            )
                        }
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "生成批量删除存储过程时发生异常：${e.message}",
                            "异常"
                        )
                    }
                }
            }
        }
    }
    
    override fun update(event: AnActionEvent) {
        // 检查是否有项目可用
        val project = event.project
        event.presentation.isEnabledAndVisible = project != null
    }
}
