package com.github.xucux.ysql.ui

import com.github.xucux.ysql.utils.I18nUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Ysql工具窗口工厂
 * 负责创建和管理Ysql插件的工具窗口
 */
class YsqlToolWindowFactory : ToolWindowFactory {
    companion object {
        const val TOOL_WINDOW_ID = "YSql"

        fun getToolWindowTitle(): String = I18nUtil.getMessage("toolwindow.title")
    }

    /**
     * 处理 `init` 逻辑。
     */
    override fun init(toolWindow: ToolWindow) {
        // 使用系统图标，自动适配主题
        toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowRun)
        toolWindow.setStripeTitle(getToolWindowTitle())
        super.init(toolWindow)
    }

    /**
     * 创建工具窗口内容。
     */
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
//        toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowRun)

        // 创建主工具窗口内容
        val toolWindowContent = YsqlToolWindowContent(project)

        val contentFactory = ContentFactory.SERVICE.getInstance()
        // 创建内容并添加到工具窗口
        // val content = contentFactory.createContent(toolWindowContent.getContentPanel(), "all", false)
        // toolWindow.contentManager.addContent(content)
        // 创建分表解析工具窗口内容
        val shardingPanel = contentFactory.createContent(
            toolWindowContent.getShardingPanel(),
            I18nUtil.getMessage("toolwindow.tab.sharding"),
            false
        )
        val stringBufferPanel = contentFactory.createContent(
            toolWindowContent.getStringBufferPanel(),
            I18nUtil.getMessage("toolwindow.tab.string.buffer"),
            false
        )
        val dynamicSqlPanel = contentFactory.createContent(
            toolWindowContent.getDynamicSqlPanel(),
            I18nUtil.getMessage("toolwindow.tab.dynamic.sql"),
            false
        )
        val batchDeletePanel = contentFactory.createContent(
            toolWindowContent.getBatchDeletePanel(),
            I18nUtil.getMessage("toolwindow.tab.batch.delete"),
            false
        )
        

        toolWindow.contentManager.addContent(shardingPanel)
        toolWindow.contentManager.addContent(stringBufferPanel)
        toolWindow.contentManager.addContent(dynamicSqlPanel)
        toolWindow.contentManager.addContent(batchDeletePanel)

    }
    
    /**
     * 判断工具窗口是否可用。
     */
    override fun shouldBeAvailable(project: Project): Boolean {
        // 工具窗口对所有项目都可用
        return true
    }
}
