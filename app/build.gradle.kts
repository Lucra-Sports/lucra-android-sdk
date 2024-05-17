plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
val mavenVersion = project.findProperty("publishVersion") as String

android {
    namespace = "com.lucrasports.sdk.app"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.lucrasports.sdk.app"
        minSdk = 24
        targetSdk = 34
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

        // TODO Add your auth0 client id here
        buildConfigField(
            "String",
            "TESTING_API_KEY",
            "\"ADD YOUR API KEY HERE\""
        )
        // TODO Add your auth0 domain url here
        buildConfigField(
            "String",
            "TESTING_API_URL",
            "\"ADD YOUR API URL HERE\""
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
    implementation("com.lucrasports.sdk:sdk-ui:$mavenVersion")

    // Required for Lucra SDK
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.jaredrummler:colorpicker:1.1.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("org.reflections:reflections:0.9.12")
    testImplementation("io.mockk:mockk:1.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.google.firebase:firebase-dynamic-links:21.1.0")
}