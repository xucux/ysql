package com.github.xucux.ysql.ui

import com.github.xucux.ysql.models.CodeLanguage
import com.github.xucux.ysql.models.StringBufferConfig
import com.github.xucux.ysql.services.StringBufferService
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
    private val addCommentsCheckBox = JBCheckBox("添加注释", false)
    private val formatCodeCheckBox = JBCheckBox("格式化代码", false)
    private val sqlTextArea = JBTextArea(10, 50)
    
    private val previewButton = JButton("预览代码")
    private val previewTextArea = JBTextArea(8, 50)
    private val templateButton = JButton("查看模板")
    
    init {
        title = "StringBuffer代码生成配置"
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
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) { showPreview() }
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) { showPreview() }
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) { showPreview() }
        })
    }
    
    private fun showPreview() {
        val config = getConfig()
        try {
            val stringBufferService = ApplicationManager.getApplication().getService(StringBufferService::class.java)
            val preview = stringBufferService.getCodePreview(config)
            previewTextArea.text = preview
        } catch (e: Exception) {
            previewTextArea.text = "预览生成失败：${e.message}"
        }
    }
    
    private fun showTemplate() {
        val selectedLanguage = languageComboBox.selectedItem as CodeLanguage
        try {
            val stringBufferService = ApplicationManager.getApplication().getService(StringBufferService::class.java)
            val template = stringBufferService.getCodeTemplate(selectedLanguage)
            
            val templateDialog = object : DialogWrapper(project) {
                init {
                    title = "${selectedLanguage.displayName} 代码模板"
                    init()
                }
                
                override fun createCenterPanel(): JComponent {
                    val textArea = JBTextArea(15, 60)
                    textArea.text = template
                    textArea.isEditable = false
                    textArea.font = java.awt.Font("Consolas", java.awt.Font.PLAIN, 12)
                    textArea.lineWrap = true
                    textArea.wrapStyleWord = true
                    
                    return JBScrollPane(textArea)
                }
                
                override fun createActions(): Array<Action> {
                    return arrayOf(cancelAction)
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
        
        // 创建配置面板
        val configPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("变量名称:", variableNameField)
            .addLabeledComponent("编程语言:", languageComboBox)
            .addComponent(addCommentsCheckBox)
            .addComponent(formatCodeCheckBox)
            .addSeparator()
            .addLabeledComponent("原始SQL:", JBScrollPane(sqlTextArea))
            .addComponent(previewButton)
            .addLabeledComponent("代码预览:", JBScrollPane(previewTextArea))
            .addComponent(templateButton)
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
        val stringBufferService = ApplicationManager.getApplication().getService(StringBufferService::class.java)
        val validationResult = stringBufferService.validateConfig(config)
        
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
    
    fun getConfig(): StringBufferConfig {
        return StringBufferConfig(
            variableName = variableNameField.text,
            language = languageComboBox.selectedItem as CodeLanguage,
            originalSql = sqlTextArea.text,
            addComments = addCommentsCheckBox.isSelected,
            formatCode = formatCodeCheckBox.isSelected
        )
    }
}
