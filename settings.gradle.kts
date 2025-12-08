pluginManagement {
    repositories {
        // 镜像优先
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        // 官方源作为备选
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        // 配置镜像
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            isAllowInsecureProtocol = true  // 如果使用 HTTP 需要这个
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            isAllowInsecureProtocol = true
        }

        // 官方源
        google()
        mavenCentral()

        // 其他可能需要的仓库
        maven { url = uri("https://jitpack.io") }  // 用于 GitHub 上的库
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }  // 快照版本
        maven { url = uri("https://plugins.gradle.org/m2/") }  // Gradle 插件仓库
    }

}

rootProject.name = "JNIProject"
include(":app")