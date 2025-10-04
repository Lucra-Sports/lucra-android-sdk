plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
val mavenVersion = project.findProperty("publishVersion") as String
val composeBomVersion = "2025.09.01"
val navigationComposeVersion = "2.9.3"

android {
    namespace = "com.lucrasports.sdk.app"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = "com.lucrasports.sdk.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = mavenVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // These are required for auth0 to run even though it won't use the web auth callback flow
        addManifestPlaceholders(
            mapOf(
                "auth0Domain" to "LUCRA_SDK_TEST",
                "auth0Scheme" to "LUCRA_SDK_TEST"
            )
        )

        // TODO add your deeplink host config here as needed (this is not needed for Lucra)
        manifestPlaceholders["deepLinkSchemaName"] = "not-needed-internal-code-reference-only"
        manifestPlaceholders["branchio-host"] = "not-needed-internal-code-reference-only"
        manifestPlaceholders["branchio-host-alt"] = "not-needed-internal-code-reference-only"
        manifestPlaceholders["branchio-key"] = "not-needed-internal-code-reference-only"
        manifestPlaceholders["branchio-key-test"] = "not-needed-internal-code-reference-only"

        buildConfigField(
            "String",
            "TESTING_API_KEY",
            "\"BHGhy6w9eOPoU7z1UdHffuDNdlihYU6T\""
        )
        buildConfigField(
            "String",
            "TESTING_API_URL",
            "\"api-sample.staging.lucrasports.com\""
        )

        // TODO this is just for our example, not required for your app!
        buildConfigField(
            "String",
            "FIREBASE_DEEPLINK_URL",
            "\"https://lucrasdk.page.link\""
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
        // Required for Lucra SDK
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    debugImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))

    implementation("com.lucrasports.sdk:sdk-ui:$mavenVersion")

    // For testing internal UI of the reward flow - not required for client integration
    implementation("com.lucrasports.sdk:feature-reward-selection-flow:$mavenVersion")

    // Required for Lucra SDK
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Only brought in to make the branch io references work (only used on internal sample copied to here)
    implementation("io.branch.sdk.android:library:5.16.1")

    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-dynamic-links:21.1.0")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.animation:animation-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.compose.runtime:runtime-rxjava2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.navigation:navigation-compose:$navigationComposeVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("org.reflections:reflections:0.9.12")
    testImplementation("io.mockk:mockk:1.12.0")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.jaredrummler:colorpicker:1.1.0")
}
