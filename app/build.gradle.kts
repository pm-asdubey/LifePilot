plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.lifepilot.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lifepilot.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.lifepilot.app.HiltTestRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signing: configure via local.properties or environment variables.
            // Required keys: KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
            // Do NOT commit keystore or credentials to the repository.
            val keystorePath = project.findProperty("KEYSTORE_PATH") as String?
            if (keystorePath != null) {
                signingConfig = signingConfigs.create("release").apply {
                    storeFile = file(keystorePath)
                    storePassword = project.findProperty("KEYSTORE_PASSWORD") as String?
                    keyAlias = project.findProperty("KEY_ALIAS") as String?
                    keyPassword = project.findProperty("KEY_PASSWORD") as String?
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    implementation(project(":core:common"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":designsystem"))
    implementation(project(":features:home"))
    implementation(project(":features:library"))
    implementation(project(":features:search"))
    implementation(project(":features:object"))
    implementation(project(":features:document"))
    implementation(project(":features:settings"))
    implementation(project(":features:timeline"))
    implementation(project(":features:planner"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.bundles.lifecycle)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.androidx.biometric)
    implementation(libs.timber)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary)

    testImplementation(libs.bundles.testing.unit)
    androidTestImplementation(libs.bundles.testing.android)
    androidTestImplementation(libs.hilt.testing)
    androidTestImplementation(libs.compose.ui.test.junit4)
    kspAndroidTest(libs.hilt.compiler)
}
