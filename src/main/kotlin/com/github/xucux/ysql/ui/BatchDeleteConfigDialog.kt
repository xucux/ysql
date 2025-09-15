package com.github.xucux.ysql.ui

import com.github.xucux.ysql.models.BatchDeleteConfig
import com.github.xucux.ysql.models.BatchDeleteTemplate
import com.github.xucux.ysql.services.BatchDeleteService
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.SpinnerNumberModel

/**
 * 批量删除存储过程配置对话框
 * 提供用户配置批量删除存储过程生成参数的界面
 */
class BatchDeleteConfigDialog(
    private val project: Project
) : DialogWrapper(project) {
    
    // 基础配置字段
    private val procedureNameField = JBTextField("DropHistoryDataByLimit")
    private val mainTableNameField = JBTextField("system_logs")
    private val primaryKeyField = JBTextField("id")
    private val timeField = JBTextField("create_time")
    
    // 存储过程参数字段
    private val limitSizeSpinner = JSpinner(SpinnerNumberModel(1000, 1, 100000, 100))
    private val minIdSpinner = JSpinner(SpinnerNumberModel(0L, 0L, Long.MAX_VALUE, 1L))
    private val createTimeEndField = JBTextField("2023-01-01 00:00:00")
    
    // 高级配置字段
    private val addLogTableCheckBox = JBCheckBox("添加日志表", true)
    private val addTempTableCheckBox = JBCheckBox("添加临时操作表", true)
    private val customWhereConditionField = JBTextField()
    private val procedureCommentField = JBTextField("循环删除历史数据-根据limit查询主键防止临时表，再联表删除")
    
    // 模板选择组件
    private val templateComboBox = JComboBox(BatchDeleteTemplate.values())
    private val applyTemplateButton = JButton("应用模板")
    private val templateDescriptionArea = JBTextArea(4, 50)
    
    // 预览相关组件
    private val previewButton = JButton("预览存储过程")
    private val previewTextArea = JBTextArea(15, 60)
    private val templateButton = JButton("查看模板")
    
    init {
        title = "批量删除存储过程生成配置"
        init()
        
        // 设置文本区域属性
        previewTextArea.isEditable = false
        previewTextArea.lineWrap = true
        previewTextArea.wrapStyleWord = true
        previewTextArea.font = java.awt.Font("Consolas", java.awt.Font.PLAIN, 11)
        
        // 设置模板描述区域属性
        templateDescriptionArea.isEditable = false
        templateDescriptionArea.lineWrap = true
        templateDescriptionArea.wrapStyleWord = true
        templateDescriptionArea.font = java.awt.Font("Dialog", java.awt.Font.PLAIN, 11)
        
        // 设置事件监听器
        setupEventListeners()
    }
    
    private fun setupEventListeners() {
        // 模板选择变化事件
        templateComboBox.addActionListener {
            updateTemplateDescription()
        }
        
        // 应用模板按钮
        applyTemplateButton.addActionListener {
            applySelectedTemplate()
        }
        
        // 预览存储过程按钮
        previewButton.addActionListener {
            showPreview()
        }
        
        // 查看模板按钮
        templateButton.addActionListener {
            showTemplate()
        }
        
        // 表名变化时自动生成存储过程名
        mainTableNameField.addActionListener {
            autoGenerateProcedureName()
        }
        
        // 配置变化时更新预览
        procedureNameField.addActionListener { showPreview() }
        mainTableNameField.addActionListener { showPreview() }
        primaryKeyField.addActionListener { showPreview() }
        timeField.addActionListener { showPreview() }
        limitSizeSpinner.addChangeListener { showPreview() }
        minIdSpinner.addChangeListener { showPreview() }
        createTimeEndField.addActionListener { showPreview() }
        addLogTableCheckBox.addActionListener { showPreview() }
        addTempTableCheckBox.addActionListener { showPreview() }
        customWhereConditionField.addActionListener { showPreview() }
        procedureCommentField.addActionListener { showPreview() }
        
        // 初始化模板描述
        updateTemplateDescription()
    }
    
    private fun updateTemplateDescription() {
        val selectedTemplate = templateComboBox.selectedItem as BatchDeleteTemplate
        templateDescriptionArea.text = selectedTemplate.description
    }
    
    private fun applySelectedTemplate() {
        val selectedTemplate = templateComboBox.selectedItem as BatchDeleteTemplate
        val templateConfig = selectedTemplate.config
        
        // 应用模板配置到各个字段
        procedureNameField.text = templateConfig.procedureName
        mainTableNameField.text = templateConfig.mainTableName
        primaryKeyField.text = templateConfig.primaryKeyField
        timeField.text = templateConfig.timeField
        limitSizeSpinner.value = templateConfig.limitSize
        minIdSpinner.value = templateConfig.minId
        createTimeEndField.text = templateConfig.createTimeEnd
        addLogTableCheckBox.isSelected = templateConfig.addLogTable
        addTempTableCheckBox.isSelected = templateConfig.addTempTable
        customWhereConditionField.text = templateConfig.customWhereCondition
        procedureCommentField.text = templateConfig.procedureComment
        
        // 更新预览
        showPreview()
        
        // 显示应用成功消息
        JOptionPane.showMessageDialog(
            this.contentPanel,
            "模板配置已应用，请根据需要调整参数",
            "模板应用成功",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    private fun autoGenerateProcedureName() {
        val tableName = mainTableNameField.text.trim()
        if (tableName.isNotBlank() && procedureNameField.text == "DropHistoryDataByLimit") {
            try {
                val batchDeleteService = ServiceManager.getService(BatchDeleteService::class.java)
                val defaultName = batchDeleteService.generateDefaultProcedureName(tableName)
                procedureNameField.text = defaultName
            } catch (e: Exception) {
                // 忽略异常，保持当前名称
            }
        }
    }
    
    private fun showPreview() {
        val config = getConfig()
        try {
            val batchDeleteService = ServiceManager.getService(BatchDeleteService::class.java)
            val result = batchDeleteService.generateBatchDeleteProcedure(config)
            if (result.success) {
                previewTextArea.text = result.generatedProcedure
            } else {
                previewTextArea.text = "预览生成失败：${result.errorMessage}"
            }
        } catch (e: Exception) {
            previewTextArea.text = "预览生成失败：${e.message}"
        }
    }
    
    private fun showTemplate() {
        try {
            val selectedTemplate = templateComboBox.selectedItem as BatchDeleteTemplate
            val batchDeleteService = ServiceManager.getService(BatchDeleteService::class.java)
            
            // 生成模板的存储过程预览
            val result = batchDeleteService.generateBatchDeleteProcedure(selectedTemplate.config)
            val templateContent = if (result.success) {
                buildString {
                    appendLine("=== ${selectedTemplate.displayName} 模板详情 ===")
                    appendLine()
                    appendLine(selectedTemplate.getDetailedDescription())
                    appendLine()
                    appendLine("=== 使用建议 ===")
                    appendLine(selectedTemplate.getUsageSuggestion())
                    appendLine()
                    appendLine("=== 生成的存储过程 ===")
                    appendLine(result.generatedProcedure)
                }
            } else {
                "模板预览生成失败：${result.errorMessage}"
            }
            
            val templateDialog = object : DialogWrapper(project) {
                init {
                    title = "${selectedTemplate.displayName} 模板详情"
                    init()
                }
                
                override fun createCenterPanel(): JComponent {
                    val textArea = JBTextArea(20, 80)
                    textArea.text = templateContent
                    textArea.isEditable = false
                    textArea.font = java.awt.Font("Consolas", java.awt.Font.PLAIN, 11)
                    textArea.lineWrap = true
                    textArea.wrapStyleWord = true
                    
                    return JBScrollPane(textArea)
                }
                
                override fun createActions(): Array<Action> {
                    val applyTemplateAction = object : AbstractAction("应用此模板") {
                        override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                            applySelectedTemplate()
                            close(0)
                        }
                    }
                    
                    return arrayOf(applyTemplateAction, cancelAction)
                }
            }
            
            templateDialog.show()
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                    this.contentPanel,
                "获取模板失败：${e.message}",
                "错误",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
    
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        
        // 创建模板选择面板
        val templatePanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("选择模板:", templateComboBox)
            .addComponent(applyTemplateButton)
            .addLabeledComponent("模板描述:", JBScrollPane(templateDescriptionArea))
            .panel
        
        // 创建基础配置面板
        val basicConfigPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("存储过程名称:", procedureNameField)
            .addLabeledComponent("主表名:", mainTableNameField)
            .addLabeledComponent("主键字段名:", primaryKeyField)
            .addLabeledComponent("时间字段名:", timeField)
            .panel
        
        // 创建参数配置面板
        val paramConfigPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("每次删除行数:", limitSizeSpinner)
            .addLabeledComponent("起始主键值:", minIdSpinner)
            .addLabeledComponent("删除截至时间:", createTimeEndField)
            .panel
        
        // 创建高级配置面板
        val advancedConfigPanel = FormBuilder.createFormBuilder()
            .addComponent(addLogTableCheckBox)
            .addComponent(addTempTableCheckBox)
            .addLabeledComponent("自定义WHERE条件:", customWhereConditionField)
            .addLabeledComponent("存储过程注释:", procedureCommentField)
            .panel
        
        // 创建预览面板
        val previewPanel = FormBuilder.createFormBuilder()
            .addComponent(previewButton)
            .addLabeledComponent("存储过程预览:", JBScrollPane(previewTextArea))
            .addComponent(templateButton)
            .panel
        
        // 创建标签页
        val tabbedPane = JTabbedPane()
        tabbedPane.addTab("模板选择", templatePanel)
        tabbedPane.addTab("基础配置", basicConfigPanel)
        tabbedPane.addTab("参数配置", paramConfigPanel)
        tabbedPane.addTab("高级配置", advancedConfigPanel)
        tabbedPane.addTab("预览", previewPanel)
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER)
        
        // 设置面板大小
        mainPanel.preferredSize = Dimension(700, 600)
        
        return mainPanel
    }
    
    override fun createActions(): Array<Action> {
        return arrayOf(okAction, cancelAction)
    }
    
    override fun doOKAction() {
        // 验证配置
        val config = getConfig()
        val batchDeleteService = ServiceManager.getService(BatchDeleteService::class.java)
        
        // 验证存储过程名称
        val procedureNameValidation = batchDeleteService.validateProcedureName(config.procedureName)
        if (!procedureNameValidation.isValid) {
            JOptionPane.showMessageDialog(
                    this.contentPanel,
                procedureNameValidation.message,
                "配置错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        // 验证表名
        val tableNameValidation = batchDeleteService.validateTableName(config.mainTableName)
        if (!tableNameValidation.isValid) {
            JOptionPane.showMessageDialog(
                    this.contentPanel,
                tableNameValidation.message,
                "配置错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        // 验证字段名
        val primaryKeyValidation = batchDeleteService.validateFieldName(config.primaryKeyField)
        if (!primaryKeyValidation.isValid) {
            JOptionPane.showMessageDialog(
                    this.contentPanel,
                primaryKeyValidation.message,
                "配置错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        val timeFieldValidation = batchDeleteService.validateFieldName(config.timeField)
        if (!timeFieldValidation.isValid) {
            JOptionPane.showMessageDialog(
                    this.contentPanel,
                timeFieldValidation.message,
                "配置错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        // 验证时间格式
        val timeValidation = batchDeleteService.validateTimeFormat(config.createTimeEnd)
        if (!timeValidation.isValid) {
            JOptionPane.showMessageDialog(
                    this.contentPanel,
                timeValidation.message,
                "配置错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        super.doOKAction()
    }
    
    fun getConfig(): BatchDeleteConfig {
        return BatchDeleteConfig(
            procedureName = procedureNameField.text,
            mainTableName = mainTableNameField.text,
            primaryKeyField = primaryKeyField.text,
            timeField = timeField.text,
            limitSize = limitSizeSpinner.value as Int,
            minId = minIdSpinner.value as Long,
            createTimeEnd = createTimeEndField.text,
            addLogTable = addLogTableCheckBox.isSelected,
            addTempTable = addTempTableCheckBox.isSelected,
            customWhereCondition = customWhereConditionField.text,
            procedureComment = procedureCommentField.text
        )
    }
}
