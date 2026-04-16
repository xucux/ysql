package com.github.xucux.ysql.ui

import com.github.xucux.ysql.models.ShardingConfig
import com.github.xucux.ysql.models.SuffixType
import com.github.xucux.ysql.services.SqlShardingService
import com.github.xucux.ysql.services.TableNameExtractorService
import com.github.xucux.ysql.utils.I18nUtil
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
    
    private val extractTableNamesButton = JButton(msg("toolwindow.button.extract.table.names"))
    private val previewButton = JButton(msg("dialog.sharding.config.preview"))
    private val previewTextArea = JBTextArea(5, 50)
    
    init {
        title = msg("dialog.sharding.config.title")
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
    
    /**
     * 设置 `upEventListeners`。
     */
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
    
    /**
     * 提取 `tableNames`。
     */
    private fun extractTableNames() {
        val sql = sqlTextArea.text.trim()
        if (sql.isBlank()) {
            JOptionPane.showMessageDialog(
                this.contentPanel,
                msg("message.input.sql.required"),
                msg("dialog.title.info"),
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
                    msg("message.table.names.recognized.success", result.tableNames.joinToString(", ")),
                    msg("dialog.title.success"),
                    JOptionPane.INFORMATION_MESSAGE
                )
            } else {
                JOptionPane.showMessageDialog(
                    this.contentPanel,
                    msg("message.table.names.recognized.failed", result.errorMessage ?: ""),
                    msg("dialog.title.error"),
                    JOptionPane.ERROR_MESSAGE
                )
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                this.contentPanel,
                msg("message.table.names.recognized.exception", e.message ?: ""),
                msg("dialog.title.exception"),
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
    
    /**
     * 展示 `preview`。
     */
    private fun showPreview() {
        val config = getConfig()
        try {
            val shardingService = ApplicationManager.getApplication().getService(SqlShardingService::class.java)
            val preview = shardingService.getShardingPreview(config)
            previewTextArea.text = preview
        } catch (e: Exception) {
            previewTextArea.text = msg("message.preview.generate.failed", e.message ?: "")
        }
    }
    
    /**
     * 更新 `fieldVisibility`。
     */
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
    
    /**
     * 创建对话框主体面板。
     */
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        
        // 创建配置面板
        val configPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.table.names"), tableNamesField)
            .addComponent(extractTableNamesButton)
            .addSeparator()
            .addLabeledComponent(msg("toolwindow.label.suffix.type"), suffixTypeComboBox)
            .addLabeledComponent(msg("toolwindow.label.suffix.format"), suffixFormatField)
            .addLabeledComponent(msg("toolwindow.label.shard.count"), shardCountField)
            .addLabeledComponent(msg("toolwindow.label.start.year"), startYearField)
            .addLabeledComponent(msg("toolwindow.label.start.month"), startMonthField)
            .addSeparator()
            .addLabeledComponent(msg("toolwindow.label.original.sql"), JBScrollPane(sqlTextArea))
            .addComponent(previewButton)
            .addLabeledComponent(msg("dialog.sharding.config.sql.preview"), JBScrollPane(previewTextArea))
            .panel
        
        mainPanel.add(configPanel, BorderLayout.CENTER)
        
        // 设置面板大小
        mainPanel.preferredSize = Dimension(600, 700)
        
        return mainPanel
    }
    
    /**
     * 创建当前对话框的操作列表。
     */
    override fun createActions(): Array<Action> {
        return arrayOf(okAction, cancelAction)
    }
    
    /**
     * 处理 `doOKAction` 逻辑。
     */
    override fun doOKAction() {
        // 验证配置
        val config = getConfig()
        val validationResult = validateConfig(config)
        
        if (!validationResult.isValid) {
            JOptionPane.showMessageDialog(
                this.contentPanel,
                validationResult.message,
                msg("dialog.title.config.error"),
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        super.doOKAction()
    }
    
    /**
     * 校验 `config`。
     */
    private fun validateConfig(config: ShardingConfig): ValidationResult {
        if (config.tableNames.isEmpty()) {
            return ValidationResult(false, msg("validation.table.names.required"))
        }
        
        if (config.shardCount <= 0) {
            return ValidationResult(false, msg("validation.shard.count.gt.zero"))
        }
        
        if (config.originalSql.isBlank()) {
            return ValidationResult(false, msg("validation.original.sql.required"))
        }
        
        return ValidationResult(true, msg("validation.config.ok"))
    }
    
    /**
     * 获取 `config`。
     */
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
    
    /**
     * 表示 `ValidationResult` 的结果数据。
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )

    /**
     * 返回国际化消息文本。
     */
    private fun msg(key: String, vararg args: Any): String = I18nUtil.getMessage(key, *args)
}
