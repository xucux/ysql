package com.github.xucux.ysql.models

/**
 * 批量删除存储过程配置模型
 * 用于存储批量删除存储过程生成的相关配置信息
 */
data class BatchDeleteConfig(
    /**
     * 存储过程名称
     */
    val procedureName: String = "",
    
    /**
     * 主表名
     */
    val mainTableName: String = "",
    
    /**
     * 主键字段名
     */
    val primaryKeyField: String = "id",
    
    /**
     * 时间字段名
     */
    val timeField: String = "create_time",
    
    /**
     * 每次删除行数
     */
    val limitSize: Int = 1000,
    
    /**
     * 起始主键值
     */
    val minId: Long = 0,
    
    /**
     * 删除截至时间
     */
    val createTimeEnd: String = "",
    
    /**
     * 是否添加日志表
     */
    val addLogTable: Boolean = true,
    
    /**
     * 是否添加临时操作表
     */
    val addTempTable: Boolean = true,
    
    /**
     * 自定义WHERE条件
     */
    val customWhereCondition: String = "",
    
    /**
     * 存储过程注释
     */
    val procedureComment: String = "批量删除历史数据存储过程"
)
