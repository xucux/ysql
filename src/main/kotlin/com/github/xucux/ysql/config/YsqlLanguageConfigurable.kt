package com.github.xucux.ysql.config

import com.github.xucux.ysql.utils.I18nUtil
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.util.Locale
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * 提供 `YsqlLanguageConfigurable` 相关配置界面。
 */
class YsqlLanguageConfigurable : Configurable {
    private lateinit var languageComboBox: JComboBox<LanguageOption>
    private lateinit var component: JComponent

    /**
     * 返回当前配置项的显示名称。
     */
    override fun getDisplayName(): String = I18nUtil.getMessage("settings.display.name")

    /**
     * 创建配置界面组件。
     */
    override fun createComponent(): JComponent {
        languageComboBox = JComboBox(LanguageOption.entries.toTypedArray())
        val formPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
            add(JLabel(I18nUtil.getMessage("settings.language.label")))
            add(languageComboBox)
        }
        component = JPanel(BorderLayout(0, 8)).apply {
            add(formPanel, BorderLayout.NORTH)
            toolTipText = I18nUtil.getMessage("settings.language.note")
        }
        reset()
        return component
    }

    /**
     * 判断当前配置是否已修改。
     */
    override fun isModified(): Boolean {
        val selected = languageComboBox.selectedItem as? LanguageOption ?: return false
        return selected.languageTag != YsqlPropertiesConfig.getLanguageTag()
    }

    /**
     * 应用当前配置。
     */
    override fun apply() {
        val selected = languageComboBox.selectedItem as? LanguageOption ?: return
        YsqlPropertiesConfig.setLanguageTag(selected.languageTag)
        I18nUtil.setLanguage(selected.toLocale())
        ProjectManager.getInstance().openProjects.forEach(LanguageUiRefresher::refreshProject)
    }

    /**
     * 重置当前配置界面。
     */
    override fun reset() {
        val currentTag = YsqlPropertiesConfig.getLanguageTag()
        val option = LanguageOption.fromLanguageTag(currentTag)
        languageComboBox.selectedItem = option
    }

    /**
     * 封装 `LanguageOption` 相关逻辑。
     */
    private enum class LanguageOption(val languageTag: String, private val displayName: String) {
        ENGLISH("en", "English"),
        CHINESE_SIMPLIFIED("zh-CN", "简体中文");

        /**
         * 返回当前对象的显示文本。
         */
        override fun toString(): String = displayName

        /**
         * 转换为 Locale 对象。
         */
        fun toLocale(): Locale {
            return if (this == CHINESE_SIMPLIFIED) Locale.SIMPLIFIED_CHINESE else Locale.ENGLISH
        }

        companion object {
            /**
             * 处理 `fromLanguageTag` 逻辑。
             */
            fun fromLanguageTag(languageTag: String): LanguageOption {
                return entries.firstOrNull { it.languageTag.equals(languageTag, ignoreCase = true) } ?: ENGLISH
            }
        }
    }
}
