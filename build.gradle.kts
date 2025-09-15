plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "com.github.xucux"
version = "1.1.0"

repositories {
    // 阿里云镜像仓库 - 国内访问速度更快
    maven("https://maven.aliyun.com/repository/gradle-plugin")
    maven("https://maven.aliyun.com/repository/public")
    maven("https://maven.aliyun.com/repository/central/")

    // 华为云镜像仓库 - 备用选择
    maven("https://mirrors.huaweicloud.com/repository/maven")

    // 专门用于 JetBrains 产品的仓库
    maven("https://maven.aliyun.com/repository/jetbrains-intellij-releases")
    // 或者使用更通用的 JB 仓库
    maven("https://maven.aliyun.com/repository/jetbrains-public")
    maven("https://repo.huaweicloud.com/repository/jetbrains-intellij-releases")
    maven("https://repo.huaweicloud.com/repository/jetbrains-public")
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.1.5")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("241.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
