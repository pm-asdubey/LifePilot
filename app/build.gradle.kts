import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

// ---------------------------------------------------------------------------
// Versioning
// Increment versionCode on every release (monotonically increasing integer).
// versionName follows semver: MAJOR.MINOR.PATCH
// GitHub Release tag = "v{versionName}" (e.g. v1.0.0)
// ---------------------------------------------------------------------------
val appVersionCode = 2
val appVersionName = "1.0.1"

// Load local.properties explicitly — findProperty() does NOT read custom keys from it.
val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}

// GitHub repository slug for OTA update checking.
// Override in local.properties: github.repo=yourname/lifepilot
// CI passes this automatically via -Pgithub.repo=${{ github.repository }}
val githubRepo = findProperty("github.repo") as String? ?: "CONFIGURE_ME/lifepilot"

// ---------------------------------------------------------------------------
// Signing (never commit keystore or passwords)
// Local: set KEYSTORE_PATH, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
//        in local.properties (already gitignored) or environment variables.
// CI:    secrets are decoded by GitHub Actions and passed as env vars.
// ---------------------------------------------------------------------------
val keystorePath = System.getenv("KEYSTORE_PATH")
    ?: findProperty("KEYSTORE_PATH") as String?
val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
    ?: findProperty("KEYSTORE_PASSWORD") as String?
val keyAlias = System.getenv("KEY_ALIAS")
    ?: findProperty("KEY_ALIAS") as String?
val keyPassword = System.getenv("KEY_PASSWORD")
    ?: findProperty("KEY_PASSWORD") as String?

android {
    namespace = "com.lifepilot.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lifepilot.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "GITHUB_REPO", "\"$githubRepo\"")

        // Demo API key — set demo.api.key in local.properties (never commit).
        // Baked into BuildConfig so the seeder can pre-configure the app for demo builds.
        val demoApiKey = System.getenv("DEMO_API_KEY")
            ?: localProps.getProperty("demo.api.key") ?: ""
        buildConfigField("String", "DEMO_API_KEY", "\"$demoApiKey\"")
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
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
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // Three distributions from the same codebase:
    //   standard → includes first-run onboarding (paste-prompt / scan)
    //   stable   → NO onboarding; separate applicationId so both install side by side
    //   demo     → NO onboarding, pre-seeded data + API key; for recruiter / demo use
    flavorDimensions += "distribution"
    productFlavors {
        create("standard") {
            dimension = "distribution"
            buildConfigField("boolean", "ONBOARDING_ENABLED", "true")
        }
        create("stable") {
            dimension = "distribution"
            applicationIdSuffix = ".stable"
            versionNameSuffix = "-stable"
            buildConfigField("boolean", "ONBOARDING_ENABLED", "false")
        }
        create("demo") {
            dimension = "distribution"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            buildConfigField("boolean", "ONBOARDING_ENABLED", "false")
        }
    }

    // Name all release APKs with their flavor so the in-app updater can match the
    // correct asset by flavor name (e.g. "-demo-" for demo, "-standard-" for standard).
    applicationVariants.configureEach {
        val variant = this
        outputs.configureEach {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "LifePilot-${variant.flavorName}-${variant.versionName}.apk"
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
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/NOTICE.md",
            )
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
    // Onboarding "Scan document" path uses the ML Kit document scanner (crop + enhance).
    implementation(libs.mlkit.document.scanner)

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
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.testing.android)
    androidTestImplementation(libs.hilt.testing)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.uiautomator)
    kspAndroidTest(libs.hilt.compiler)
}
