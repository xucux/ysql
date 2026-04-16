package com.github.xucux.ysql.actions

import com.github.xucux.ysql.models.BatchDeleteConfig
import com.github.xucux.ysql.services.BatchDeleteService
import com.github.xucux.ysql.ui.BatchDeleteConfigDialog
import com.github.xucux.ysql.ui.BatchDeleteResultDialog
import com.github.xucux.ysql.utils.I18nUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange

/**
 * 批量删除存储过程生成Action
 * 主入口Action，处理用户的批量删除存储过程生成请求
 */
class BatchDeleteAction : AnAction(
    I18nUtil.getMessage("action.batch.delete.procedure.text"),
    I18nUtil.getMessage("action.batch.delete.procedure.description"),
    null
), DumbAware {
    
    /**
     * 执行当前动作。
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        
        // 创建配置对话框
        val configDialog = BatchDeleteConfigDialog(project)
        
        if (configDialog.showAndGet()) {
            val config = configDialog.getConfig()
            
            // 在后台线程中生成存储过程
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    val batchDeleteService = ApplicationManager.getApplication().getService(BatchDeleteService::class.java)
                    val result = batchDeleteService.generateBatchDeleteProcedure(config)
                    
                    // 在UI线程中显示结果
                    ApplicationManager.getApplication().invokeLater {
                        if (result.success) {
                            val resultDialog = BatchDeleteResultDialog(project, result)
                            resultDialog.show()
                        } else {
                            Messages.showErrorDialog(
                                project,
                                I18nUtil.getMessage("message.batch.delete.generate.failed", result.errorMessage ?: ""),
                                I18nUtil.getMessage("dialog.title.error")
                            )
                        }
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            I18nUtil.getMessage("message.batch.delete.generate.exception", e.message ?: ""),
                            I18nUtil.getMessage("dialog.title.exception")
                        )
                    }
                }
            }
        }
    }
    
    /**
     * 更新当前动作的展示状态。
     */
    override fun update(event: AnActionEvent) {
        event.presentation.text = I18nUtil.getMessage("action.batch.delete.procedure.text")
        event.presentation.description = I18nUtil.getMessage("action.batch.delete.procedure.description")
        // 检查是否有项目可用
        val project = event.project
        event.presentation.isEnabledAndVisible = project != null
    }
}
