plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ---------------------------------------------------------------------------------------------
// Release signing: read ONLY from environment variables. Never falls back to the debug key.
//   ANDROID_KEYSTORE_PATH      absolute path to the PKCS12 (.p12) keystore
//   ANDROID_KEYSTORE_PASSWORD  store password
//   ANDROID_KEY_ALIAS          key alias
//   ANDROID_KEY_PASSWORD       key password (for PKCS12 usually equal to the store password)
// ---------------------------------------------------------------------------------------------
val signingEnvNames = listOf(
    "ANDROID_KEYSTORE_PATH",
    "ANDROID_KEYSTORE_PASSWORD",
    "ANDROID_KEY_ALIAS",
    "ANDROID_KEY_PASSWORD",
)
val signingEnv: Map<String, String?> = signingEnvNames.associateWith { name ->
    System.getenv(name)?.takeIf { it.isNotBlank() }
}
val missingSigningEnv: List<String> = signingEnv.filterValues { it == null }.keys.toList()
val releaseKeystoreFile: File? = signingEnv["ANDROID_KEYSTORE_PATH"]?.let { file(it) }
val releaseSigningReady: Boolean =
    missingSigningEnv.isEmpty() && releaseKeystoreFile != null && releaseKeystoreFile.isFile

android {
    namespace = "com.arcticfishtrail.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.arcticfishtrail.game"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = releaseKeystoreFile
                storePassword = signingEnv["ANDROID_KEYSTORE_PASSWORD"]
                keyAlias = signingEnv["ANDROID_KEY_ALIAS"]
                keyPassword = signingEnv["ANDROID_KEY_PASSWORD"]
                storeType = "pkcs12"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            // Staged R8: ship the first release NON-minified. After a verified release build,
            // switch both flags to true, rebuild and repeat the local verification checklist.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
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
    }
    buildFeatures {
        compose = true
        buildConfig = false
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Fail loudly (and early) when a signed release artifact is requested without signing env vars.
val validateReleaseSigning by tasks.registering {
    group = "verification"
    description = "Fails the build if release signing environment variables are missing."
    val missing = missingSigningEnv
    val keystorePath = signingEnv["ANDROID_KEYSTORE_PATH"]
    val ready = releaseSigningReady
    doFirst {
        if (!ready) {
            val reason = when {
                missing.isNotEmpty() -> "Missing environment variables: ${missing.joinToString()}"
                else -> "Keystore file not found at ANDROID_KEYSTORE_PATH=$keystorePath"
            }
            throw GradleException(
                "Release signing is not configured. $reason\n" +
                    "Set ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS and " +
                    "ANDROID_KEY_PASSWORD to build a release APK/AAB. The debug key is never used " +
                    "for release builds. For local testing use ./gradlew assembleDebug.",
            )
        }
    }
}

// Hook the validation into the tasks that produce signed release artifacts only, so
// `./gradlew assembleDebug` / `testDebugUnitTest` never need signing secrets.
tasks.configureEach {
    if (name == "packageRelease" || name == "signReleaseBundle") {
        dependsOn(validateReleaseSigning)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
