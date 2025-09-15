package com.github.xucux.ysql.ui

import com.github.xucux.ysql.models.BatchDeleteResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
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
        title = "批量删除存储过程生成结果"
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
    
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        
        // 创建标签页
        val tabbedPane = JTabbedPane()
        
        // 存储过程标签页
        val procedurePanel = JPanel(BorderLayout())
        procedurePanel.add(JBScrollPane(procedureTextArea), BorderLayout.CENTER)
        tabbedPane.addTab("生成存储过程", procedurePanel)
        
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
        val copyAction = object : AbstractAction("复制存储过程") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                copyToClipboard()
            }
        }
        
        val exportAction = object : AbstractAction("导出到文件") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                exportToFile()
            }
        }
        
        val callExampleAction = object : AbstractAction("生成调用示例") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                showCallExample()
            }
        }
        
        return arrayOf(copyAction, exportAction, callExampleAction, cancelAction)
    }
    
    private fun copyToClipboard() {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(result.generatedProcedure)
        clipboard.setContents(selection, null)
        
        JOptionPane.showMessageDialog(
            this.contentPanel,
            "批量删除存储过程已复制到剪贴板",
            "复制成功",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    private fun exportToFile() {
        val fileChooser = JFileChooser()
        fileChooser.selectedFile = java.io.File("${result.procedureName}_${System.currentTimeMillis()}.sql")
        fileChooser.fileFilter = object : javax.swing.filechooser.FileFilter() {
            override fun accept(f: java.io.File): Boolean {
                return f.isDirectory || f.name.lowercase().endsWith(".sql")
            }
            
            override fun getDescription(): String {
                return "SQL文件 (*.sql)"
            }
        }
        
        if (fileChooser.showSaveDialog(this.contentPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                val file = fileChooser.selectedFile
                file.writeText(result.generatedProcedure)
                
                JOptionPane.showMessageDialog(
                    this.contentPanel,
                    "批量删除存储过程已导出到：${file.absolutePath}",
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
    
    private fun showCallExample() {
        val callExample = generateCallExample()
        
        val exampleDialog = object : DialogWrapper(project) {
            init {
                title = "存储过程调用示例"
                init()
            }
            
            override fun createCenterPanel(): JComponent {
                val textArea = JBTextArea(10, 60)
                textArea.text = callExample
                textArea.isEditable = false
                textArea.font = java.awt.Font("Consolas", java.awt.Font.PLAIN, 12)
                textArea.lineWrap = true
                textArea.wrapStyleWord = true
                
                return JBScrollPane(textArea)
            }
            
            override fun createActions(): Array<Action> {
                val copyExampleAction = object : AbstractAction("复制示例") {
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
    
    private fun generateCallExample(): String {
        return buildString {
            appendLine("-- 批量删除存储过程调用示例")
            appendLine("-- 存储过程名称：${result.procedureName}")
            appendLine("-- 主表名：${result.mainTableName}")
            appendLine()
            appendLine("-- 基本调用示例：")
            appendLine("CALL ${result.procedureName}(1000, '2023-01-01 00:00:00', 0);")
            appendLine()
            appendLine("-- 参数说明：")
            appendLine("-- 第一个参数：每次删除的行数（建议1000-5000）")
            appendLine("-- 第二个参数：删除截至时间（格式：'YYYY-MM-DD HH:MM:SS'）")
            appendLine("-- 第三个参数：起始主键值（通常为0）")
            appendLine()
            appendLine("-- 其他调用示例：")
            appendLine("-- 删除2022年之前的数据，每次删除5000行：")
            appendLine("CALL ${result.procedureName}(5000, '2022-01-01 00:00:00', 0);")
            appendLine()
            appendLine("-- 删除2021年之前的数据，每次删除2000行：")
            appendLine("CALL ${result.procedureName}(2000, '2021-01-01 00:00:00', 0);")
            appendLine()
            appendLine("-- 注意事项：")
            appendLine("-- 1. 建议在业务低峰期执行")
            appendLine("-- 2. 执行前请备份重要数据")
            appendLine("-- 3. 可以根据服务器性能调整每次删除的行数")
            appendLine("-- 4. 执行过程中会显示删除日志")
        }
    }
}
