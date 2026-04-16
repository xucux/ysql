package com.github.xucux.ysql.actions

import com.github.xucux.ysql.ui.YsqlToolWindowFactory
import com.github.xucux.ysql.utils.I18nUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

/**
 * 打开Ysql工具窗口Action
 * 用于从菜单中打开Ysql插件的工具窗口
 */
class OpenYsqlToolWindowAction : AnAction(
    I18nUtil.getMessage("action.open.tool.window.text"),
    I18nUtil.getMessage("action.open.tool.window.description"),
    null
), DumbAware {
    
    /**
     * 执行当前动作。
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        
        // 获取工具窗口管理器并激活Ysql工具窗口
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(YsqlToolWindowFactory.TOOL_WINDOW_ID)
        
        if (toolWindow != null) {
            toolWindow.activate(null)
        }
    }
    
    /**
     * 更新当前动作的展示状态。
     */
    override fun update(event: AnActionEvent) {
        event.presentation.text = I18nUtil.getMessage("action.open.tool.window.text")
        event.presentation.description = I18nUtil.getMessage("action.open.tool.window.description")
        // 检查是否有项目可用
        event.presentation.isEnabledAndVisible = event.project != null
    }
}
