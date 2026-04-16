package com.github.xucux.ysql.ui

import com.github.xucux.ysql.models.StringBufferResult
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
 * StringBuffer结果展示对话框
 * 显示生成的StringBuffer代码结果
 */
class StringBufferResultDialog(
    private val project: Project,
    private val result: StringBufferResult
) : DialogWrapper(project) {
    
    private val codeTextArea = JBTextArea(20, 80)
    private val statisticsTextArea = JBTextArea(8, 80)
    
    init {
        title = msg("dialog.string.buffer.result.title")
        init()
        
        // 设置代码文本区域
        codeTextArea.text = result.generatedCode
        codeTextArea.isEditable = false
        codeTextArea.lineWrap = true
        codeTextArea.wrapStyleWord = true
        codeTextArea.font = java.awt.Font("Consolas", java.awt.Font.PLAIN, 12)
        
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
        
        // 代码标签页
        val codePanel = JPanel(BorderLayout())
        codePanel.add(JBScrollPane(codeTextArea), BorderLayout.CENTER)
        tabbedPane.addTab(msg("dialog.string.buffer.result.tab.code"), codePanel)
        
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
        val copyAction = object : AbstractAction(msg("dialog.action.copy.code")) {
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
        
        return arrayOf(copyAction, exportAction, cancelAction)
    }
    
    /**
     * 复制 `toClipboard`。
     */
    private fun copyToClipboard() {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(result.generatedCode)
        clipboard.setContents(selection, null)
        
        JOptionPane.showMessageDialog(
            this.contentPanel,
            msg("message.string.buffer.code.copied"),
            msg("dialog.title.success"),
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    /**
     * 导出 `toFile`。
     */
    private fun exportToFile() {
        val fileChooser = JFileChooser()
        val extension = result.language.fileExtension
        fileChooser.selectedFile = java.io.File("stringbuffer_code_${System.currentTimeMillis()}.$extension")
        fileChooser.fileFilter = object : javax.swing.filechooser.FileFilter() {
            /**
             * 处理 `accept` 逻辑。
             */
            override fun accept(f: java.io.File): Boolean {
                return f.isDirectory || f.name.lowercase().endsWith(".$extension")
            }
            
            /**
             * 获取 `description`。
             */
            override fun getDescription(): String {
                return msg("dialog.file.filter.language", result.language.displayName, extension)
            }
        }
        
        if (fileChooser.showSaveDialog(this.contentPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                val file = fileChooser.selectedFile
                file.writeText(result.generatedCode)
                
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
     * 返回国际化消息文本。
     */
    private fun msg(key: String, vararg args: Any): String = I18nUtil.getMessage(key, *args)
}
