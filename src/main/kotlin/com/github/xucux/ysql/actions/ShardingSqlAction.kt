package com.github.xucux.ysql.actions

import com.github.xucux.ysql.models.ShardingConfig
import com.github.xucux.ysql.models.SuffixType
import com.github.xucux.ysql.services.SqlShardingService
import com.github.xucux.ysql.services.TableNameExtractorService
import com.github.xucux.ysql.ui.ShardingConfigDialog
import com.github.xucux.ysql.ui.ShardingResultDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange

/**
 * 分表SQL解析Action
 * 主入口Action，处理用户的分表SQL生成请求
 */
class ShardingSqlAction : AnAction("生成分表SQL", "从单个SQL语句生成多个分表SQL语句", null), DumbAware {
    
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
        val configDialog = ShardingConfigDialog(project, selectedText)
        
        if (configDialog.showAndGet()) {
            val config = configDialog.getConfig()
            
            // 在后台线程中生成分表SQL
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    val shardingService = ApplicationManager.getApplication().getService(SqlShardingService::class.java)
                    val result = shardingService.generateShardingSql(config)
                    
                    // 在UI线程中显示结果
                    ApplicationManager.getApplication().invokeLater {
                        if (result.success) {
                            val resultDialog = ShardingResultDialog(project, result)
                            resultDialog.show()
                        } else {
                            Messages.showErrorDialog(
                                project,
                                "生成分表SQL失败：${result.errorMessage}",
                                "错误"
                            )
                        }
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "生成分表SQL时发生异常：${e.message}",
                            "异常"
                        )
                    }
                }
            }
        }
    }
    
    override fun update(event: AnActionEvent) {
        // 检查是否有编辑器可用
        val editor = event.getData(CommonDataKeys.EDITOR)
        event.presentation.isEnabledAndVisible = editor != null
    }
}
