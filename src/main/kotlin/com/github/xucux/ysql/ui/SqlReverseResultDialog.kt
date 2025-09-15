package com.github.xucux.ysql.ui

import com.github.xucux.ysql.models.SqlReverseResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*

/**
 * SQL反向解析结果对话框
 * 显示从StringBuffer/StringBuilder代码中解析出的SQL语句
 */
class SqlReverseResultDialog(
    project: Project,
    private val result: SqlReverseResult
) : DialogWrapper(project) {
    
    private lateinit var sqlTextArea: JBTextArea
    private lateinit var fragmentsTextArea: JBTextArea
    private lateinit var statisticsTextArea: JBTextArea
    private lateinit var tabbedPane: JBTabbedPane
    
    init {
        title = "SQL反向解析结果"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // 创建选项卡面板
        tabbedPane = JBTabbedPane()
        
        // SQL语句选项卡
        createSqlTab()
        
        // SQL片段选项卡
        createFragmentsTab()
        
        // 统计信息选项卡
        createStatisticsTab()
        
        panel.add(tabbedPane, BorderLayout.CENTER)
        
        // 设置对话框大小
        panel.preferredSize = Dimension(800, 600)
        
        return panel
    }
    
    /**
     * 创建SQL语句选项卡
     */
    private fun createSqlTab() {
        val sqlPanel = JPanel(BorderLayout())
        
        // 创建SQL文本区域
        sqlTextArea = JBTextArea()
        sqlTextArea.text = result.extractedSql
        sqlTextArea.isEditable = false
        sqlTextArea.lineWrap = true
        sqlTextArea.wrapStyleWord = true
        sqlTextArea.font = sqlTextArea.font.deriveFont(12f)
        
        // 添加滚动面板
        val sqlScrollPane = JBScrollPane(sqlTextArea)
        sqlScrollPane.preferredSize = Dimension(750, 400)
        
        // 创建按钮面板
        val buttonPanel = createButtonPanel()
        
        sqlPanel.add(sqlScrollPane, BorderLayout.CENTER)
        sqlPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        tabbedPane.addTab("提取的SQL语句", sqlPanel)
    }
    
    /**
     * 创建SQL片段选项卡
     */
    private fun createFragmentsTab() {
        val fragmentsPanel = JPanel(BorderLayout())
        
        // 创建片段文本区域
        fragmentsTextArea = JBTextArea()
        fragmentsTextArea.text = result.getSqlFragmentsDetail()
        fragmentsTextArea.isEditable = false
        fragmentsTextArea.lineWrap = true
        fragmentsTextArea.wrapStyleWord = true
        fragmentsTextArea.font = fragmentsTextArea.font.deriveFont(12f)
        
        // 添加滚动面板
        val fragmentsScrollPane = JBScrollPane(fragmentsTextArea)
        fragmentsScrollPane.preferredSize = Dimension(750, 400)
        
        fragmentsPanel.add(fragmentsScrollPane, BorderLayout.CENTER)
        
        tabbedPane.addTab("SQL片段详情", fragmentsPanel)
    }
    
    /**
     * 创建统计信息选项卡
     */
    private fun createStatisticsTab() {
        val statisticsPanel = JPanel(BorderLayout())
        
        // 创建统计信息文本区域
        statisticsTextArea = JBTextArea()
        statisticsTextArea.text = result.getStatistics()
        statisticsTextArea.isEditable = false
        statisticsTextArea.lineWrap = true
        statisticsTextArea.wrapStyleWord = true
        statisticsTextArea.font = statisticsTextArea.font.deriveFont(12f)
        
        // 添加滚动面板
        val statisticsScrollPane = JBScrollPane(statisticsTextArea)
        statisticsScrollPane.preferredSize = Dimension(750, 400)
        
        statisticsPanel.add(statisticsScrollPane, BorderLayout.CENTER)
        
        tabbedPane.addTab("解析统计", statisticsPanel)
    }
    
    /**
     * 创建按钮面板
     */
    private fun createButtonPanel(): JPanel {
        val buttonPanel = JPanel()
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.X_AXIS)
        
        // 复制SQL按钮
        val copySqlButton = JButton("复制SQL语句")
        copySqlButton.addActionListener {
            copyToClipboard(result.extractedSql)
        }
        
        // 复制所有内容按钮
        val copyAllButton = JButton("复制所有内容")
        copyAllButton.addActionListener {
            copyToClipboard(result.getFormattedResult())
        }
        
        // 格式化SQL按钮
        val formatSqlButton = JButton("格式化SQL")
        formatSqlButton.addActionListener {
            formatSql()
        }
        
        buttonPanel.add(Box.createHorizontalGlue())
        buttonPanel.add(copySqlButton)
        buttonPanel.add(Box.createHorizontalStrut(10))
        buttonPanel.add(copyAllButton)
        buttonPanel.add(Box.createHorizontalStrut(10))
        buttonPanel.add(formatSqlButton)
        buttonPanel.add(Box.createHorizontalGlue())
        
        return buttonPanel
    }
    
    /**
     * 复制文本到剪贴板
     */
    private fun copyToClipboard(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val stringSelection = java.awt.datatransfer.StringSelection(text)
        clipboard.setContents(stringSelection, null)
        
        JOptionPane.showMessageDialog(
            this.contentPanel,
            "内容已复制到剪贴板",
            "提示",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    /**
     * 格式化SQL语句
     */
    private fun formatSql() {
        val formattedSql = result.extractedSql
            .replace(Regex("\\s+"), " ") // 合并多个空格
            .replace(Regex("\\s*,\\s*"), ", ") // 格式化逗号
            .replace(Regex("\\s*\\(\\s*"), " (") // 格式化左括号
            .replace(Regex("\\s*\\)\\s*"), ") ") // 格式化右括号
            .trim()
        
        sqlTextArea.text = formattedSql
        
        JOptionPane.showMessageDialog(
            this.contentPanel,
            "SQL语句已格式化",
            "提示",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    override fun createActions(): Array<Action> {
        return arrayOf(okAction)
    }
}
