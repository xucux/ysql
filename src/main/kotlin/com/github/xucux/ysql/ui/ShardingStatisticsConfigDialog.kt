package com.github.xucux.ysql.ui

import com.github.xucux.ysql.models.ShardingConfig
import com.github.xucux.ysql.utils.I18nUtil
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.openapi.ui.ComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import com.intellij.ui.table.JBTable
import javax.swing.JButton
import javax.swing.table.DefaultTableModel
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.DefaultCellEditor

/**
 * 分表统计配置弹窗
 * 用于配置分表统计的相关参数
 */
class ShardingStatisticsConfigDialog(
    private val project: com.intellij.openapi.project.Project,
    private val originalSql: String
) : DialogWrapper(project) {
    
    // 统计字段配置表格
    private val fieldStatisticsTable: JBTable
    private val tableModel: DefaultTableModel
    
    // 一键设置组件
    private val globalStatisticsTypeComboBox = ComboBox(StatisticsType.values())
    private val applyToAllButton = JButton(msg("dialog.sharding.statistics.apply.all"))
    
    // 解析出的字段列表
    private val extractedFields = mutableListOf<String>()
    
    init {
        title = msg("dialog.sharding.statistics.title")
        
        // 初始化表格模型
        tableModel = object : DefaultTableModel(arrayOf(msg("dialog.sharding.statistics.column.field"), msg("dialog.sharding.statistics.column.type")), 0) {
            /**
             * 判断是否为 `cellEditable`。
             */
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return column == 1 // 只有统计类型列可编辑
            }
        }
        
        fieldStatisticsTable = JBTable(tableModel)
        
        // 为统计类型列设置下拉编辑器
        val statisticsTypeComboBox = ComboBox(StatisticsType.values())
        fieldStatisticsTable.columnModel.getColumn(1).cellEditor = DefaultCellEditor(statisticsTypeComboBox)
        
        // 从原始SQL中解析字段
        extractFieldsFromSql()
        
        // 设置一键应用按钮事件
        applyToAllButton.addActionListener {
            applyGlobalStatisticsType()
        }
        
        // 在所有组件初始化完成后，再调用DialogWrapper的init()
        init()
    }
    
    /**
     * 创建对话框主体面板。
     */
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // 创建一键设置面板
        val globalConfigPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        globalConfigPanel.add(com.intellij.ui.components.JBLabel(msg("dialog.sharding.statistics.global.type")))
        globalConfigPanel.add(globalStatisticsTypeComboBox)
        globalConfigPanel.add(applyToAllButton)
        
        // 创建字段统计配置面板
        val fieldConfigPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("dialog.sharding.statistics.field.config"), fieldStatisticsTable)
            .panel
        
        // 组合面板
        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(globalConfigPanel, BorderLayout.NORTH)
        mainPanel.add(fieldConfigPanel, BorderLayout.CENTER)
        
        panel.add(mainPanel, BorderLayout.CENTER)
        return panel
    }
    
    /**
     * 获取字段统计配置
     */
    fun getFieldStatisticsConfig(): Map<String, StatisticsType> {
        val config = mutableMapOf<String, StatisticsType>()
        for (i in 0 until tableModel.rowCount) {
            val fieldName = tableModel.getValueAt(i, 0) as String
            val statisticsType = tableModel.getValueAt(i, 1) as StatisticsType
            config[fieldName] = statisticsType
        }
        return config
    }
    
    /**
     * 从SQL中解析字段
     */
    private fun extractFieldsFromSql() {
        val trimmedSql = originalSql.trim()
        val selectIndex = trimmedSql.indexOf("SELECT", ignoreCase = true)
        val fromIndex = trimmedSql.indexOf("FROM", ignoreCase = true)
        
        if (selectIndex == -1 || fromIndex == -1 || selectIndex >= fromIndex) {
            return
        }
        
        val selectClause = trimmedSql.substring(selectIndex + 6, fromIndex).trim()
        if (selectClause == "*") {
            // 如果是SELECT *，提示用户
            tableModel.addRow(arrayOf(msg("dialog.sharding.statistics.wildcard.hint"), StatisticsType.SUM))
            return
        }
        
        // 分割字段，处理逗号分隔
        val fields = selectClause.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { field ->
                // 处理字段别名，如 "qty AS quantity" -> "qty"
                if (field.contains(" AS ", ignoreCase = true)) {
                    field.substringBefore(" AS ").trim()
                } else {
                    field
                }
            }
        
        extractedFields.addAll(fields)
        
        // 添加到表格中，默认使用SUM统计
        fields.forEach { field ->
            tableModel.addRow(arrayOf(field, StatisticsType.SUM))
        }
    }
    
    /**
     * 一键应用统计类型到所有字段
     */
    private fun applyGlobalStatisticsType() {
        val selectedType = globalStatisticsTypeComboBox.selectedItem as StatisticsType
        for (i in 0 until tableModel.rowCount) {
            tableModel.setValueAt(selectedType, i, 1)
        }
    }
    
    /**
     * 统计类型枚举
     */
    enum class StatisticsType(private val displayNameKey: String, val descriptionKey: String) {
        SUM("dialog.sharding.statistics.type.sum", "dialog.sharding.statistics.type.sum.description"),
        COUNT("dialog.sharding.statistics.type.count", "dialog.sharding.statistics.type.count.description"),
        AVG("dialog.sharding.statistics.type.avg", "dialog.sharding.statistics.type.avg.description"),
        MAX("dialog.sharding.statistics.type.max", "dialog.sharding.statistics.type.max.description"),
        MIN("dialog.sharding.statistics.type.min", "dialog.sharding.statistics.type.min.description");

        override fun toString(): String = I18nUtil.getMessage(displayNameKey)
    }
    /**
     * 返回国际化消息文本。
     */
    private fun msg(key: String, vararg args: Any): String = I18nUtil.getMessage(key, *args)
}
