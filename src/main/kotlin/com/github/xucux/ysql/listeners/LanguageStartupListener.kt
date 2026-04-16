package com.github.xucux.ysql.listeners

import com.github.xucux.ysql.config.LanguageUiRefresher
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

/**
 * 在项目启动后刷新语言相关 UI。
 */
class LanguageStartupListener : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        LanguageUiRefresher.refreshProject(project)
    }
}
