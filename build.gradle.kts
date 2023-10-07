// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
}

allprojects {
    repositories {

        mavenCentral()
        google()

        // Required for Lucra SDK
        maven { url = uri("https://zendesk.jfrog.io/zendesk/repo") }
        // Required for Lucra SDK
        maven {
            name = "LucraGithubPackages"
            url = uri("https://maven.pkg.github.com/Lucra-Sports/lucra-android")
            credentials {
                val gprUser: String? = findProperty("GPR_USER") as String? ?: System.getenv("GPR_USER")
                val gprKey: String? = findProperty("GPR_KEY") as String? ?: System.getenv("GPR_KEY")

                if (gprUser.isNullOrEmpty()) {
                    throw GradleException("GPR_USER not set in ~ .gradle/gradle.properties, local gradle.properties or as an environment variable.")
                }
                if (gprKey.isNullOrEmpty()) {
                    throw GradleException("GPR_KEY not set in ~ .gradle/gradle.properties, local gradle.properties or as an environment variable.")
                }

                username = gprUser
                password = gprKey
            }
        }
    }
}