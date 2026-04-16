package com.github.xucux.ysql.utils

import com.github.xucux.ysql.config.YsqlPropertiesConfig
import java.text.MessageFormat
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.Properties

/**
 * 提供 `I18nUtil` 相关工具方法。
 */
object I18nUtil {
    private const val DEFAULT_RESOURCE_NAME = "messages.properties"
    private const val ZH_CN_RESOURCE_NAME = "messages_zh_CN.properties"
    private const val LOCALE_ZH_CN = "zh-CN"
    private val defaultLocale: Locale = Locale.ENGLISH
    private val propertiesCache = mutableMapOf<String, Properties>()

    /**
     * 获取 `message`。
     */
    fun getMessage(key: String, vararg args: Any): String {
        val locale = getCurrentLocale()
        val text = findMessage(key, locale) ?: findMessage(key, defaultLocale) ?: key
        return if (args.isEmpty()) text else MessageFormat.format(text, *args)
    }

    /**
     * 判断是否为 `chinese`。
     */
    fun isChinese(): Boolean = getCurrentLocale().toLanguageTag() == LOCALE_ZH_CN

    /**
     * 设置 `language`。
     */
    fun setLanguage(locale: Locale) {
        val languageTag = if (locale.language == Locale.CHINESE.language) LOCALE_ZH_CN else Locale.ENGLISH.toLanguageTag()
        YsqlPropertiesConfig.setLanguageTag(languageTag)
        clearCache()
    }

    /**
     * 获取 `currentLocale`。
     */
    fun getCurrentLocale(): Locale {
        val languageTag = YsqlPropertiesConfig.getLanguageTag().ifBlank { Locale.ENGLISH.toLanguageTag() }
        return when {
            languageTag.equals(LOCALE_ZH_CN, ignoreCase = true) -> Locale.SIMPLIFIED_CHINESE
            else -> Locale.ENGLISH
        }
    }

    /**
     * 查找 `message`。
     */
    private fun findMessage(key: String, locale: Locale): String? {
        val properties = loadProperties(getResourceName(locale))
        return properties.getProperty(key)
    }

    private fun getResourceName(locale: Locale): String {
        return if (locale.toLanguageTag().equals(LOCALE_ZH_CN, ignoreCase = true)) {
            ZH_CN_RESOURCE_NAME
        } else {
            DEFAULT_RESOURCE_NAME
        }
    }

    private fun loadProperties(resourceName: String): Properties {
        return synchronized(propertiesCache) {
            propertiesCache.getOrPut(resourceName) {
                val properties = Properties()
                val inputStream = I18nUtil::class.java.classLoader.getResourceAsStream(resourceName) ?: return@getOrPut properties
                inputStream.use { stream ->
                    InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
                        properties.load(reader)
                    }
                }
                properties
            }
        }
    }

    private fun clearCache() {
        synchronized(propertiesCache) {
            propertiesCache.clear()
        }
    }
}
