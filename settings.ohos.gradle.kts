pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}

rootProject.name = "KuiklyBanner"
rootProject.buildFileName = "build.ohos.gradle.kts"

include(":kuikly-banner")
project(":kuikly-banner").buildFileName = "build.ohos.gradle.kts"

include(":sample")
project(":sample").buildFileName = "build.ohos.gradle.kts"

