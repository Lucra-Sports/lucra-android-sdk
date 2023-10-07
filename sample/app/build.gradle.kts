plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lucrasports.sdk.app"
    compileSdk = 33

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.lucrasports.sdk.app"
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // These are required for auth0 to run even though it won't use the web auth callback flow
        addManifestPlaceholders(mapOf("auth0Domain" to "LUCRA_SDK_TEST", "auth0Scheme" to "LUCRA_SDK_TEST"))

        buildConfigField(
            "String",
            "TESTING_AUTH_ID",
            "\"ADD YOUR AUTH0 AUTH ID HERE\""
        )
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")

            applicationIdSuffix = ".debug"
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}


dependencies {
    implementation("com.lucrasports:sdk-ui:1.0.1-alpha-SNAPSHOT-2023-09-21-1695313286")

    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.1")
    testImplementation("org.reflections:reflections:0.9.12")
    testImplementation("io.mockk:mockk:1.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}