plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `maven-publish`
}

group = providers.gradleProperty("GROUP_ID").get()
version = providers.gradleProperty("VERSION_NAME").get()

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    js(IR) {
        browser()
        nodejs()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api("com.tencent.kuikly-open:core:${providers.gradleProperty("KUIKLY_VERSION").get()}")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "io.github.nobbyyinchen.kuikly.banner"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("KuiklyBanner")
            description.set("Reusable PeekBannerV2, DoubleBanner, and VerticalBanner components for KuiklyUI DSL.")
            url.set("https://github.com/Nobbyyinchen/KuiklyBanner")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("Nobbyyinchen")
                    name.set("Nobbyyinchen")
                }
            }
            scm {
                url.set("https://github.com/Nobbyyinchen/KuiklyBanner")
                connection.set("scm:git:https://github.com/Nobbyyinchen/KuiklyBanner.git")
                developerConnection.set("scm:git:ssh://git@github.com/Nobbyyinchen/KuiklyBanner.git")
            }
        }
    }
}
