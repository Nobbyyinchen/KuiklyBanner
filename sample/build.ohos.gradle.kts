plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    ohosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":kuikly-banner"))
            implementation("com.tencent.kuikly-open:core:${providers.gradleProperty("KUIKLY_OHOS_VERSION").get()}")
        }
    }
}

android {
    namespace = "com.hzbank.kuikly.banner.sample"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }
}

