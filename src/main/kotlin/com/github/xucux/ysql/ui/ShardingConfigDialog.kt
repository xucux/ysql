package com.github.xucux.ysql.ui

import com.github.xucux.ysql.models.ShardingConfig
import com.github.xucux.ysql.models.SuffixType
import com.github.xucux.ysql.services.SqlShardingService
import com.github.xucux.ysql.services.TableNameExtractorService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.openapi.ui.ComboBox
import javax.swing.SpinnerNumberModel
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * 分表SQL配置对话框
 * 提供用户配置分表参数的界面
 */
class ShardingConfigDialog(
    private val project: Project,
    private val initialSql: String = ""
) : DialogWrapper(project) {
    
    private val tableNamesField = JBTextField()
    private val suffixTypeComboBox = ComboBox(SuffixType.values())
    private val suffixFormatField = JBTextField().apply { text = "_" }
    private val shardCountField = JSpinner(SpinnerNumberModel(1, 1, 1000, 1))
    private val startYearField = JSpinner(SpinnerNumberModel(2020, 1900, 2100, 1))
    private val startMonthField = JSpinner(SpinnerNumberModel(1, 1, 12, 1))
    private val sqlTextArea = JBTextArea(10, 50)
    
    private val extractTableNamesButton = JButton("自动识别表名")
    private val previewButton = JButton("预览配置")
    private val previewTextArea = JBTextArea(5, 50)
    
    init {
        title = "分表SQL配置"
        init()
        
        // 设置初始值
        sqlTextArea.text = initialSql
        sqlTextArea.lineWrap = true
        sqlTextArea.wrapStyleWord = true
        
        previewTextArea.isEditable = false
        previewTextArea.lineWrap = true
        previewTextArea.wrapStyleWord = true
        
        // 设置事件监听器
        setupEventListeners()
        
        // 如果初始SQL不为空，自动识别表名
        if (initialSql.isNotBlank()) {
            extractTableNames()
        }
    }
    
    private fun setupEventListeners() {
        // 自动识别表名按钮
        extractTableNamesButton.addActionListener {
            extractTableNames()
        }
        
        // 预览配置按钮
        previewButton.addActionListener {
            showPreview()
        }
        
        // 后缀类型变化时更新相关字段的可见性
        suffixTypeComboBox.addActionListener {
            updateFieldVisibility()
        }
        
        // 初始更新字段可见性
        updateFieldVisibility()
    }
    
    private fun extractTableNames() {
        val sql = sqlTextArea.text.trim()
        if (sql.isBlank()) {
            JOptionPane.showMessageDialog(
                this.contentPanel,
                "请先输入SQL语句",
                "提示",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }
        
        try {
            val extractorService = ApplicationManager.getApplication().getService(TableNameExtractorService::class.java)
            val result = extractorService.validateAndExtractTableNames(sql)
            
            if (result.success) {
                tableNamesField.text = result.tableNames.joinToString(", ")
                JOptionPane.showMessageDialog(
                    this.contentPanel,
                    "成功识别到表名：${result.tableNames.joinToString(", ")}",
                    "成功",
                    JOptionPane.INFORMATION_MESSAGE
                )
            } else {
                JOptionPane.showMessageDialog(
                    this.contentPanel,
                    "识别表名失败：${result.errorMessage}",
                    "错误",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                this.contentPanel,
                "识别表名时发生异常：${e.message}",
                "异常",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
    
    private fun showPreview() {
        val config = getConfig()
        try {
            val shardingService = ApplicationManager.getApplication().getService(SqlShardingService::class.java)
            val preview = shardingService.getShardingPreview(config)
            previewTextArea.text = preview
        } catch (e: Exception) {
            previewTextArea.text = "预览生成失败：${e.message}"
        }
    }
    
    private fun updateFieldVisibility() {
        val suffixType = suffixTypeComboBox.selectedItem as SuffixType
        
        // 根据后缀类型显示/隐藏相关字段
        when (suffixType) {
            SuffixType.YEAR -> {
                startYearField.isVisible = true
                startMonthField.isVisible = false
            }
            SuffixType.YEAR_MONTH -> {
                startYearField.isVisible = true
                startMonthField.isVisible = true
            }
            else -> {
                startYearField.isVisible = false
                startMonthField.isVisible = false
            }
        }
    }
    
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        
        // 创建配置面板
        val configPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("表名列表:", tableNamesField)
            .addComponent(extractTableNamesButton)
            .addSeparator()
            .addLabeledComponent("后缀类型:", suffixTypeComboBox)
            .addLabeledComponent("后缀格式:", suffixFormatField)
            .addLabeledComponent("分表数量:", shardCountField)
            .addLabeledComponent("起始年份:", startYearField)
            .addLabeledComponent("起始月份:", startMonthField)
            .addSeparator()
            .addLabeledComponent("原始SQL:", JBScrollPane(sqlTextArea))
            .addComponent(previewButton)
            .addLabeledComponent("SQL预览:", JBScrollPane(previewTextArea))
            .panel
        
        mainPanel.add(configPanel, BorderLayout.CENTER)
        
        // 设置面板大小
        mainPanel.preferredSize = Dimension(600, 700)
        
        return mainPanel
    }
    
    override fun createActions(): Array<Action> {
        return arrayOf(okAction, cancelAction)
    }
    
    override fun doOKAction() {
        // 验证配置
        val config = getConfig()
        val validationResult = validateConfig(config)
        
        if (!validationResult.isValid) {
            JOptionPane.showMessageDialog(
                this.contentPanel,
                validationResult.message,
                "配置错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        super.doOKAction()
    }
    
    private fun validateConfig(config: ShardingConfig): ValidationResult {
        if (config.tableNames.isEmpty()) {
            return ValidationResult(false, "表名列表不能为空")
        }
        
        if (config.shardCount <= 0) {
            return ValidationResult(false, "分表数量必须大于0")
        }
        
        if (config.originalSql.isBlank()) {
            return ValidationResult(false, "原始SQL语句不能为空")
        }
        
        return ValidationResult(true, "配置验证通过")
    }
    
    fun getConfig(): ShardingConfig {
        val tableNames = tableNamesField.text.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        return ShardingConfig(
            tableNames = tableNames,
            suffixType = suffixTypeComboBox.selectedItem as SuffixType,
            suffixFormat = suffixFormatField.text,
            shardCount = shardCountField.value as Int,
            startYear = startYearField.value as Int,
            startMonth = startMonthField.value as Int,
            originalSql = sqlTextArea.text
        )
    }
    
    private data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
