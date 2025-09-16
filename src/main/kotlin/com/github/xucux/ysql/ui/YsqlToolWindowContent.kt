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
import com.github.xucux.ysql.services.SqlShardingService
import com.github.xucux.ysql.services.ShardingStatisticsService
import com.github.xucux.ysql.services.StringBufferService
import com.github.xucux.ysql.services.TableNameExtractorService
import com.github.xucux.ysql.services.BatchDeleteService
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
    private val mainPanel = JPanel(BorderLayout())
    
    // 分表SQL解析相关组件
    private val shardingTableNamesField = JBTextField()
    private val shardingSuffixTypeComboBox = ComboBox(SuffixType.values())
    private val shardingSuffixFormatField = JBTextField().apply { text = "_" }
    private val shardingShardCountField = JSpinner(SpinnerNumberModel(16, 1, 1000, 1))
    private val shardingStartYearField = JSpinner(SpinnerNumberModel(2020, 1900, 2100, 1))
    private val shardingStartMonthField = JSpinner(SpinnerNumberModel(1, 1, 12, 1))
    private val shardingSqlTextArea = JBTextArea(8, 40)
    private val shardingExtractTableNamesButton = JButton("自动识别表名")
    private val shardingGenerateButton = JButton("生成分表SQL")
    private val shardingStatisticsButton = JButton("生成分表统计")
    private val shardingResultTextArea = JBTextArea(8, 40)
    
    // StringBuffer代码生成相关组件
    private val stringBufferVariableNameField = JBTextField().apply { text = "sql" }
    private val stringBufferLanguageComboBox = ComboBox(CodeLanguage.values())
    private val stringBufferAddCommentsCheckBox = JCheckBox("添加注释", false)
    private val stringBufferFormatCodeCheckBox = JCheckBox("格式化代码", false)
    private val stringBufferSqlTextArea = JBTextArea(8, 40)
    private val stringBufferGenerateButton = JButton("生成代码")
    private val stringBufferResultTextArea = JBTextArea(8, 40)
    
    // SQL反向解析相关组件
    private val reverseParseCodeTextArea = JBTextArea(8, 40)
    private val reverseParseLanguageComboBox = ComboBox(CodeLanguage.values())
    private val reverseParseAutoDetectCheckBox = JCheckBox("自动检测语言", true)
    private val reverseParseButton = JButton("反向解析SQL")
    private val reverseParseResultTextArea = JBTextArea(8, 40)
    
    // 批量删除存储过程相关组件
    private val batchDeleteTemplateComboBox = ComboBox(BatchDeleteTemplate.values())
    private val batchDeleteApplyTemplateButton = JButton("应用模板")
    private val batchDeleteProcedureNameField = JBTextField("DropHistoryDataByLimit")
    private val batchDeleteMainTableNameField = JBTextField("system_logs")
    private val batchDeletePrimaryKeyField = JBTextField("id")
    private val batchDeleteTimeField = JBTextField("create_time")
    private val batchDeleteLimitSizeSpinner = JSpinner(SpinnerNumberModel(1000, 1, 100000, 100))
    private val batchDeleteMinIdSpinner = JSpinner(SpinnerNumberModel(0L, 0L, Long.MAX_VALUE, 1L))
    private val batchDeleteCreateTimeEndField = JBTextField("2023-01-01 00:00:00")
    private val batchDeleteAddLogTableCheckBox = JCheckBox("添加日志表", true)
    private val batchDeleteAddTempTableCheckBox = JCheckBox("添加临时操作表", true)
    private val batchDeleteCustomWhereConditionField = JBTextField()
    private val batchDeleteProcedureCommentField = JBTextField("循环删除历史数据-根据limit查询主键防止临时表，再联表删除")
    private val batchDeleteGenerateButton = JButton("生成存储过程")
    private val batchDeleteResultTextArea = JBTextArea(8, 40)
    
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
        
        // 创建SQL反向解析面板
        val reverseParsePanel = createReverseParsePanel()
        
        // 创建批量删除存储过程面板
        val batchDeletePanel = createBatchDeletePanel()
        
        // 添加标签页
        tabbedPane.addTab("分表SQL解析",  shardingPanel)
        tabbedPane.addTab("StringBuffer代码生成",  stringBufferPanel)
        tabbedPane.addTab("SQL反向解析",  reverseParsePanel)
        tabbedPane.addTab("批量删除存储过程",batchDeletePanel)
        
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
        
        // 创建按钮面板
        val buttonPanel = JPanel()
        buttonPanel.add(shardingGenerateButton)
        buttonPanel.add(shardingStatisticsButton)
        
        // 创建SQL输入面板
        val sqlInputPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("原始SQL:", JBScrollPane(shardingSqlTextArea))
            .addComponent(buttonPanel)
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
    
    private fun createReverseParsePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // 创建配置面板
        val configPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("编程语言:", reverseParseLanguageComboBox)
            .addComponent(reverseParseAutoDetectCheckBox)
            .panel
        
        // 创建反向解析面板
        val parsePanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("StringBuffer代码:", JBScrollPane(reverseParseCodeTextArea))
            .addComponent(reverseParseButton)
            .addLabeledComponent("解析结果:", JBScrollPane(reverseParseResultTextArea))
            .panel
        
        // 创建分割面板
        val splitter = JBSplitter(true, 0.3f)
        splitter.firstComponent = configPanel
        splitter.secondComponent = parsePanel
        
        panel.add(splitter, BorderLayout.CENTER)
        return panel
    }
    
    private fun createBatchDeletePanel(): JPanel {
        val panel = JPanel(BorderLayout())
        
        // 创建模板选择面板
        val templatePanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("选择模板:", batchDeleteTemplateComboBox)
            .addComponent(batchDeleteApplyTemplateButton)
            .panel
        
        // 创建基础配置面板
        val basicConfigPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("存储过程名称:", batchDeleteProcedureNameField)
            .addLabeledComponent("主表名:", batchDeleteMainTableNameField)
            .addLabeledComponent("主键字段名:", batchDeletePrimaryKeyField)
            .addLabeledComponent("时间字段名:", batchDeleteTimeField)
            .panel
        
        // 创建参数配置面板
        val paramConfigPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("每次删除行数:", batchDeleteLimitSizeSpinner)
            .addLabeledComponent("起始主键值:", batchDeleteMinIdSpinner)
            .addLabeledComponent("删除截至时间:", batchDeleteCreateTimeEndField)
            .panel
        
        // 创建高级配置面板
        val advancedConfigPanel = FormBuilder.createFormBuilder()
            .addComponent(batchDeleteAddLogTableCheckBox)
            .addComponent(batchDeleteAddTempTableCheckBox)
            .addLabeledComponent("自定义WHERE条件:", batchDeleteCustomWhereConditionField)
            .addLabeledComponent("存储过程注释:", batchDeleteProcedureCommentField)
            .panel
        
        // 创建配置面板（左侧）
        val configPanel = JPanel(BorderLayout())
        val configTabbedPane = JBTabbedPane()
        configTabbedPane.addTab("模板选择", templatePanel)
        configTabbedPane.addTab("基础配置", basicConfigPanel)
        configTabbedPane.addTab("参数配置", paramConfigPanel)
        configTabbedPane.addTab("高级配置", advancedConfigPanel)
        configPanel.add(configTabbedPane, BorderLayout.CENTER)
        
        // 创建生成面板（右侧）
        val generatePanel = FormBuilder.createFormBuilder()
            .addComponent(batchDeleteGenerateButton)
            .addLabeledComponent("生成结果:", JBScrollPane(batchDeleteResultTextArea))
            .panel
        
        // 创建分割面板
        val splitter = JBSplitter(true, 0.4f)
        splitter.firstComponent = configPanel
        splitter.secondComponent = generatePanel
        
        panel.add(splitter, BorderLayout.CENTER)
        return panel
    }
    
    private fun setupEventListeners() {
        // 分表SQL解析事件监听器
        setupShardingEventListeners()
        
        // StringBuffer代码生成事件监听器
        setupStringBufferEventListeners()
        
        // SQL反向解析事件监听器
        setupReverseParseEventListeners()
        
        // 批量删除存储过程事件监听器
        setupBatchDeleteEventListeners()
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
    
    private fun setupStringBufferEventListeners() {
        
        // 生成代码按钮
        stringBufferGenerateButton.addActionListener {
            generateStringBufferCode()
        }
    }
    
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
    
    private fun extractTableNames() {
        val sql = shardingSqlTextArea.text.trim()
        if (sql.isBlank()) {
            Messages.showInfoMessage("请先输入SQL语句", "提示")
            return
        }
        
        try {
            val extractorService = ApplicationManager.getApplication().getService(TableNameExtractorService::class.java)
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
                val shardingService = ApplicationManager.getApplication().getService(SqlShardingService::class.java)
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
    
    private fun generateShardingStatistics() {
        // 获取当前配置
        val currentConfig = getShardingConfig()
        
        // 验证基础配置
        val validationResult = validateShardingConfig(currentConfig)
        if (!validationResult.isValid) {
            Messages.showErrorDialog(validationResult.message, "配置错误")
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
                            Messages.showErrorDialog("生成分表统计SQL失败：${result.errorMessage}", "错误")
                        }
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog("生成分表统计SQL时发生异常：${e.message}", "异常")
                    }
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
                val stringBufferService = ApplicationManager.getApplication().getService(StringBufferService::class.java)
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
    
    private fun reverseParseSql() {
        val code = reverseParseCodeTextArea.text.trim()
        if (code.isBlank()) {
            Messages.showInfoMessage("请先输入StringBuffer/StringBuilder代码", "提示")
            return
        }
        
        // 检查是否包含StringBuffer/StringBuilder
        val stringBufferService = ApplicationManager.getApplication().getService(StringBufferService::class.java)
        if (!stringBufferService.containsStringBuffer(code)) {
            Messages.showWarningDialog(
                "输入的代码中未找到StringBuffer或StringBuilder语句，请检查代码格式。",
                "警告"
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
                        Messages.showErrorDialog("反向解析SQL失败：${result.errorMessage}", "错误")
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog("反向解析SQL时发生异常：${e.message}", "异常")
                }
            }
        }
    }
    
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
        
        Messages.showInfoMessage("模板配置已应用，请根据需要调整参数", "模板应用成功")
    }
    
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
    
    private fun generateBatchDeleteProcedure() {
        val config = getBatchDeleteConfig()
        val validationResult = validateBatchDeleteConfig(config)
        
        if (!validationResult.isValid) {
            Messages.showErrorDialog(validationResult.message, "配置错误")
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
                        Messages.showErrorDialog("生成批量删除存储过程失败：${result.errorMessage}", "错误")
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog("生成批量删除存储过程时发生异常：${e.message}", "异常")
                }
            }
        }
    }
    
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
    
    private fun validateBatchDeleteConfig(config: BatchDeleteConfig): ValidationResult {
        if (config.procedureName.isBlank()) {
            return ValidationResult(false, "存储过程名称不能为空")
        }
        
        if (config.mainTableName.isBlank()) {
            return ValidationResult(false, "主表名不能为空")
        }
        
        if (config.primaryKeyField.isBlank()) {
            return ValidationResult(false, "主键字段名不能为空")
        }
        
        if (config.timeField.isBlank()) {
            return ValidationResult(false, "时间字段名不能为空")
        }
        
        if (config.limitSize <= 0) {
            return ValidationResult(false, "每次删除行数必须大于0")
        }
        
        if (config.createTimeEnd.isBlank()) {
            return ValidationResult(false, "删除截至时间不能为空")
        }
        
        return ValidationResult(true, "配置验证通过")
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
