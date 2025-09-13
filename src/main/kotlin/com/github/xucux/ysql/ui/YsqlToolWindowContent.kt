package com.github.xucux.ysql.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBLabel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JSpinner
import com.intellij.util.ui.FormBuilder
import javax.swing.JSeparator
import com.github.xucux.ysql.models.ShardingConfig
import com.github.xucux.ysql.models.SuffixType
import com.github.xucux.ysql.models.StringBufferConfig
import com.github.xucux.ysql.models.CodeLanguage
import com.github.xucux.ysql.services.SqlShardingService
import com.github.xucux.ysql.services.StringBufferService
import com.github.xucux.ysql.services.TableNameExtractorService
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import javax.swing.*
import javax.swing.SpinnerNumberModel

/**
 * Ysql工具窗口主内容
 * 包含分表SQL解析和StringBuffer代码生成两个主要功能
 */
class YsqlToolWindowContent(private val project: Project) {
    
    private val tabbedPane = JTabbedPane()
    private val mainPanel = JPanel(BorderLayout())
    
    // 分表SQL解析相关组件
    private val shardingTableNamesField = JBTextField()
    private val shardingSuffixTypeComboBox = JComboBox(SuffixType.values())
    private val shardingSuffixFormatField = JBTextField().apply { text = "_" }
    private val shardingShardCountField = JSpinner(SpinnerNumberModel(16, 1, 1000, 1))
    private val shardingStartYearField = JSpinner(SpinnerNumberModel(2020, 1900, 2100, 1))
    private val shardingStartMonthField = JSpinner(SpinnerNumberModel(1, 1, 12, 1))
    private val shardingSqlTextArea = JBTextArea(8, 40)
    private val shardingExtractTableNamesButton = JButton("自动识别表名")
    private val shardingGenerateButton = JButton("生成分表SQL")
    private val shardingResultTextArea = JBTextArea(8, 40)
    
    // StringBuffer代码生成相关组件
    private val stringBufferVariableNameField = JBTextField().apply { text = "sql" }
    private val stringBufferLanguageComboBox = JComboBox(CodeLanguage.values())
    private val stringBufferAddCommentsCheckBox = JCheckBox("添加注释", true)
    private val stringBufferFormatCodeCheckBox = JCheckBox("格式化代码", true)
    private val stringBufferSqlTextArea = JBTextArea(8, 40)
    private val stringBufferGenerateButton = JButton("生成代码")
    private val stringBufferResultTextArea = JBTextArea(8, 40)
    
    init {
        setupUI()
        setupEventListeners()
    }
    
    private fun setupUI() {
        // 设置文本区域属性
        setupTextAreas()
        
        // 创建分表SQL解析面板
        val shardingPanel = createShardingPanel()
        
        // 创建StringBuffer代码生成面板
        val stringBufferPanel = createStringBufferPanel()
        
        // 添加标签页
        tabbedPane.addTab("分表SQL解析", shardingPanel)
        tabbedPane.addTab("StringBuffer代码生成", stringBufferPanel)
        
        // 设置主面板
        mainPanel.add(tabbedPane, BorderLayout.CENTER)
        mainPanel.preferredSize = Dimension(800, 600)
    }
    
    private fun setupTextAreas() {
        // 分表SQL相关文本区域
        shardingSqlTextArea.lineWrap = true
        shardingSqlTextArea.wrapStyleWord = true
        shardingResultTextArea.isEditable = false
        shardingResultTextArea.lineWrap = true
        shardingResultTextArea.wrapStyleWord = true
        
        // StringBuffer相关文本区域
        stringBufferSqlTextArea.lineWrap = true
        stringBufferSqlTextArea.wrapStyleWord = true
        stringBufferResultTextArea.isEditable = false
        stringBufferResultTextArea.lineWrap = true
        stringBufferResultTextArea.wrapStyleWord = true
    }
    
    private fun createShardingPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // 创建配置面板
        val configPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("表名列表:", shardingTableNamesField)
            .addComponent(shardingExtractTableNamesButton)
            .addSeparator()
            .addLabeledComponent("后缀类型:", shardingSuffixTypeComboBox)
            .addLabeledComponent("后缀格式:", shardingSuffixFormatField)
            .addLabeledComponent("分表数量:", shardingShardCountField)
            .addLabeledComponent("起始年份:", shardingStartYearField)
            .addLabeledComponent("起始月份:", shardingStartMonthField)
            .panel
        
        // 创建SQL输入面板
        val sqlInputPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("原始SQL:", JBScrollPane(shardingSqlTextArea))
            .addComponent(shardingGenerateButton)
            .addLabeledComponent("生成结果:", JBScrollPane(shardingResultTextArea))
            .panel
        
        // 创建分割面板
        val splitter = JBSplitter(true, 0.4f)
        splitter.firstComponent = configPanel
        splitter.secondComponent = sqlInputPanel
        
        panel.add(splitter, BorderLayout.CENTER)
        return panel
    }
    
    private fun createStringBufferPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // 创建配置面板
        val configPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("变量名称:", stringBufferVariableNameField)
            .addLabeledComponent("编程语言:", stringBufferLanguageComboBox)
            .addComponent(stringBufferAddCommentsCheckBox)
            .addComponent(stringBufferFormatCodeCheckBox)
            .panel
        
        // 创建代码生成面板
        val codePanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("原始SQL:", JBScrollPane(stringBufferSqlTextArea))
            .addComponent(stringBufferGenerateButton)
            .addLabeledComponent("生成结果:", JBScrollPane(stringBufferResultTextArea))
            .panel
        
        // 创建分割面板
        val splitter = JBSplitter(true, 0.3f)
        splitter.firstComponent = configPanel
        splitter.secondComponent = codePanel
        
        panel.add(splitter, BorderLayout.CENTER)
        return panel
    }
    
    private fun setupEventListeners() {
        // 分表SQL解析事件监听器
        setupShardingEventListeners()
        
        // StringBuffer代码生成事件监听器
        setupStringBufferEventListeners()
    }
    
    private fun setupShardingEventListeners() {
        // 自动识别表名按钮
        shardingExtractTableNamesButton.addActionListener {
            extractTableNames()
        }
        
        
        // 生成分表SQL按钮
        shardingGenerateButton.addActionListener {
            generateShardingSql()
        }
        
        // 后缀类型变化时更新相关字段的可见性
        shardingSuffixTypeComboBox.addActionListener {
            updateShardingFieldVisibility()
        }
        
        // 初始更新字段可见性
        updateShardingFieldVisibility()
    }
    
    private fun setupStringBufferEventListeners() {
        
        // 生成代码按钮
        stringBufferGenerateButton.addActionListener {
            generateStringBufferCode()
        }
    }
    
    private fun extractTableNames() {
        val sql = shardingSqlTextArea.text.trim()
        if (sql.isBlank()) {
            Messages.showInfoMessage("请先输入SQL语句", "提示")
            return
        }
        
        try {
            val extractorService = ServiceManager.getService(TableNameExtractorService::class.java)
            val result = extractorService.validateAndExtractTableNames(sql)
            
            if (result.success) {
                shardingTableNamesField.text = result.tableNames.joinToString(", ")
                Messages.showInfoMessage("成功识别到表名：${result.tableNames.joinToString(", ")}", "成功")
            } else {
                Messages.showErrorDialog("识别表名失败：${result.errorMessage}", "错误")
            }
        } catch (e: Exception) {
            Messages.showErrorDialog("识别表名时发生异常：${e.message}", "异常")
        }
    }
    
    
    private fun generateShardingSql() {
        val config = getShardingConfig()
        val validationResult = validateShardingConfig(config)
        
        if (!validationResult.isValid) {
            Messages.showErrorDialog(validationResult.message, "配置错误")
            return
        }
        
        // 在后台线程中生成SQL
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val shardingService = ServiceManager.getService(SqlShardingService::class.java)
                val result = shardingService.generateShardingSql(config)
                
                // 在UI线程中显示结果
                ApplicationManager.getApplication().invokeLater {
                    if (result.success) {
                        shardingResultTextArea.text = result.getFormattedResult()
                    } else {
                        Messages.showErrorDialog("生成分表SQL失败：${result.errorMessage}", "错误")
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog("生成分表SQL时发生异常：${e.message}", "异常")
                }
            }
        }
    }
    
    
    private fun generateStringBufferCode() {
        val config = getStringBufferConfig()
        val validationResult = validateStringBufferConfig(config)
        
        if (!validationResult.isValid) {
            Messages.showErrorDialog(validationResult.message, "配置错误")
            return
        }
        
        // 在后台线程中生成代码
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val stringBufferService = ServiceManager.getService(StringBufferService::class.java)
                val result = stringBufferService.generateStringBufferCode(config)
                
                // 在UI线程中显示结果
                ApplicationManager.getApplication().invokeLater {
                    if (result.success) {
                        stringBufferResultTextArea.text = result.getFormattedResult()
                    } else {
                        Messages.showErrorDialog("生成StringBuffer代码失败：${result.errorMessage}", "错误")
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog("生成StringBuffer代码时发生异常：${e.message}", "异常")
                }
            }
        }
    }
    
    private fun updateShardingFieldVisibility() {
        val suffixType = shardingSuffixTypeComboBox.selectedItem as SuffixType
        
        // 根据后缀类型显示/隐藏相关字段
        when (suffixType) {
            SuffixType.YEAR -> {
                shardingStartYearField.isVisible = true
                shardingStartMonthField.isVisible = false
            }
            SuffixType.YEAR_MONTH -> {
                shardingStartYearField.isVisible = true
                shardingStartMonthField.isVisible = true
            }
            else -> {
                shardingStartYearField.isVisible = false
                shardingStartMonthField.isVisible = false
            }
        }
    }
    
    private fun getShardingConfig(): ShardingConfig {
        val tableNames = shardingTableNamesField.text.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        return ShardingConfig(
            tableNames = tableNames,
            suffixType = shardingSuffixTypeComboBox.selectedItem as SuffixType,
            suffixFormat = shardingSuffixFormatField.text,
            shardCount = shardingShardCountField.value as Int,
            startYear = shardingStartYearField.value as Int,
            startMonth = shardingStartMonthField.value as Int,
            originalSql = shardingSqlTextArea.text
        )
    }
    
    private fun getStringBufferConfig(): StringBufferConfig {
        return StringBufferConfig(
            variableName = stringBufferVariableNameField.text,
            language = stringBufferLanguageComboBox.selectedItem as CodeLanguage,
            originalSql = stringBufferSqlTextArea.text,
            addComments = stringBufferAddCommentsCheckBox.isSelected,
            formatCode = stringBufferFormatCodeCheckBox.isSelected
        )
    }
    
    private fun validateShardingConfig(config: ShardingConfig): ValidationResult {
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
    
    private fun validateStringBufferConfig(config: StringBufferConfig): ValidationResult {
        if (config.variableName.isBlank()) {
            return ValidationResult(false, "变量名称不能为空")
        }
        
        if (config.originalSql.isBlank()) {
            return ValidationResult(false, "原始SQL语句不能为空")
        }
        
        return ValidationResult(true, "配置验证通过")
    }
    
    fun getContentPanel(): JComponent {
        return mainPanel
    }
    
    private data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
