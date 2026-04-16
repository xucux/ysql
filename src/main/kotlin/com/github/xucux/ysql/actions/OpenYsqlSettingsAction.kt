package com.github.xucux.ysql.actions

import com.github.xucux.ysql.utils.I18nUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.options.ShowSettingsUtil

/**
 * 打开插件 Settings 配置页（用于齿轮菜单入口）。
 */
class OpenYsqlSettingsAction : AnAction(
    I18nUtil.getMessage("action.open.ysql.settings.text"),
    I18nUtil.getMessage("action.open.ysql.settings.description"),
    null
), DumbAware {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        // 与 plugin.xml 中 <applicationConfigurable id="com.github.xucux.ysql.settings.language" .../> 保持一致
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "com.github.xucux.ysql.settings.language")
    }

    override fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }
}

