package com.github.xucux.ysql.actions

import com.github.xucux.ysql.models.StringBufferConfig
import com.github.xucux.ysql.services.StringBufferService
import com.github.xucux.ysql.ui.StringBufferConfigDialog
import com.github.xucux.ysql.ui.StringBufferResultDialog
import com.github.xucux.ysql.utils.I18nUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange

/**
 * StringBuffer代码生成Action
 * 主入口Action，处理用户的StringBuffer代码生成请求
 */
class StringBufferAction : AnAction(
    I18nUtil.getMessage("action.generate.string.buffer.text"),
    I18nUtil.getMessage("action.generate.string.buffer.description"),
    null
), DumbAware {
    
    /**
     * 执行当前动作。
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR)
        val document = editor?.document
        
        // 获取选中的文本或当前行的文本
        val selectedText = if (editor != null && editor.selectionModel.hasSelection()) {
            editor.selectionModel.selectedText ?: ""
        } else if (document != null) {
            val caretModel = editor.caretModel
            val lineNumber = caretModel.logicalPosition.line
            val startOffset = document.getLineStartOffset(lineNumber)
            val endOffset = document.getLineEndOffset(lineNumber)
            document.getText(TextRange(startOffset, endOffset))
        } else {
            ""
        }
        
        // 创建配置对话框
        val configDialog = StringBufferConfigDialog(project, selectedText)
        
        if (configDialog.showAndGet()) {
            val config = configDialog.getConfig()
            
            // 在后台线程中生成代码
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    val stringBufferService = ApplicationManager.getApplication().getService(StringBufferService::class.java)
                    val result = stringBufferService.generateStringBufferCode(config)
                    
                    // 在UI线程中显示结果
                    ApplicationManager.getApplication().invokeLater {
                        if (result.success) {
                            val resultDialog = StringBufferResultDialog(project, result)
                            resultDialog.show()
                        } else {
                            Messages.showErrorDialog(
                                project,
                                I18nUtil.getMessage("message.string.buffer.generate.failed", result.errorMessage ?: ""),
                                I18nUtil.getMessage("dialog.title.error")
                            )
                        }
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            I18nUtil.getMessage("message.string.buffer.generate.exception", e.message ?: ""),
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
        event.presentation.text = I18nUtil.getMessage("action.generate.string.buffer.text")
        event.presentation.description = I18nUtil.getMessage("action.generate.string.buffer.description")
        // 检查是否有编辑器可用
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isEnabledAndVisible = editor != null
    }
}
