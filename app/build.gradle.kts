import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.ksp)
}

object AppVersion {
    const val major = 1
    const val minor = 0
    const val patch = 1
    const val code = major * 10000 + minor * 100 + patch
    const val name = "$major.$minor.$patch"
    const val dbVersion = 3
}

android {
    namespace = "com.example.appui"
    compileSdk = 36

    val localPropertiesFile = rootProject.file("local.properties")
    val localProperties = Properties()
    if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
    }

    defaultConfig {
        applicationId = "com.example.appui"
        minSdk = 26
        targetSdk = 36
        versionCode = AppVersion.code
        versionName = AppVersion.name

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // ✅ Fixed: Add fallback to environment variables
        buildConfigField(
            "String",
            "GITHUB_TOKEN",
            "\"${localProperties.getProperty("github.token")
                ?: System.getenv("GITHUB_TOKEN")
                ?: ""}\""
        )

        buildConfigField(
            "String",
            "ELEVENLABS_BASE_URL",
            "\"${localProperties.getProperty("XI_BASE_URL")
                ?: System.getenv("XI_BASE_URL")
                ?: "https://api.elevenlabs.io"}\""
        )

        buildConfigField(
            "String",
            "ELEVENLABS_API_KEY",
            "\"${localProperties.getProperty("XI_API_KEY")
                ?: System.getenv("XI_API_KEY")
                ?: ""}\""
        )

        buildConfigField(
            "String",
            "ELEVENLABS_AGENT_ID",
            "\"${localProperties.getProperty("XI_AGENT_ID")
                ?: System.getenv("XI_AGENT_ID")
                ?: ""}\""
        )

        buildConfigField(
            "String",
            "GITHUB_OWNER",
            "\"${localProperties.getProperty("GITHUB_OWNER")
                ?: System.getenv("GITHUB_OWNER")
                ?: "baolongdev"}\""
        )

        buildConfigField(
            "String",
            "GITHUB_REPO",
            "\"${localProperties.getProperty("GITHUB_REPO")
                ?: System.getenv("GITHUB_REPO")
                ?: "appUI"}\""
        )

        buildConfigField(
            "String",
            "UPDATE_JSON_URL",
            "\"${localProperties.getProperty("UPDATE_JSON_URL")
                ?: System.getenv("UPDATE_JSON_URL")
                ?: "https://raw.githubusercontent.com/baolongdev/appUI/main/update.json"}\""
        )
    }

    // ✅ Lint config
    lint {
        checkReleaseBuilds = false
        abortOnError = false
        quiet = true
        checkAllWarnings = false
        checkTestSources = false
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProperties = Properties()
    val useKeystoreFile = keystorePropertiesFile.exists()

    if (useKeystoreFile) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    signingConfigs {
        create("release") {
            if (useKeystoreFile) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                println("✅ Using keystore from keystore.properties")
            } else {
                val envKeystoreFile = System.getenv("KEYSTORE_FILE")
                if (envKeystoreFile != null) {
                    val keystoreFile = rootProject.file(envKeystoreFile)

                    if (keystoreFile.exists()) {
                        storeFile = keystoreFile
                        storePassword = System.getenv("KEYSTORE_PASSWORD")
                        keyAlias = System.getenv("KEY_ALIAS")
                        keyPassword = System.getenv("KEY_PASSWORD")
                        println("✅ Using keystore from environment: ${keystoreFile.absolutePath}")
                    } else {
                        println("⚠️ Keystore not found: ${keystoreFile.absolutePath}")
                    }
                } else {
                    println("⚠️ No KEYSTORE_FILE environment variable")
                }
            }
        }
    }

    buildTypes {
        // ✅ Release-debug variant để test minification
        create("releaseDebug") {
            initWith(getByName("release"))
            isDebuggable = true
            isMinifyEnabled = true
            isShrinkResources = true
            applicationIdSuffix = ".release.debug"
            versionNameSuffix = "-release-debug"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.composeBom))
    androidTestImplementation(platform(libs.composeBom))

    implementation(libs.androidxCoreKtx)
    implementation(libs.activityCompose)
    implementation(libs.composeMaterial3)
    implementation(libs.navigationCompose)
    implementation(libs.lifecycleRuntimeKtx)
    implementation(libs.lifecycleViewmodelCompose)

    // Hilt - ✅ MIGRATION TO KSP
    implementation(libs.hiltAndroid)
    ksp(libs.hiltCompiler)  // ✅ THAY kapt → ksp
    implementation(libs.hiltNavigationCompose)

    // Coroutines
    implementation(libs.coroutinesAndroid)

    // Network
    implementation(libs.retrofit)
    implementation(libs.converterMoshi)
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)
    implementation(libs.moshi)

    // Gson for GitHub API
    implementation(libs.converterGson)
    implementation(libs.gson)

    // ElevenLabs Android SDK
    implementation(libs.elevenlabsAndroid)

    // WorkManager - ✅ MIGRATION TO KSP
    implementation(libs.workRuntimeKtx)
    implementation(libs.hiltWork)
    ksp(libs.androidxHiltCompiler)  // ✅ THAY kapt → ksp

    // Storage / Security
    implementation(libs.datastore)
    implementation(libs.securityCrypto)

    // TFLite
    implementation(libs.tflite)

    // UI
    implementation(libs.androidxAppcompat)
    implementation(libs.material)
    implementation(libs.materialIconsExtended)
    debugImplementation(libs.uiTooling)
    debugImplementation(libs.uiToolingPreview)

    // Markdown
    implementation(libs.composeMarkdown)

    // Permission handling
    implementation(libs.accompanistPermissions)

    // Room Database
    implementation(libs.roomRuntime)
    implementation(libs.roomKtx)
    ksp(libs.roomCompiler)

    // ✅ Rive Animation
    implementation(libs.riveAndroid)
    implementation(libs.startupRuntime)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(libs.androidxEspressoCore)
}
