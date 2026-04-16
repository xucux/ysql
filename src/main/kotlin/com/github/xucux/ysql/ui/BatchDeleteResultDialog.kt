package com.github.xucux.ysql.ui

import com.github.xucux.ysql.models.BatchDeleteResult
import com.github.xucux.ysql.utils.I18nUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.*

/**
 * 批量删除存储过程结果展示对话框
 * 显示生成的批量删除存储过程结果
 */
class BatchDeleteResultDialog(
    private val project: Project,
    private val result: BatchDeleteResult
) : DialogWrapper(project) {
    
    private val procedureTextArea = JBTextArea(20, 80)
    private val statisticsTextArea = JBTextArea(8, 80)
    
    init {
        title = msg("dialog.batch.delete.result.title")
        init()
        
        // 设置存储过程文本区域
        procedureTextArea.text = result.generatedProcedure
        procedureTextArea.isEditable = false
        procedureTextArea.lineWrap = true
        procedureTextArea.wrapStyleWord = true
        procedureTextArea.font = java.awt.Font("Consolas", java.awt.Font.PLAIN, 12)
        
        // 设置统计信息文本区域
        statisticsTextArea.text = result.getStatistics()
        statisticsTextArea.isEditable = false
        statisticsTextArea.lineWrap = true
        statisticsTextArea.wrapStyleWord = true
        statisticsTextArea.font = java.awt.Font("Dialog", java.awt.Font.PLAIN, 11)
    }
    
    /**
     * 创建对话框主体面板。
     */
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        
        // 创建标签页
        val tabbedPane = JBTabbedPane()
        
        // 存储过程标签页
        val procedurePanel = JPanel(BorderLayout())
        procedurePanel.add(JBScrollPane(procedureTextArea), BorderLayout.CENTER)
        tabbedPane.addTab(msg("dialog.batch.delete.result.tab.procedure"), procedurePanel)
        
        // 统计信息标签页
        val statisticsPanel = JPanel(BorderLayout())
        statisticsPanel.add(JBScrollPane(statisticsTextArea), BorderLayout.CENTER)
        tabbedPane.addTab(msg("dialog.result.tab.statistics"), statisticsPanel)
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER)
        
        // 设置面板大小
        mainPanel.preferredSize = Dimension(800, 600)
        
        return mainPanel
    }
    
    /**
     * 创建当前对话框的操作列表。
     */
    override fun createActions(): Array<Action> {
        val copyAction = object : AbstractAction(msg("dialog.batch.delete.result.action.copy.procedure")) {
            /**
             * 执行当前动作。
             */
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                copyToClipboard()
            }
        }
        
        val exportAction = object : AbstractAction(msg("dialog.action.export.file")) {
            /**
             * 执行当前动作。
             */
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                exportToFile()
            }
        }
        
        val callExampleAction = object : AbstractAction(msg("dialog.batch.delete.result.action.call.example")) {
            /**
             * 执行当前动作。
             */
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                showCallExample()
            }
        }
        
        return arrayOf(copyAction, exportAction, callExampleAction, cancelAction)
    }
    
    /**
     * 复制 `toClipboard`。
     */
    private fun copyToClipboard() {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(result.generatedProcedure)
        clipboard.setContents(selection, null)
        
        JOptionPane.showMessageDialog(
            this.contentPanel,
            msg("message.batch.delete.procedure.copied"),
            msg("dialog.title.success"),
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    /**
     * 导出 `toFile`。
     */
    private fun exportToFile() {
        val fileChooser = JFileChooser()
        fileChooser.selectedFile = java.io.File("${result.procedureName}_${System.currentTimeMillis()}.sql")
        fileChooser.fileFilter = object : javax.swing.filechooser.FileFilter() {
            /**
             * 处理 `accept` 逻辑。
             */
            override fun accept(f: java.io.File): Boolean {
                return f.isDirectory || f.name.lowercase().endsWith(".sql")
            }
            
            /**
             * 获取 `description`。
             */
            override fun getDescription(): String {
                return msg("dialog.file.filter.sql")
            }
        }
        
        if (fileChooser.showSaveDialog(this.contentPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                val file = fileChooser.selectedFile
                file.writeText(result.generatedProcedure)
                
                JOptionPane.showMessageDialog(
                    this.contentPanel,
                    msg("message.file.export.success", file.absolutePath),
                    msg("dialog.title.success"),
                    JOptionPane.INFORMATION_MESSAGE
                )
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    this.contentPanel,
                    msg("message.file.export.failed", e.message ?: ""),
                    msg("dialog.title.error"),
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }
    
    /**
     * 展示 `callExample`。
     */
    private fun showCallExample() {
        val callExample = generateCallExample()
        
        val exampleDialog = object : DialogWrapper(project) {
            init {
                title = msg("dialog.batch.delete.result.call.example.title")
                init()
            }
            
            /**
             * 创建对话框主体面板。
             */
            override fun createCenterPanel(): JComponent {
                val textArea = JBTextArea(10, 60)
                textArea.text = callExample
                textArea.isEditable = false
                textArea.font = java.awt.Font("Consolas", java.awt.Font.PLAIN, 12)
                textArea.lineWrap = true
                textArea.wrapStyleWord = true
                
                return JBScrollPane(textArea)
            }
            
            /**
             * 创建当前对话框的操作列表。
             */
            override fun createActions(): Array<Action> {
                val copyExampleAction = object : AbstractAction(msg("dialog.batch.delete.result.action.copy.example")) {
                    /**
                     * 执行当前动作。
                     */
                    override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        val selection = StringSelection(callExample)
                        clipboard.setContents(selection, null)
                        
//                        JOptionPane.showMessageDialog(
//                                this.contentPanel,
//                            "调用示例已复制到剪贴板",
//                            "复制成功",
//                            JOptionPane.INFORMATION_MESSAGE
//                        )
                    }
                }
                
                return arrayOf(copyExampleAction, cancelAction)
            }
        }
        
        exampleDialog.show()
    }
    
    /**
     * 生成 `callExample`。
     */
    private fun generateCallExample(): String {
        return buildString {
            appendLine(msg("batch.delete.result.example.header"))
            appendLine(msg("batch.delete.result.example.procedure.name", result.procedureName))
            appendLine(msg("batch.delete.result.example.main.table", result.mainTableName))
            appendLine()
            appendLine(msg("batch.delete.result.example.basic.call"))
            appendLine("CALL ${result.procedureName}(1000, '2023-01-01 00:00:00', 0);")
            appendLine()
            appendLine(msg("batch.delete.result.example.parameter.header"))
            appendLine(msg("batch.delete.result.example.parameter.first"))
            appendLine(msg("batch.delete.result.example.parameter.second"))
            appendLine(msg("batch.delete.result.example.parameter.third"))
            appendLine()
            appendLine(msg("batch.delete.result.example.other.call"))
            appendLine(msg("batch.delete.result.example.other.call.2022"))
            appendLine("CALL ${result.procedureName}(5000, '2022-01-01 00:00:00', 0);")
            appendLine()
            appendLine(msg("batch.delete.result.example.other.call.2021"))
            appendLine("CALL ${result.procedureName}(2000, '2021-01-01 00:00:00', 0);")
            appendLine()
            appendLine(msg("batch.delete.result.example.notice.header"))
            appendLine(msg("batch.delete.result.example.notice.1"))
            appendLine(msg("batch.delete.result.example.notice.2"))
            appendLine(msg("batch.delete.result.example.notice.3"))
            appendLine(msg("batch.delete.result.example.notice.4"))
        }
    }
    /**
     * 返回国际化消息文本。
     */
    private fun msg(key: String, vararg args: Any): String = I18nUtil.getMessage(key, *args)
}
