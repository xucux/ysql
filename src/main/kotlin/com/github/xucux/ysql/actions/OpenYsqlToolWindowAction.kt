package com.github.xucux.ysql.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

/**
 * 打开Ysql工具窗口Action
 * 用于从菜单中打开Ysql插件的工具窗口
 */
class OpenYsqlToolWindowAction : AnAction("打开Ysql工具窗口", "打开Ysql插件的工具窗口，包含分表SQL解析和StringBuffer代码生成功能", null), DumbAware {
    
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        
        // 获取工具窗口管理器并激活Ysql工具窗口
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("Ysql")
        
        if (toolWindow != null) {
            toolWindow.activate(null)
        }
    }
    
    override fun update(event: AnActionEvent) {
        // 检查是否有项目可用
        event.presentation.isEnabledAndVisible = event.project != null
    }
}
