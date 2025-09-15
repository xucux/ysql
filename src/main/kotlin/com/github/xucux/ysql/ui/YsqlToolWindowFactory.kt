package com.github.xucux.ysql.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.UIUtil

/**
 * Ysql工具窗口工厂
 * 负责创建和管理Ysql插件的工具窗口
 */
class YsqlToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {

//        // 检测当前主题
//        val isDarkTheme = UIUtil.isUnderDarcula()
//
//        // 根据主题选择图标
//        val iconPath = if (isDarkTheme) {
//            "/icons/pluginIcon.svg"  // 暗色主题使用浅色图标
//        } else {
//            "/icons/pluginIcon_dark.svg"   // 亮色主题使用深色图标
//        }
//        val icon = IconLoader.getIcon(iconPath, YsqlToolWindowFactory::class.java)
//        toolWindow.setIcon(icon)

        // 使用系统图标，自动适配主题
        toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowRun)

        // 创建主工具窗口内容
        val toolWindowContent = YsqlToolWindowContent(project)
        
        // 创建内容并添加到工具窗口
        val content = ContentFactory.getInstance().createContent(
            toolWindowContent.getContentPanel(),
            null,
            false
        )
        
        toolWindow.contentManager.addContent(content)
    }
    
    override fun shouldBeAvailable(project: Project): Boolean {
        // 工具窗口对所有项目都可用
        return true
    }
}
