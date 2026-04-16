package com.github.xucux.ysql.utils

import com.github.xucux.ysql.models.SuffixType

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
        val startDate = java.time.LocalDate.of(startYear, startMonth, 1)
        val targetDate = startDate.plusMonths(index.toLong())
        
        val year = targetDate.year
        val month = targetDate.monthValue
        
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
            return ValidationResult(false, I18nUtil.getMessage("suffix.format.required"))
        }
        
        when (suffixType) {
            SuffixType.CUSTOM -> {
                // 检查是否包含有效的占位符
                val hasPlaceholder = format.contains("{index}") || 
                                   format.contains("{INDEX}") || 
                                   format.contains("{i}") || 
                                   format.contains("{I}")
                if (!hasPlaceholder) {
                    return ValidationResult(false, I18nUtil.getMessage("suffix.format.custom.placeholder.required"))
                }
            }
            else -> {
                // 其他类型只需要非空即可
            }
        }
        
        return ValidationResult(true, I18nUtil.getMessage("suffix.format.valid"))
    }
    
    /**
     * 获取后缀类型的示例
     * @param suffixType 后缀类型
     * @return 示例字符串
     */
    fun getSuffixExample(suffixType: SuffixType): String {
        return when (suffixType) {
            SuffixType.SEQUENCE -> I18nUtil.getMessage("suffix.example.sequence")
            SuffixType.YEAR -> I18nUtil.getMessage("suffix.example.year")
            SuffixType.YEAR_MONTH -> I18nUtil.getMessage("suffix.example.year_month")
            SuffixType.CUSTOM -> I18nUtil.getMessage("suffix.example.custom")
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
