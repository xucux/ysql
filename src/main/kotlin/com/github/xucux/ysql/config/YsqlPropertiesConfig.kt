package com.github.xucux.ysql.config

import com.intellij.openapi.diagnostic.Logger
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties

/**
 * 使用用户目录下的 properties 文件保存 YSQL 配置。
 */
object YsqlPropertiesConfig {
    private const val CONFIG_DIRECTORY_NAME = ".ysql"
    private const val CONFIG_FILE_NAME = "ysql.properties"
    private const val KEY_LANGUAGE_TAG = "language.tag"
    private const val DEFAULT_LANGUAGE_TAG = "en"

    private val logger = Logger.getInstance(YsqlPropertiesConfig::class.java)
    private val configDirectory: Path by lazy { Paths.get(System.getProperty("user.home", "."), CONFIG_DIRECTORY_NAME) }
    private val configFile: Path by lazy { configDirectory.resolve(CONFIG_FILE_NAME) }

    /**
     * 读取语言标签。
     */
    fun getLanguageTag(): String {
        val languageTag = readProperties().getProperty(KEY_LANGUAGE_TAG)?.trim().orEmpty()
        return if (languageTag.isBlank()) DEFAULT_LANGUAGE_TAG else languageTag
    }

    /**
     * 保存语言标签。
     */
    fun setLanguageTag(languageTag: String) {
        val normalizedLanguageTag = languageTag.trim().ifBlank { DEFAULT_LANGUAGE_TAG }
        updateProperties { properties ->
            properties.setProperty(KEY_LANGUAGE_TAG, normalizedLanguageTag)
        }
    }

    /**
     * 返回配置文件路径。
     */
    fun getConfigFilePath(): Path = configFile

    private fun readProperties(): Properties {
        val properties = Properties()
        if (!Files.exists(configFile)) {
            return properties
        }

        return try {
            Files.newInputStream(configFile).use { inputStream: InputStream ->
                properties.load(inputStream)
            }
            properties
        } catch (exception: Exception) {
            logger.warn("Failed to read YSQL config from $configFile", exception)
            Properties()
        }
    }

    private fun updateProperties(updateAction: (Properties) -> Unit) {
        synchronized(this) {
            val properties = readProperties()
            updateAction(properties)
            writeProperties(properties)
        }
    }

    private fun writeProperties(properties: Properties) {
        try {
            Files.createDirectories(configDirectory)
            Files.newOutputStream(configFile).use { outputStream: OutputStream ->
                properties.store(outputStream, "YSQL plugin settings")
            }
        } catch (exception: Exception) {
            logger.warn("Failed to write YSQL config to $configFile", exception)
        }
    }
}

