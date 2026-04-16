package com.github.xucux.ysql.ui

import com.github.xucux.ysql.models.BatchDeleteConfig
import com.github.xucux.ysql.models.BatchDeleteTemplate
import com.github.xucux.ysql.services.BatchDeleteService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBTabbedPane
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.SpinnerNumberModel
import com.github.xucux.ysql.utils.I18nUtil

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
    private val addLogTableCheckBox = JBCheckBox(msg("toolwindow.checkbox.add.log.table"), true)
    private val addTempTableCheckBox = JBCheckBox(msg("toolwindow.checkbox.add.temp.table"), true)
    private val customWhereConditionField = JBTextField()
    private val procedureCommentField = JBTextField(msg("toolwindow.default.procedure.comment"))
    
    // 模板选择组件
    private val templateComboBox = ComboBox(BatchDeleteTemplate.values())
    private val applyTemplateButton = JButton(msg("dialog.batch.delete.config.button.apply.template"))
    private val templateDescriptionArea = JBTextArea(4, 50)
    
    // 预览相关组件
    private val previewButton = JButton(msg("dialog.batch.delete.config.button.preview"))
    private val previewTextArea = JBTextArea(15, 60)
    private val templateButton = JButton(msg("dialog.batch.delete.config.button.view.template"))
    
    init {
        title = msg("dialog.batch.delete.config.title")
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
    
    /**
     * 设置 `upEventListeners`。
     */
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
    
    /**
     * 更新 `templateDescription`。
     */
    private fun updateTemplateDescription() {
        val selectedTemplate = templateComboBox.selectedItem as BatchDeleteTemplate
        templateDescriptionArea.text = selectedTemplate.description
    }
    
    /**
     * 处理 `applySelectedTemplate` 逻辑。
     */
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
            msg("dialog.batch.delete.config.message.template.applied"),
            msg("dialog.batch.delete.config.message.template.applied.title"),
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    /**
     * 处理 `autoGenerateProcedureName` 逻辑。
     */
    private fun autoGenerateProcedureName() {
        val tableName = mainTableNameField.text.trim()
        if (tableName.isNotBlank() && procedureNameField.text == "DropHistoryDataByLimit") {
            try {
                val batchDeleteService = ApplicationManager.getApplication().getService(BatchDeleteService::class.java)
                val defaultName = batchDeleteService.generateDefaultProcedureName(tableName)
                procedureNameField.text = defaultName
            } catch (e: Exception) {
                // 忽略异常，保持当前名称
            }
        }
    }
    
    /**
     * 展示 `preview`。
     */
    private fun showPreview() {
        val config = getConfig()
        try {
            val batchDeleteService = ApplicationManager.getApplication().getService(BatchDeleteService::class.java)
            val result = batchDeleteService.generateBatchDeleteProcedure(config)
            if (result.success) {
                previewTextArea.text = result.generatedProcedure
            } else {
                previewTextArea.text = msg("message.preview.generate.failed", result.errorMessage ?: "")
            }
        } catch (e: Exception) {
            previewTextArea.text = msg("message.preview.generate.failed", e.message ?: "")
        }
    }
    
    /**
     * 展示 `template`。
     */
    private fun showTemplate() {
        try {
            val selectedTemplate = templateComboBox.selectedItem as BatchDeleteTemplate
            val batchDeleteService = ApplicationManager.getApplication().getService(BatchDeleteService::class.java)
            
            // 生成模板的存储过程预览
            val result = batchDeleteService.generateBatchDeleteProcedure(selectedTemplate.config)
            val templateContent = if (result.success) {
                buildString {
                    appendLine(msg("dialog.batch.delete.config.template.detail.header", selectedTemplate.displayName))
                    appendLine()
                    appendLine(selectedTemplate.getDetailedDescription())
                    appendLine()
                    appendLine(msg("dialog.batch.delete.config.template.usage.hint.header"))
                    appendLine(selectedTemplate.getUsageSuggestion())
                    appendLine()
                    appendLine(msg("dialog.batch.delete.config.template.generated.procedure.header"))
                    appendLine(result.generatedProcedure)
                }
            } else {
                msg("message.template.preview.generate.failed", result.errorMessage ?: "")
            }
            
            val templateDialog = object : DialogWrapper(project) {
                init {
                    title = msg("dialog.batch.delete.config.template.detail.title", selectedTemplate.displayName)
                    init()
                }
                
                /**
                 * 创建对话框主体面板。
                 */
                override fun createCenterPanel(): JComponent {
                    val textArea = JBTextArea(20, 80)
                    textArea.text = templateContent
                    textArea.isEditable = false
                    textArea.font = java.awt.Font("Consolas", java.awt.Font.PLAIN, 11)
                    textArea.lineWrap = true
                    textArea.wrapStyleWord = true
                    
                    return JBScrollPane(textArea)
                }
                
                /**
                 * 创建当前对话框的操作列表。
                 */
                override fun createActions(): Array<Action> {
                    val applyTemplateAction = object : AbstractAction(msg("dialog.batch.delete.config.template.action.apply.this.template")) {
                        /**
                         * 执行当前动作。
                         */
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
                msg("message.template.get.failed", e.message ?: ""),
                msg("dialog.title.error"),
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
    
    /**
     * 创建对话框主体面板。
     */
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        
        // 创建模板选择面板
        val templatePanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("dialog.batch.delete.config.label.select.template"), templateComboBox)
            .addComponent(applyTemplateButton)
            .addLabeledComponent(msg("dialog.batch.delete.config.label.template.description"), JBScrollPane(templateDescriptionArea))
            .panel
        
        // 创建基础配置面板
        val basicConfigPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("dialog.batch.delete.config.label.procedure.name"), procedureNameField)
            .addLabeledComponent(msg("dialog.batch.delete.config.label.main.table.name"), mainTableNameField)
            .addLabeledComponent(msg("dialog.batch.delete.config.label.primary.key.field"), primaryKeyField)
            .addLabeledComponent(msg("dialog.batch.delete.config.label.time.field"), timeField)
            .panel
        
        // 创建参数配置面板
        val paramConfigPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("dialog.batch.delete.config.label.delete.limit"), limitSizeSpinner)
            .addLabeledComponent(msg("dialog.batch.delete.config.label.min.id"), minIdSpinner)
            .addLabeledComponent(msg("dialog.batch.delete.config.label.delete.end.time"), createTimeEndField)
            .panel
        
        // 创建高级配置面板
        val advancedConfigPanel = FormBuilder.createFormBuilder()
            .addComponent(addLogTableCheckBox)
            .addComponent(addTempTableCheckBox)
            .addLabeledComponent(msg("dialog.batch.delete.config.label.custom.where"), customWhereConditionField)
            .addLabeledComponent(msg("dialog.batch.delete.config.label.procedure.comment"), procedureCommentField)
            .panel
        
        // 创建预览面板
        val previewPanel = FormBuilder.createFormBuilder()
            .addComponent(previewButton)
            .addLabeledComponent(msg("dialog.batch.delete.config.label.procedure.preview"), JBScrollPane(previewTextArea))
            .addComponent(templateButton)
            .panel
        
        // 创建标签页
        val tabbedPane = JBTabbedPane()
        tabbedPane.addTab(msg("dialog.batch.delete.config.tab.select.template"), templatePanel)
        tabbedPane.addTab(msg("dialog.batch.delete.config.tab.basic.config"), basicConfigPanel)
        tabbedPane.addTab(msg("dialog.batch.delete.config.tab.param.config"), paramConfigPanel)
        tabbedPane.addTab(msg("dialog.batch.delete.config.tab.advanced.config"), advancedConfigPanel)
        tabbedPane.addTab(msg("dialog.batch.delete.config.tab.preview"), previewPanel)
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER)
        
        // 设置面板大小
        mainPanel.preferredSize = Dimension(700, 600)
        
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
        val batchDeleteService = ApplicationManager.getApplication().getService(BatchDeleteService::class.java)
        
        // 验证存储过程名称
        val procedureNameValidation = batchDeleteService.validateProcedureName(config.procedureName)
        if (!procedureNameValidation.isValid) {
            JOptionPane.showMessageDialog(
                    this.contentPanel,
                procedureNameValidation.message,
                msg("dialog.title.config.error"),
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
                msg("dialog.title.config.error"),
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
                msg("dialog.title.config.error"),
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        val timeFieldValidation = batchDeleteService.validateFieldName(config.timeField)
        if (!timeFieldValidation.isValid) {
            JOptionPane.showMessageDialog(
                    this.contentPanel,
                timeFieldValidation.message,
                msg("dialog.title.config.error"),
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
                msg("dialog.title.config.error"),
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        super.doOKAction()
    }
    
    /**
     * 获取 `config`。
     */
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

    /**
     * 返回国际化消息文本。
     */
    private fun msg(key: String, vararg args: Any): String = I18nUtil.getMessage(key, *args)
}
