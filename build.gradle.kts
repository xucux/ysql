plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.16.1"
}

group = "com.github.xucux"
version = "1.2.0-201"

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
    version.set("2020.1.1")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

dependencies {
    implementation("com.github.jsqlparser:jsqlparser:4.9")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<JavaExec> {
        // 解决控制台中文乱码
        jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dfile.stdout.encoding=UTF-8", "-Dfile.stderr.encoding=UTF-8")
    }

    patchPluginXml {
        sinceBuild.set("201.6668.113")
        untilBuild.set("223.*")
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
