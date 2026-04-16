package com.github.xucux.ysql.config

import com.github.xucux.ysql.ui.YsqlToolWindowFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

/**
 * 刷新受插件独立语言配置影响的 UI。
 */
object LanguageUiRefresher {
    fun refreshProject(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(YsqlToolWindowFactory.TOOL_WINDOW_ID) ?: return@invokeLater
            toolWindow.setStripeTitle(YsqlToolWindowFactory.getToolWindowTitle())

            val contentManager = toolWindow.contentManager
            if (contentManager.contentCount == 0) {
                return@invokeLater
            }

            contentManager.removeAllContents(true)
            YsqlToolWindowFactory().createToolWindowContent(project, toolWindow)
        }
    }
}
