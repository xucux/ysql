package com.github.xucux.ysql.ui

import com.github.xucux.ysql.models.StringBufferResult
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
        title = "StringBuffer代码生成结果"
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
    
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        
        // 创建标签页
        val tabbedPane = JBTabbedPane()
        
        // 代码标签页
        val codePanel = JPanel(BorderLayout())
        codePanel.add(JBScrollPane(codeTextArea), BorderLayout.CENTER)
        tabbedPane.addTab("生成代码", codePanel)
        
        // 统计信息标签页
        val statisticsPanel = JPanel(BorderLayout())
        statisticsPanel.add(JBScrollPane(statisticsTextArea), BorderLayout.CENTER)
        tabbedPane.addTab("统计信息", statisticsPanel)
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER)
        
        // 设置面板大小
        mainPanel.preferredSize = Dimension(800, 600)
        
        return mainPanel
    }
    
    override fun createActions(): Array<Action> {
        val copyAction = object : AbstractAction("复制代码") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                copyToClipboard()
            }
        }
        
        val exportAction = object : AbstractAction("导出到文件") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                exportToFile()
            }
        }
        
        return arrayOf(copyAction, exportAction, cancelAction)
    }
    
    private fun copyToClipboard() {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(result.generatedCode)
        clipboard.setContents(selection, null)
        
        JOptionPane.showMessageDialog(
            this.contentPanel,
            "StringBuffer代码已复制到剪贴板",
            "复制成功",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    private fun exportToFile() {
        val fileChooser = JFileChooser()
        val extension = result.language.fileExtension
        fileChooser.selectedFile = java.io.File("stringbuffer_code_${System.currentTimeMillis()}.$extension")
        fileChooser.fileFilter = object : javax.swing.filechooser.FileFilter() {
            override fun accept(f: java.io.File): Boolean {
                return f.isDirectory || f.name.lowercase().endsWith(".$extension")
            }
            
            override fun getDescription(): String {
                return "${result.language.displayName}文件 (*.$extension)"
            }
        }
        
        if (fileChooser.showSaveDialog(this.contentPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                val file = fileChooser.selectedFile
                file.writeText(result.generatedCode)
                
                JOptionPane.showMessageDialog(
                    this.contentPanel,
                    "StringBuffer代码已导出到：${file.absolutePath}",
                    "导出成功",
                    JOptionPane.INFORMATION_MESSAGE
                )
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    this.contentPanel,
                    "导出失败：${e.message}",
                    "导出错误",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }
}
