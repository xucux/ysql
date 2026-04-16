package com.github.xucux.ysql.actions

import com.github.xucux.ysql.models.CodeLanguage
import com.github.xucux.ysql.services.StringBufferService
import com.github.xucux.ysql.ui.SqlReverseResultDialog
import com.github.xucux.ysql.utils.I18nUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange

/**
 * SQL反向解析Action
 * 从StringBuffer/StringBuilder代码中提取SQL语句
 */
class SqlReverseAction : AnAction(
    I18nUtil.getMessage("action.reverse.parse.sql.text"),
    I18nUtil.getMessage("action.reverse.parse.sql.description"),
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
        
        // 检查是否包含StringBuffer/StringBuilder
        val stringBufferService = ApplicationManager.getApplication().getService(StringBufferService::class.java)
        if (!stringBufferService.containsStringBuffer(selectedText)) {
            Messages.showWarningDialog(
                project,
                I18nUtil.getMessage("message.reverse.parse.no.buffer.selected"),
                I18nUtil.getMessage("dialog.title.warning")
            )
            return
        }
        
        // 在后台线程中解析SQL
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val result = stringBufferService.reverseParseSql(selectedText)
                
                // 在UI线程中显示结果
                ApplicationManager.getApplication().invokeLater {
                    if (result.success) {
                        val resultDialog = SqlReverseResultDialog(project, result)
                        resultDialog.show()
                    } else {
                        Messages.showErrorDialog(
                            project,
                            I18nUtil.getMessage("message.reverse.parse.failed", result.errorMessage ?: ""),
                            I18nUtil.getMessage("dialog.title.error")
                        )
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        I18nUtil.getMessage("message.reverse.parse.exception", e.message ?: ""),
                        I18nUtil.getMessage("dialog.title.exception")
                    )
                }
            }
        }
    }
    
    /**
     * 更新当前动作的展示状态。
     */
    override fun update(event: AnActionEvent) {
        event.presentation.text = I18nUtil.getMessage("action.reverse.parse.sql.text")
        event.presentation.description = I18nUtil.getMessage("action.reverse.parse.sql.description")
        // 检查是否有编辑器可用
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isEnabledAndVisible = editor != null
    }
}
