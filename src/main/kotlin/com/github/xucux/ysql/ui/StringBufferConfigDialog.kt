package com.github.xucux.ysql.ui

import com.github.xucux.ysql.models.CodeLanguage
import com.github.xucux.ysql.models.StringBufferConfig
import com.github.xucux.ysql.services.StringBufferService
import com.github.xucux.ysql.utils.I18nUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.openapi.ui.ComboBox
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

/**
 * StringBuffer配置对话框
 * 提供用户配置StringBuffer代码生成参数的界面
 */
class StringBufferConfigDialog(
    private val project: Project,
    private val initialSql: String = ""
) : DialogWrapper(project) {
    
    private val variableNameField = JBTextField("sql")
    private val languageComboBox = ComboBox(CodeLanguage.values())
    private val addCommentsCheckBox = JBCheckBox(msg("toolwindow.checkbox.add.comments"), false)
    private val formatCodeCheckBox = JBCheckBox(msg("toolwindow.checkbox.format.code"), false)
    private val sqlTextArea = JBTextArea(10, 50)
    
    private val previewButton = JButton(msg("dialog.string.buffer.config.preview"))
    private val previewTextArea = JBTextArea(8, 50)
    private val templateButton = JButton(msg("dialog.string.buffer.config.view.template"))
    
    init {
        title = msg("dialog.string.buffer.config.title")
        init()
        
        // 设置初始值
        sqlTextArea.text = initialSql
        sqlTextArea.lineWrap = true
        sqlTextArea.wrapStyleWord = true
        
        previewTextArea.isEditable = false
        previewTextArea.lineWrap = true
        previewTextArea.wrapStyleWord = true
        previewTextArea.font = java.awt.Font("Consolas", java.awt.Font.PLAIN, 11)
        
        // 设置事件监听器
        setupEventListeners()
    }
    
    /**
     * 设置 `upEventListeners`。
     */
    private fun setupEventListeners() {
        // 预览代码按钮
        previewButton.addActionListener {
            showPreview()
        }
        
        // 查看模板按钮
        templateButton.addActionListener {
            showTemplate()
        }
        
        // 语言变化时更新预览
        languageComboBox.addActionListener {
            showPreview()
        }
        
        // 其他配置变化时更新预览
        variableNameField.addActionListener { showPreview() }
        addCommentsCheckBox.addActionListener { showPreview() }
        formatCodeCheckBox.addActionListener { showPreview() }
        sqlTextArea.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            /**
             * 处理 `insertUpdate` 逻辑。
             */
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) { showPreview() }
            /**
             * 移除 `update`。
             */
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) { showPreview() }
            /**
             * 处理 `changedUpdate` 逻辑。
             */
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) { showPreview() }
        })
    }
    
    /**
     * 展示 `preview`。
     */
    private fun showPreview() {
        val config = getConfig()
        try {
            val stringBufferService = ApplicationManager.getApplication().getService(StringBufferService::class.java)
            val preview = stringBufferService.getCodePreview(config)
            previewTextArea.text = preview
        } catch (e: Exception) {
            previewTextArea.text = msg("message.preview.generate.failed", e.message ?: "")
        }
    }
    
    /**
     * 展示 `template`。
     */
    private fun showTemplate() {
        val selectedLanguage = languageComboBox.selectedItem as CodeLanguage
        try {
            val stringBufferService = ApplicationManager.getApplication().getService(StringBufferService::class.java)
            val template = stringBufferService.getCodeTemplate(selectedLanguage)
            
            val templateDialog = object : DialogWrapper(project) {
                init {
                    title = msg("dialog.string.buffer.config.code.template.title", selectedLanguage.displayName)
                    init()
                }
                
                /**
                 * 创建对话框主体面板。
                 */
                override fun createCenterPanel(): JComponent {
                    val textArea = JBTextArea(15, 60)
                    textArea.text = template
                    textArea.isEditable = false
                    textArea.font = java.awt.Font("Consolas", java.awt.Font.PLAIN, 12)
                    textArea.lineWrap = true
                    textArea.wrapStyleWord = true
                    
                    return JBScrollPane(textArea)
                }
                
                /**
                 * 创建当前对话框的操作列表。
                 */
                override fun createActions(): Array<Action> {
                    return arrayOf(cancelAction)
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
        
        // 创建配置面板
        val configPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(msg("toolwindow.label.variable.name"), variableNameField)
            .addLabeledComponent(msg("toolwindow.label.programming.language"), languageComboBox)
            .addComponent(addCommentsCheckBox)
            .addComponent(formatCodeCheckBox)
            .addSeparator()
            .addLabeledComponent(msg("toolwindow.label.original.sql"), JBScrollPane(sqlTextArea))
            .addComponent(previewButton)
            .addLabeledComponent(msg("dialog.string.buffer.config.code.preview"), JBScrollPane(previewTextArea))
            .addComponent(templateButton)
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
        val stringBufferService = ApplicationManager.getApplication().getService(StringBufferService::class.java)
        val validationResult = stringBufferService.validateConfig(config)
        
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
     * 获取 `config`。
     */
    fun getConfig(): StringBufferConfig {
        return StringBufferConfig(
            variableName = variableNameField.text,
            language = languageComboBox.selectedItem as CodeLanguage,
            originalSql = sqlTextArea.text,
            addComments = addCommentsCheckBox.isSelected,
            formatCode = formatCodeCheckBox.isSelected
        )
    }

    /**
     * 返回国际化消息文本。
     */
    private fun msg(key: String, vararg args: Any): String = I18nUtil.getMessage(key, *args)
}
