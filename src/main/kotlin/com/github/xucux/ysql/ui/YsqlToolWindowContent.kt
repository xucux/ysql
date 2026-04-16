package com.github.xucux.ysql.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import javax.swing.JButton
import com.intellij.openapi.ui.ComboBox
import javax.swing.JSpinner
import com.intellij.util.ui.FormBuilder
import javax.swing.JSeparator
import com.github.xucux.ysql.models.ShardingConfig
import com.github.xucux.ysql.models.SuffixType
import com.github.xucux.ysql.models.StringBufferConfig
import com.github.xucux.ysql.models.CodeLanguage
import com.github.xucux.ysql.models.SqlReverseResult
import com.github.xucux.ysql.models.BatchDeleteConfig
import com.github.xucux.ysql.models.BatchDeleteTemplate
import com.github.xucux.ysql.models.DynamicSqlConfig
import com.github.xucux.ysql.services.SqlShardingService
import com.github.xucux.ysql.services.ShardingStatisticsService
import com.github.xucux.ysql.services.StringBufferService
import com.github.xucux.ysql.services.TableNameExtractorService
import com.github.xucux.ysql.services.BatchDeleteService
import com.github.xucux.ysql.services.DynamicSqlService
import com.github.xucux.ysql.utils.I18nUtil
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
    
    private val tabbedPane = JBTabbedPane()
    private val stingBufferPanel = JPanel(BorderLayout())
    private var shardingPanel : JPanel? = null
    private var batchDeletePanel : JPanel? = null
    private var dynamicSqlPanel : JPanel? = null
    
    // 分表SQL解析相关组件
    private val shardingTableNamesField = JBTextField()
    private val shardingSuffixTypeComboBox = ComboBox(SuffixType.values())
    private val shardingSuffixFormatField = JBTextField().apply { text = "_" }
    private val shardingShardCountField = JSpinner(SpinnerNumberModel(16, 1, 1000, 1))
    private val shardingStartYearField = JSpinner(SpinnerNumberModel(2020, 1900, 2100, 1))
    private val shardingStartMonthField = JSpinner(SpinnerNumberModel(1, 1, 12, 1))
    private val shardingSqlTextArea = JBTextArea(8, 40)
    private val shardingExtractTableNamesButton = JButton(msg("toolwindow.button.extract.table.names"))
    private val shardingGenerateButton = JButton(msg("toolwindow.button.generate.sharding.sql"))
    private val shardingStatisticsButton = JButton(msg("toolwindow.button.generate.sharding.statistics"))
    private val shardingResultTextArea = JBTextArea(8, 40)
    
    // StringBuffer代码生成相关组件
    private val stringBufferVariableNameField = JBTextField().apply { text = "sql" }
    private val stringBufferLanguageComboBox = ComboBox(CodeLanguage.values())
    private val stringBufferAddCommentsCheckBox = JCheckBox(msg("toolwindow.checkbox.add.comments"), false)
    private val stringBufferFormatCodeCheckBox = JCheckBox(msg("toolwindow.checkbox.format.code"), false)
    private val stringBufferSqlTextArea = JBTextArea(8, 40)
    private val stringBufferGenerateButton = JButton(msg("toolwindow.button.generate.code"))
    private val stringBufferResultTextArea = JBTextArea(8, 40)
    
    // SQL反向解析相关组件
    private val reverseParseCodeTextArea = JBTextArea(8, 40)
    private val reverseParseLanguageComboBox = ComboBox(CodeLanguage.values())
    private val reverseParseAutoDetectCheckBox = JCheckBox(msg("toolwindow.checkbox.auto.detect.language"), true)
    private val reverseParseButton = JButton(msg("toolwindow.button.reverse.parse.sql"))
    private val reverseParseResultTextArea = JBTextArea(8, 40)
    
    // 批量删除存储过程相关组件
    private val batchDeleteTemplateComboBox = ComboBox(BatchDeleteTemplate.values())
    private val batchDeleteApplyTemplateButton = JButton(msg("toolwindow.button.apply.template"))
    private val batchDeleteProcedureNameField = JBTextField("DropHistoryDataByLimit")
    private val batchDeleteMainTableNameField = JBTextField("system_logs")
    private val batchDeletePrimaryKeyField = JBTextField("id")
    private val batchDeleteTimeField = JBTextField("create_time")
    private val batchDeleteLimitSizeSpinner = JSpinner(SpinnerNumberModel(1000, 1, 100000, 100))
    private val batchDeleteMinIdSpinner = JSpinner(SpinnerNumberModel(0L, 0L, Long.MAX_VALUE, 1L))
    private val batchDeleteCreateTimeEndField = JBTextField("2023-01-01 00:00:00")
    private val batchDeleteAddLogTableCheckBox = JCheckBox(msg("toolwindow.checkbox.add.log.table"), true)
    private val batchDeleteAddTempTableCheckBox = JCheckBox(msg("toolwindow.checkbox.add.temp.table"), true)
    private val batchDeleteCustomWhereConditionField = JBTextField()
    private val batchDeleteProcedureCommentField = JBTextField(msg("toolwindow.default.procedure.comment"))
    private val batchDeleteGenerateButton = JButton(msg("toolwindow.button.generate.procedure"))
    private val batchDeleteResultTextArea = JBTextArea(8, 40)
    
    // 动态语句相关组件
    private val dynamicSqlTextArea = JBTextArea(8, 40)
    private val dynamicSqlIgnoredVariablesField = JBTextField()
    private val dynamicSqlRecognizedVariablesTextArea = JBTextArea(4, 40)
    private val dynamicSqlRecognizeVariablesButton = JButton(msg("toolwindow.button.recognize.variables"))
    private val dynamicSqlCopyToIgnoredButton = JButton(msg("toolwindow.button.copy.selected.to.ignored"))
    private val dynamicSqlEnableShardingCheckBox = JCheckBox(msg("toolwindow.checkbox.enable.sharding.suffix"), false)
    private val dynamicSqlShardingSuffixFormatField = JBTextField().apply { text = "_" }
    private val dynamicSqlShardingSuffixVariableField = JBTextField().apply { text = "sharding_suffix" }
    private val dynamicSqlVariablePrefixField = JBTextField().apply { text = "query_sql" }
    private val dynamicSqlStatementPrefixField = JBTextField().apply { text = "stmt" }
    private val dynamicSqlGenerateButton = JButton(msg("toolwindow.button.generate.dynamic.sql"))
    private val dynamicSqlResultTextArea = JBTextArea(8, 40)
    
    init {
        setupUI()
        setupEventListeners()
    }
    
    /**
     * 设置 `upUI`。
     */
    private fun setupUI() {
        // 设置文本区域属性
        setupTextAreas()
        
        // 创建分表SQL解析面板
        shardingPanel = createShardingPanel()
        
        // 创建StringBuffer代码生成面板
        val stringBufferPanel = createStringBufferPanel()
        
        // 创建SQL反向解析面板
        val reverseParsePanel = createReverseParsePanel()
        
        // 创建批量删除存储过程面板
        batchDeletePanel = createBatchDeletePanel()
        
        // 创建动态语句面板
        dynamicSqlPanel = createDynamicSqlPanel()
        
        // 添加标签页
//        tabbedPane.addTab("分表SQL解析",  shardingPanel)
        tabbedPane.addTab(msg("toolwindow.inner.tab.string.buffer"),  stringBufferPanel)
        tabbedPane.addTab(msg("toolwindow.inner.tab.reverse.parse"),  reverseParsePanel)
        tabbedPane.addTab(msg("toolwindow.inner.tab.dynamic.sql"),  dynamicSqlPanel)
//        tabbedPane.addTab("批量删除存储过程",batchDeletePanel)
        
        // 设置主面板
        stingBufferPanel.add(tabbedPane, BorderLayout.CENTER)
        stingBufferPanel.preferredSize = Dimension(800, 600)
    }
    
    /**
     * 设置 `upTextAreas`。
     */
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
        
        // SQL反向解析相关文本区域
        reverseParseCodeTextArea.lineWrap = true
        reverseParseCodeTextArea.wrapStyleWord = true
        reverseParseResultTextArea.isEditable = false
        reverseParseResultTextArea.lineWrap = true
        reverseParseResultTextArea.wrapStyleWord = true
        
        // 批量删除存储过程相关文本区域
        batchDeleteResultTextArea.isEditable = false
        batchDeleteResultTextArea.lineWrap = true
        batchDeleteResultTextArea.wrapStyleWord = true
        
        // 动态语句相关文本区域
        dynamicSqlTextArea.lineWrap = true
        dynamicSqlTextArea.wrapStyleWord = true
        dynamicSqlRecognizedVariablesTextArea.isEditable = false
        dynamicSqlRecognizedVariablesTextArea.lineWrap = true
        dynamicSqlRecognizedVariablesTextArea.wrapStyleWord = true
        dynamicSqlResultTextArea.isEditable = false
        dynamicSqlResultTextArea.lineWrap = true
        dynamicSqlResultTextArea.wrapStyleWord = true
    }
    
    /**
     * 创建 `shardingPanel`。
     */
    private fun createShardingPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // 创建配置面板
        val configPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.table.names"), shardingTableNamesField)
            .addComponent(shardingExtractTableNamesButton)
            .addSeparator()
            .addLabeledComponent(msg("toolwindow.label.suffix.type"), shardingSuffixTypeComboBox)
            .addLabeledComponent(msg("toolwindow.label.suffix.format"), shardingSuffixFormatField)
            .addLabeledComponent(msg("toolwindow.label.shard.count"), shardingShardCountField)
            .addLabeledComponent(msg("toolwindow.label.start.year"), shardingStartYearField)
            .addLabeledComponent(msg("toolwindow.label.start.month"), shardingStartMonthField)
            .panel
        
        // 创建按钮面板
        val buttonPanel = JPanel()
        buttonPanel.add(shardingGenerateButton)
        buttonPanel.add(shardingStatisticsButton)
        
        // 创建SQL输入面板
        val sqlInputPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.original.sql"), JBScrollPane(shardingSqlTextArea))
            .addComponent(buttonPanel)
            .addLabeledComponent(msg("toolwindow.label.generated.result"), JBScrollPane(shardingResultTextArea))
            .panel
        
        // 创建分割面板
        val splitter = JBSplitter(true, 0.4f)
        splitter.firstComponent = configPanel
        splitter.secondComponent = sqlInputPanel
        
        panel.add(splitter, BorderLayout.CENTER)
        return panel
    }
    
    /**
     * 创建 `stringBufferPanel`。
     */
    private fun createStringBufferPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // 创建配置面板
        val configPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.variable.name"), stringBufferVariableNameField)
            .addLabeledComponent(msg("toolwindow.label.programming.language"), stringBufferLanguageComboBox)
            .addComponent(stringBufferAddCommentsCheckBox)
            .addComponent(stringBufferFormatCodeCheckBox)
            .panel
        
        // 创建代码生成面板
        val codePanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.original.sql"), JBScrollPane(stringBufferSqlTextArea))
            .addComponent(stringBufferGenerateButton)
            .addLabeledComponent(msg("toolwindow.label.generated.result"), JBScrollPane(stringBufferResultTextArea))
            .panel
        
        // 创建分割面板
        val splitter = JBSplitter(true, 0.3f)
        splitter.firstComponent = configPanel
        splitter.secondComponent = codePanel
        
        panel.add(splitter, BorderLayout.CENTER)
        return panel
    }
    
    /**
     * 创建 `reverseParsePanel`。
     */
    private fun createReverseParsePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // 创建配置面板
        val configPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.programming.language"), reverseParseLanguageComboBox)
            .addComponent(reverseParseAutoDetectCheckBox)
            .panel
        
        // 创建反向解析面板
        val parsePanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.code"), JBScrollPane(reverseParseCodeTextArea))
            .addComponent(reverseParseButton)
            .addLabeledComponent(msg("toolwindow.label.parse.result"), JBScrollPane(reverseParseResultTextArea))
            .panel
        
        // 创建分割面板
        val splitter = JBSplitter(true, 0.3f)
        splitter.firstComponent = configPanel
        splitter.secondComponent = parsePanel
        
        panel.add(splitter, BorderLayout.CENTER)
        return panel
    }
    
    /**
     * 创建 `batchDeletePanel`。
     */
    private fun createBatchDeletePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // 创建模板选择面板
        val templatePanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.select.template"), batchDeleteTemplateComboBox)
            .addComponent(batchDeleteApplyTemplateButton)
            .panel
        
        // 创建基础配置面板
        val basicConfigPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.procedure.name"), batchDeleteProcedureNameField)
            .addLabeledComponent(msg("toolwindow.label.main.table.name"), batchDeleteMainTableNameField)
            .addLabeledComponent(msg("toolwindow.label.primary.key.field"), batchDeletePrimaryKeyField)
            .addLabeledComponent(msg("toolwindow.label.time.field"), batchDeleteTimeField)
            .panel
        
        // 创建参数配置面板
        val paramConfigPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.delete.limit"), batchDeleteLimitSizeSpinner)
            .addLabeledComponent(msg("toolwindow.label.min.id"), batchDeleteMinIdSpinner)
            .addLabeledComponent(msg("toolwindow.label.delete.end.time"), batchDeleteCreateTimeEndField)
            .panel
        
        // 创建高级配置面板
        val advancedConfigPanel = FormBuilder.createFormBuilder()
            .addComponent(batchDeleteAddLogTableCheckBox)
            .addComponent(batchDeleteAddTempTableCheckBox)
            .addLabeledComponent(msg("toolwindow.label.custom.where"), batchDeleteCustomWhereConditionField)
            .addLabeledComponent(msg("toolwindow.label.procedure.comment"), batchDeleteProcedureCommentField)
            .panel
        
        // 创建配置面板（左侧）
        val configPanel = JPanel(BorderLayout())
        val configTabbedPane = JBTabbedPane()
        configTabbedPane.addTab(msg("toolwindow.tab.template"), templatePanel)
        configTabbedPane.addTab(msg("toolwindow.tab.basic.config"), basicConfigPanel)
        configTabbedPane.addTab(msg("toolwindow.tab.param.config"), paramConfigPanel)
        configTabbedPane.addTab(msg("toolwindow.tab.advanced.config"), advancedConfigPanel)
        configPanel.add(configTabbedPane, BorderLayout.CENTER)
        
        // 创建生成面板（右侧）
        val generatePanel = FormBuilder.createFormBuilder()
            .addComponent(batchDeleteGenerateButton)
            .addLabeledComponent(msg("toolwindow.label.generated.result"), JBScrollPane(batchDeleteResultTextArea))
            .panel
        
        // 创建分割面板
        val splitter = JBSplitter(true, 0.4f)
        splitter.firstComponent = configPanel
        splitter.secondComponent = generatePanel
        
        panel.add(splitter, BorderLayout.CENTER)
        return panel
    }
    
    /**
     * 创建 `dynamicSqlPanel`。
     */
    private fun createDynamicSqlPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // 创建基础配置面板
        val basicConfigPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.ignored.variables"), dynamicSqlIgnoredVariablesField)
            .addComponent(JBLabel(msg("toolwindow.hint.ignored.variables")))
            .addSeparator()
            .addLabeledComponent(msg("toolwindow.label.recognized.variables"), JBScrollPane(dynamicSqlRecognizedVariablesTextArea))
            .addComponent(dynamicSqlRecognizeVariablesButton)
            .addComponent(dynamicSqlCopyToIgnoredButton)
            .panel
        
        // 创建分片配置面板
        val shardingConfigPanel = FormBuilder.createFormBuilder()
            .addComponent(dynamicSqlEnableShardingCheckBox)
            .addLabeledComponent(msg("toolwindow.label.sharding.suffix.format"), dynamicSqlShardingSuffixFormatField)
            .addLabeledComponent(msg("toolwindow.label.sharding.suffix.variable"), dynamicSqlShardingSuffixVariableField)
            .panel
        
        // 创建变量配置面板
        val variableConfigPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.sql.variable.prefix"), dynamicSqlVariablePrefixField)
            .addLabeledComponent(msg("toolwindow.label.statement.variable.prefix"), dynamicSqlStatementPrefixField)
            .panel
        
        // 创建配置面板（左侧）
        val configPanel = JPanel(BorderLayout())
        val configTabbedPane = JBTabbedPane()
        configTabbedPane.addTab(msg("toolwindow.tab.basic.config"), basicConfigPanel)
        configTabbedPane.addTab(msg("toolwindow.tab.sharding.config"), shardingConfigPanel)
        configTabbedPane.addTab(msg("toolwindow.tab.variable.config"), variableConfigPanel)
        configPanel.add(configTabbedPane, BorderLayout.CENTER)
        
        // 创建生成面板（右侧）
        val generatePanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.original.sql"), JBScrollPane(dynamicSqlTextArea))
            .addComponent(dynamicSqlGenerateButton)
            .addLabeledComponent(msg("toolwindow.label.generated.result"), JBScrollPane(dynamicSqlResultTextArea))
            .panel
        
        // 创建分割面板
        val splitter = JBSplitter(true, 0.4f)
        splitter.firstComponent = configPanel
        splitter.secondComponent = generatePanel
        
        panel.add(splitter, BorderLayout.CENTER)
        return panel
    }
    
    /**
     * 设置 `upEventListeners`。
     */
    private fun setupEventListeners() {
        // 分表SQL解析事件监听器
        setupShardingEventListeners()
        
        // StringBuffer代码生成事件监听器
        setupStringBufferEventListeners()
        
        // SQL反向解析事件监听器
        setupReverseParseEventListeners()
        
        // 批量删除存储过程事件监听器
        setupBatchDeleteEventListeners()
        
        // 动态语句事件监听器
        setupDynamicSqlEventListeners()
    }
    
    /**
     * 设置 `upShardingEventListeners`。
     */
    private fun setupShardingEventListeners() {
        // 自动识别表名按钮
        shardingExtractTableNamesButton.addActionListener {
            extractTableNames()
        }
        
        
        // 生成分表SQL按钮
        shardingGenerateButton.addActionListener {
            generateShardingSql()
        }
        
        // 生成分表统计按钮
        shardingStatisticsButton.addActionListener {
            generateShardingStatistics()
        }
        
        // 后缀类型变化时更新相关字段的可见性
        shardingSuffixTypeComboBox.addActionListener {
            updateShardingFieldVisibility()
        }
        
        // 初始更新字段可见性
        updateShardingFieldVisibility()
    }
    
    /**
     * 设置 `upStringBufferEventListeners`。
     */
    private fun setupStringBufferEventListeners() {
        
        // 生成代码按钮
        stringBufferGenerateButton.addActionListener {
            generateStringBufferCode()
        }
    }
    
    /**
     * 设置 `upReverseParseEventListeners`。
     */
    private fun setupReverseParseEventListeners() {
        // 反向解析按钮
        reverseParseButton.addActionListener {
            reverseParseSql()
        }
        
        // 自动检测语言复选框
        reverseParseAutoDetectCheckBox.addActionListener {
            reverseParseLanguageComboBox.isEnabled = !reverseParseAutoDetectCheckBox.isSelected
        }
        
        // 初始设置语言选择框状态
        reverseParseLanguageComboBox.isEnabled = !reverseParseAutoDetectCheckBox.isSelected
    }
    
    /**
     * 设置 `upBatchDeleteEventListeners`。
     */
    private fun setupBatchDeleteEventListeners() {
        // 应用模板按钮
        batchDeleteApplyTemplateButton.addActionListener {
            applyBatchDeleteTemplate()
        }
        
        // 生成存储过程按钮
        batchDeleteGenerateButton.addActionListener {
            generateBatchDeleteProcedure()
        }
        
        // 表名变化时自动生成存储过程名
        batchDeleteMainTableNameField.addActionListener {
            autoGenerateBatchDeleteProcedureName()
        }
    }
    
    /**
     * 设置 `upDynamicSqlEventListeners`。
     */
    private fun setupDynamicSqlEventListeners() {
        // 生成动态语句按钮
        dynamicSqlGenerateButton.addActionListener {
            generateDynamicSql()
        }
        
        // 识别变量按钮
        dynamicSqlRecognizeVariablesButton.addActionListener {
            recognizeVariables()
        }
        
        // 复制选中到忽略列表按钮
        dynamicSqlCopyToIgnoredButton.addActionListener {
            copySelectedToIgnored()
        }
        
        // 启用分片后缀复选框变化时更新相关字段的可见性
        dynamicSqlEnableShardingCheckBox.addActionListener {
            updateDynamicSqlFieldVisibility()
        }
        
        // 初始更新字段可见性
        updateDynamicSqlFieldVisibility()
    }
    
    /**
     * 提取 `tableNames`。
     */
    private fun extractTableNames() {
        val sql = shardingSqlTextArea.text.trim()
        if (sql.isBlank()) {
            Messages.showInfoMessage(msg("message.input.sql.required"), msg("dialog.title.info"))
            return
        }
        
        try {
            val extractorService = ApplicationManager.getApplication().getService(TableNameExtractorService::class.java)
            val result = extractorService.validateAndExtractTableNames(sql)
            
            if (result.success) {
                shardingTableNamesField.text = result.tableNames.joinToString(", ")
                Messages.showInfoMessage(
                    msg("message.table.names.recognized.success", result.tableNames.joinToString(", ")),
                    msg("dialog.title.success")
                )
            } else {
                Messages.showErrorDialog(
                    msg("message.table.names.recognized.failed", result.errorMessage ?: ""),
                    msg("dialog.title.error")
                )
            }
        } catch (e: Exception) {
            Messages.showErrorDialog(msg("message.table.names.recognized.exception", e.message ?: ""), msg("dialog.title.exception"))
        }
    }
    
    
    /**
     * 生成 `shardingSql`。
     */
    private fun generateShardingSql() {
        val config = getShardingConfig()
        val validationResult = validateShardingConfig(config)
        
        if (!validationResult.isValid) {
            Messages.showErrorDialog(validationResult.message, msg("dialog.title.config.error"))
            return
        }
        
        // 在后台线程中生成SQL
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val shardingService = ApplicationManager.getApplication().getService(SqlShardingService::class.java)
                val result = shardingService.generateShardingSql(config)
                
                // 在UI线程中显示结果
                ApplicationManager.getApplication().invokeLater {
                    if (result.success) {
                        shardingResultTextArea.text = result.getFormattedResult()
                    } else {
                        Messages.showErrorDialog(msg("message.sharding.generate.failed", result.errorMessage ?: ""), msg("dialog.title.error"))
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(msg("message.sharding.generate.exception", e.message ?: ""), msg("dialog.title.exception"))
                }
            }
        }
    }
    
    /**
     * 生成 `shardingStatistics`。
     */
    private fun generateShardingStatistics() {
        // 获取当前配置
        val currentConfig = getShardingConfig()
        
        // 验证基础配置
        val validationResult = validateShardingConfig(currentConfig)
        if (!validationResult.isValid) {
            Messages.showErrorDialog(validationResult.message, msg("dialog.title.config.error"))
            return
        }
        
        // 显示统计配置弹窗
        val dialog = ShardingStatisticsConfigDialog(project, currentConfig.originalSql)
        if (dialog.showAndGet()) {
            val fieldStatisticsConfig = dialog.getFieldStatisticsConfig()
            
            // 在后台线程中生成统计SQL
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    val statisticsService = ApplicationManager.getApplication().getService(ShardingStatisticsService::class.java)
                    val result = statisticsService.generateShardingStatistics(currentConfig, fieldStatisticsConfig)
                    
                    // 在UI线程中显示结果
                    ApplicationManager.getApplication().invokeLater {
                        if (result.success) {
                            shardingResultTextArea.text = result.getFormattedResult()
                        } else {
                            Messages.showErrorDialog(msg("message.sharding.statistics.generate.failed", result.errorMessage), msg("dialog.title.error"))
                        }
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(msg("message.sharding.statistics.generate.exception", e.message ?: ""), msg("dialog.title.exception"))
                    }
                }
            }
        }
    }
    
    /**
     * 生成 `stringBufferCode`。
     */
    private fun generateStringBufferCode() {
        val config = getStringBufferConfig()
        val validationResult = validateStringBufferConfig(config)
        
        if (!validationResult.isValid) {
            Messages.showErrorDialog(validationResult.message, msg("dialog.title.config.error"))
            return
        }
        
        // 在后台线程中生成代码
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val stringBufferService = ApplicationManager.getApplication().getService(StringBufferService::class.java)
                val result = stringBufferService.generateStringBufferCode(config)
                
                // 在UI线程中显示结果
                ApplicationManager.getApplication().invokeLater {
                    if (result.success) {
                        stringBufferResultTextArea.text = result.getFormattedResult()
                    } else {
                        Messages.showErrorDialog(msg("message.string.buffer.generate.failed", result.errorMessage ?: ""), msg("dialog.title.error"))
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(msg("message.string.buffer.generate.exception", e.message ?: ""), msg("dialog.title.exception"))
                }
            }
        }
    }
    
    /**
     * 处理 `reverseParseSql` 逻辑。
     */
    private fun reverseParseSql() {
        val code = reverseParseCodeTextArea.text.trim()
        if (code.isBlank()) {
            Messages.showInfoMessage(msg("message.input.string.buffer.code.required"), msg("dialog.title.info"))
            return
        }
        
        // 检查是否包含StringBuffer/StringBuilder
        val stringBufferService = ApplicationManager.getApplication().getService(StringBufferService::class.java)
        if (!stringBufferService.containsStringBuffer(code)) {
            Messages.showWarningDialog(
                msg("message.reverse.parse.no.buffer.input"),
                msg("dialog.title.warning")
            )
            return
        }
        
        // 在后台线程中解析SQL
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val language = if (reverseParseAutoDetectCheckBox.isSelected) {
                    null // 自动检测
                } else {
                    reverseParseLanguageComboBox.selectedItem as CodeLanguage
                }
                
                val result = stringBufferService.reverseParseSql(code, language)
                
                // 在UI线程中显示结果
                ApplicationManager.getApplication().invokeLater {
                    if (result.success) {
                        reverseParseResultTextArea.text = result.getFormattedResult()
                    } else {
                        Messages.showErrorDialog(msg("message.reverse.parse.failed", result.errorMessage ?: ""), msg("dialog.title.error"))
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(msg("message.reverse.parse.exception", e.message ?: ""), msg("dialog.title.exception"))
                }
            }
        }
    }
    
    /**
     * 处理 `applyBatchDeleteTemplate` 逻辑。
     */
    private fun applyBatchDeleteTemplate() {
        val selectedTemplate = batchDeleteTemplateComboBox.selectedItem as BatchDeleteTemplate
        val templateConfig = selectedTemplate.config
        
        // 应用模板配置到各个字段
        batchDeleteProcedureNameField.text = templateConfig.procedureName
        batchDeleteMainTableNameField.text = templateConfig.mainTableName
        batchDeletePrimaryKeyField.text = templateConfig.primaryKeyField
        batchDeleteTimeField.text = templateConfig.timeField
        batchDeleteLimitSizeSpinner.value = templateConfig.limitSize
        batchDeleteMinIdSpinner.value = templateConfig.minId
        batchDeleteCreateTimeEndField.text = templateConfig.createTimeEnd
        batchDeleteAddLogTableCheckBox.isSelected = templateConfig.addLogTable
        batchDeleteAddTempTableCheckBox.isSelected = templateConfig.addTempTable
        batchDeleteCustomWhereConditionField.text = templateConfig.customWhereCondition
        batchDeleteProcedureCommentField.text = templateConfig.procedureComment
        
        Messages.showInfoMessage(msg("message.template.applied"), msg("dialog.title.template.applied"))
    }
    
    /**
     * 处理 `autoGenerateBatchDeleteProcedureName` 逻辑。
     */
    private fun autoGenerateBatchDeleteProcedureName() {
        val tableName = batchDeleteMainTableNameField.text.trim()
        if (tableName.isNotBlank() && batchDeleteProcedureNameField.text == "DropHistoryDataByLimit") {
            try {
                val batchDeleteService = ApplicationManager.getApplication().getService(BatchDeleteService::class.java)
                val defaultName = batchDeleteService.generateDefaultProcedureName(tableName)
                batchDeleteProcedureNameField.text = defaultName
            } catch (e: Exception) {
                // 忽略异常，保持当前名称
            }
        }
    }
    
    /**
     * 生成 `batchDeleteProcedure`。
     */
    private fun generateBatchDeleteProcedure() {
        val config = getBatchDeleteConfig()
        val validationResult = validateBatchDeleteConfig(config)
        
        if (!validationResult.isValid) {
            Messages.showErrorDialog(validationResult.message, msg("dialog.title.config.error"))
            return
        }
        
        // 在后台线程中生成存储过程
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val batchDeleteService = ApplicationManager.getApplication().getService(BatchDeleteService::class.java)
                val result = batchDeleteService.generateBatchDeleteProcedure(config)
                
                // 在UI线程中显示结果
                ApplicationManager.getApplication().invokeLater {
                    if (result.success) {
                        batchDeleteResultTextArea.text = result.getFormattedResult()
                    } else {
                        Messages.showErrorDialog(msg("message.batch.delete.generate.failed", result.errorMessage ?: ""), msg("dialog.title.error"))
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(msg("message.batch.delete.generate.exception", e.message ?: ""), msg("dialog.title.exception"))
                }
            }
        }
    }
    
    /**
     * 生成 `dynamicSql`。
     */
    private fun generateDynamicSql() {
        val config = getDynamicSqlConfig()
        val validationResult = validateDynamicSqlConfig(config)
        
        if (!validationResult.isValid) {
            Messages.showErrorDialog(validationResult.message, msg("dialog.title.config.error"))
            return
        }
        
        // 在后台线程中生成动态SQL
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val dynamicSqlService = ApplicationManager.getApplication().getService(DynamicSqlService::class.java)
                val result = dynamicSqlService.generateDynamicSql(config)
                
                // 在UI线程中显示结果
                ApplicationManager.getApplication().invokeLater {
                    if (result.success) {
                        dynamicSqlResultTextArea.text = result.getFormattedResult()
                    } else {
                        Messages.showErrorDialog(msg("message.dynamic.sql.generate.failed", result.errorMessage ?: ""), msg("dialog.title.error"))
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(msg("message.dynamic.sql.generate.exception", e.message ?: ""), msg("dialog.title.exception"))
                }
            }
        }
    }
    
    /**
     * 更新 `dynamicSqlFieldVisibility`。
     */
    private fun updateDynamicSqlFieldVisibility() {
        val enableSharding = dynamicSqlEnableShardingCheckBox.isSelected
        
        // 根据是否启用分片后缀显示/隐藏相关字段
        dynamicSqlShardingSuffixFormatField.isVisible = enableSharding
        dynamicSqlShardingSuffixVariableField.isVisible = enableSharding
    }
    
    /**
     * 处理 `recognizeVariables` 逻辑。
     */
    private fun recognizeVariables() {
        val sql = dynamicSqlTextArea.text.trim()
        if (sql.isBlank()) {
            Messages.showInfoMessage(msg("message.input.sql.required"), msg("dialog.title.info"))
            return
        }
        
        try {
            val dynamicSqlService = ApplicationManager.getApplication().getService(DynamicSqlService::class.java)
            val ignoredVariables = dynamicSqlIgnoredVariablesField.text.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            
            val variables = dynamicSqlService.extractVariablesFromSql(sql, ignoredVariables)
            
            if (variables.isEmpty()) {
                dynamicSqlRecognizedVariablesTextArea.text = msg("message.variable.none.found")
                Messages.showInfoMessage(msg("message.variable.none.found"), msg("dialog.title.info"))
            } else {
                val variableText = buildVariableDisplayText(variables)
                dynamicSqlRecognizedVariablesTextArea.text = variableText
                Messages.showInfoMessage(msg("message.variable.recognized.success", variables.size), msg("dialog.title.success"))
            }
        } catch (e: Exception) {
            Messages.showErrorDialog(msg("message.variable.recognized.exception", e.message ?: ""), msg("dialog.title.exception"))
        }
    }
    
    /**
     * 复制 `selectedToIgnored`。
     */
    private fun copySelectedToIgnored() {
        val selectedText = dynamicSqlRecognizedVariablesTextArea.selectedText
        if (selectedText.isNullOrBlank()) {
            Messages.showInfoMessage(msg("message.variable.select.to.ignore"), msg("dialog.title.info"))
            return
        }
        
        // 解析选中的文本，提取变量名
        val selectedVariables = extractVariableNamesFromText(selectedText)
        if (selectedVariables.isEmpty()) {
            Messages.showInfoMessage(msg("message.variable.invalid.name"), msg("dialog.title.info"))
            return
        }
        
        // 获取当前忽略变量列表
        val currentIgnored = dynamicSqlIgnoredVariablesField.text.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableSet()
        
        // 添加新选择的变量
        currentIgnored.addAll(selectedVariables)
        
        // 更新忽略变量字段
        dynamicSqlIgnoredVariablesField.text = currentIgnored.joinToString(", ")
        
        Messages.showInfoMessage(msg("message.variable.added.to.ignore", selectedVariables.size), msg("dialog.title.success"))
    }
    
    /**
     * 构建 `variableDisplayText`。
     */
    private fun buildVariableDisplayText(variables: List<com.github.xucux.ysql.models.SqlVariable>): String {
        val result = StringBuilder()
        result.appendLine(msg("message.variable.recognized.header", variables.size))
        result.appendLine()
        
        variables.forEachIndexed { index, variable ->
            result.appendLine(msg("message.variable.item.name", index + 1, variable.name))
            result.appendLine(msg("message.variable.item.value", variable.value))
            result.appendLine(msg("message.variable.item.type", variable.type.displayName))
            result.appendLine(msg("message.variable.item.position", variable.position))
            result.appendLine()
        }
        
        result.appendLine(msg("message.variable.ignore.hint"))
        
        return result.toString()
    }
    
    /**
     * 提取 `variableNamesFromText`。
     */
    private fun extractVariableNamesFromText(text: String): List<String> {
        val variableNames = mutableListOf<String>()
        val lines = text.split("\n")
        
        for (line in lines) {
            // 匹配 "变量名: xxx" 格式
            val match = Regex(msg("regex.variable.name.extract")).find(line)
            if (match != null) {
                variableNames.add(match.groupValues[1])
            }
        }
        
        return variableNames.distinct()
    }
    
    /**
     * 获取 `batchDeleteConfig`。
     */
    private fun getBatchDeleteConfig(): BatchDeleteConfig {
        return BatchDeleteConfig(
            procedureName = batchDeleteProcedureNameField.text,
            mainTableName = batchDeleteMainTableNameField.text,
            primaryKeyField = batchDeletePrimaryKeyField.text,
            timeField = batchDeleteTimeField.text,
            limitSize = batchDeleteLimitSizeSpinner.value as Int,
            minId = batchDeleteMinIdSpinner.value as Long,
            createTimeEnd = batchDeleteCreateTimeEndField.text,
            addLogTable = batchDeleteAddLogTableCheckBox.isSelected,
            addTempTable = batchDeleteAddTempTableCheckBox.isSelected,
            customWhereCondition = batchDeleteCustomWhereConditionField.text,
            procedureComment = batchDeleteProcedureCommentField.text
        )
    }
    
    /**
     * 校验 `batchDeleteConfig`。
     */
    private fun validateBatchDeleteConfig(config: BatchDeleteConfig): ValidationResult {
        if (config.procedureName.isBlank()) {
            return ValidationResult(false, msg("validation.procedure.name.required"))
        }
        
        if (config.mainTableName.isBlank()) {
            return ValidationResult(false, msg("validation.main.table.name.required"))
        }
        
        if (config.primaryKeyField.isBlank()) {
            return ValidationResult(false, msg("validation.primary.key.field.required"))
        }
        
        if (config.timeField.isBlank()) {
            return ValidationResult(false, msg("validation.time.field.required"))
        }
        
        if (config.limitSize <= 0) {
            return ValidationResult(false, msg("validation.delete.limit.gt.zero"))
        }
        
        if (config.createTimeEnd.isBlank()) {
            return ValidationResult(false, msg("validation.delete.end.time.required"))
        }
        
        return ValidationResult(true, msg("validation.config.ok"))
    }
    
    /**
     * 获取 `dynamicSqlConfig`。
     */
    private fun getDynamicSqlConfig(): DynamicSqlConfig {
        val ignoredVariables = dynamicSqlIgnoredVariablesField.text.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        return DynamicSqlConfig(
            originalSql = dynamicSqlTextArea.text,
            ignoredVariables = ignoredVariables,
            enableShardingSuffix = dynamicSqlEnableShardingCheckBox.isSelected,
            shardingSuffixFormat = dynamicSqlShardingSuffixFormatField.text,
            shardingSuffixVariableName = dynamicSqlShardingSuffixVariableField.text,
            sqlVariablePrefix = dynamicSqlVariablePrefixField.text,
            statementVariablePrefix = dynamicSqlStatementPrefixField.text
        )
    }
    
    /**
     * 校验 `dynamicSqlConfig`。
     */
    private fun validateDynamicSqlConfig(config: DynamicSqlConfig): ValidationResult {
        if (config.originalSql.isBlank()) {
            return ValidationResult(false, msg("validation.original.sql.required"))
        }
        
        if (config.sqlVariablePrefix.isBlank()) {
            return ValidationResult(false, msg("validation.sql.variable.prefix.required"))
        }
        
        if (config.statementVariablePrefix.isBlank()) {
            return ValidationResult(false, msg("validation.statement.variable.prefix.required"))
        }
        
        if (config.enableShardingSuffix) {
            if (config.shardingSuffixFormat.isBlank()) {
                return ValidationResult(false, msg("validation.sharding.suffix.format.required"))
            }
            
            if (config.shardingSuffixVariableName.isBlank()) {
                return ValidationResult(false, msg("validation.sharding.suffix.variable.required"))
            }
        }
        
        return ValidationResult(true, msg("validation.config.ok"))
    }
    
    /**
     * 更新 `shardingFieldVisibility`。
     */
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
    
    /**
     * 获取 `shardingConfig`。
     */
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
    
    /**
     * 获取 `stringBufferConfig`。
     */
    private fun getStringBufferConfig(): StringBufferConfig {
        return StringBufferConfig(
            variableName = stringBufferVariableNameField.text,
            language = stringBufferLanguageComboBox.selectedItem as CodeLanguage,
            originalSql = stringBufferSqlTextArea.text,
            addComments = stringBufferAddCommentsCheckBox.isSelected,
            formatCode = stringBufferFormatCodeCheckBox.isSelected
        )
    }
    
    /**
     * 校验 `shardingConfig`。
     */
    private fun validateShardingConfig(config: ShardingConfig): ValidationResult {
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
     * 校验 `stringBufferConfig`。
     */
    private fun validateStringBufferConfig(config: StringBufferConfig): ValidationResult {
        if (config.variableName.isBlank()) {
            return ValidationResult(false, msg("validation.variable.name.required"))
        }
        
        if (config.originalSql.isBlank()) {
            return ValidationResult(false, msg("validation.original.sql.required"))
        }
        
        return ValidationResult(true, msg("validation.config.ok"))
    }
    
    /**
     * 获取 `stringBufferPanel`。
     */
    fun getStringBufferPanel(): JComponent {
        return stingBufferPanel
    }
    /**
     * 获取 `shardingPanel`。
     */
    fun getShardingPanel() : JPanel {
        return shardingPanel!!
    }

    /**
     * 获取 `batchDeletePanel`。
     */
    fun getBatchDeletePanel() : JPanel {
        return batchDeletePanel!!
    }
    
    /**
     * 获取 `dynamicSqlPanel`。
     */
    fun getDynamicSqlPanel() : JPanel {
        return dynamicSqlPanel!!
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
