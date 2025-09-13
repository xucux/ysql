package com.github.xucux.ysql.utils

import com.github.xucux.ysql.models.SuffixType
import java.text.SimpleDateFormat
import java.util.*

/**
 * 后缀生成器
 * 根据不同的策略生成分表后缀
 */
object SuffixGenerator {
    
    /**
     * 生成后缀
     * @param index 索引
     * @param suffixType 后缀类型
     * @param format 格式字符串
     * @param startYear 起始年份
     * @param startMonth 起始月份
     * @return 生成的后缀
     */
    fun generateSuffix(
        index: Int,
        suffixType: SuffixType,
        format: String = "_",
        startYear: Int = 2020,
        startMonth: Int = 1
    ): String {
        return when (suffixType) {
            SuffixType.SEQUENCE -> generateSequenceSuffix(index, format)
            SuffixType.YEAR -> generateYearSuffix(index, format, startYear)
            SuffixType.YEAR_MONTH -> generateYearMonthSuffix(index, format, startYear, startMonth)
            SuffixType.CUSTOM -> generateCustomSuffix(index, format)
        }
    }
    
    /**
     * 生成数字序列后缀
     * @param index 索引
     * @param format 格式字符串
     * @return 序列后缀
     */
    private fun generateSequenceSuffix(index: Int, format: String): String {
        return "$format$index"
    }
    
    /**
     * 生成年份后缀
     * @param index 索引
     * @param format 格式字符串
     * @param startYear 起始年份
     * @return 年份后缀
     */
    private fun generateYearSuffix(index: Int, format: String, startYear: Int): String {
        val year = startYear + index
        return "$format$year"
    }
    
    /**
     * 生成年月后缀
     * @param index 索引
     * @param format 格式字符串
     * @param startYear 起始年份
     * @param startMonth 起始月份
     * @return 年月后缀
     */
    private fun generateYearMonthSuffix(index: Int, format: String, startYear: Int, startMonth: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(startYear, startMonth - 1, 1) // 月份从0开始
        calendar.add(Calendar.MONTH, index)
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // 月份从0开始，需要+1
        
        return "$format$year${String.format("%02d", month)}"
    }
    
    /**
     * 生成自定义后缀
     * @param index 索引
     * @param format 格式字符串
     * @return 自定义后缀
     */
    private fun generateCustomSuffix(index: Int, format: String): String {
        // 支持简单的占位符替换
        return format.replace("{index}", index.toString())
            .replace("{INDEX}", index.toString())
            .replace("{i}", index.toString())
            .replace("{I}", index.toString())
    }
    
    /**
     * 批量生成后缀列表
     * @param count 生成数量
     * @param suffixType 后缀类型
     * @param format 格式字符串
     * @param startYear 起始年份
     * @param startMonth 起始月份
     * @return 后缀列表
     */
    fun generateSuffixList(
        count: Int,
        suffixType: SuffixType,
        format: String = "_",
        startYear: Int = 2020,
        startMonth: Int = 1
    ): List<String> {
        return (0 until count).map { index ->
            generateSuffix(index, suffixType, format, startYear, startMonth)
        }
    }
    
    /**
     * 验证格式字符串
     * @param format 格式字符串
     * @param suffixType 后缀类型
     * @return 验证结果
     */
    fun validateFormat(format: String, suffixType: SuffixType): ValidationResult {
        if (format.isBlank()) {
            return ValidationResult(false, "格式字符串不能为空")
        }
        
        when (suffixType) {
            SuffixType.CUSTOM -> {
                // 检查是否包含有效的占位符
                val hasPlaceholder = format.contains("{index}") || 
                                   format.contains("{INDEX}") || 
                                   format.contains("{i}") || 
                                   format.contains("{I}")
                if (!hasPlaceholder) {
                    return ValidationResult(false, "自定义格式必须包含占位符，如 {index} 或 {i}")
                }
            }
            else -> {
                // 其他类型只需要非空即可
            }
        }
        
        return ValidationResult(true, "格式字符串有效")
    }
    
    /**
     * 获取后缀类型的示例
     * @param suffixType 后缀类型
     * @return 示例字符串
     */
    fun getSuffixExample(suffixType: SuffixType): String {
        return when (suffixType) {
            SuffixType.SEQUENCE -> "示例：_0, _1, _2, _3..."
            SuffixType.YEAR -> "示例：_2020, _2021, _2022, _2023..."
            SuffixType.YEAR_MONTH -> "示例：_202001, _202002, _202003, _202004..."
            SuffixType.CUSTOM -> "示例：使用 {index} 占位符，如 table_{index} 生成 table_0, table_1..."
        }
    }
    
    /**
     * 验证结果数据类
     */
    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
