import java.io.File
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// ---------------------------------------------------------------------------------------------
// Integration secrets: read from local.properties (never committed), with environment variables
// as an override so CI can inject them from repository secrets.
//   ADJUST_APP_TOKEN, OFFER_BASE_URL, OFFER_ROUTING, GOOGLE_SERVICES_JSON
// ---------------------------------------------------------------------------------------------
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}

fun integrationSecret(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(name)?.trim().orEmpty()

// ---------------------------------------------------------------------------------------------
// google-services.json materialisation.
//
// The file never enters the repository. It arrives like the other secrets: GOOGLE_SERVICES_JSON
// (base64) from local.properties locally, GitHub Secrets on CI. The google-services plugin needs
// a file per build type whose package_name matches the applied applicationId, so one is written
// for debug and one for release before the plugin reads them.
//
//   - debug ALWAYS gets a stub: its applicationId ends in `.debug`, which the real production
//     file never matches, and the plugin fails on a mismatch.
//   - release gets the real decoded file when the secret is present, otherwise a stub so a local
//     build still assembles. A stub ships WITHOUT working push: project_number is all zeros.
//     Locally this is only good for White/Black, routing, signals and signature - not push.
// ---------------------------------------------------------------------------------------------
fun stubGoogleServices(packageName: String): String = """
    {
      "project_info": {
        "project_number": "000000000000",
        "project_id": "arctic-fish-trail-stub",
        "storage_bucket": "arctic-fish-trail-stub.appspot.com"
      },
      "client": [
        {
          "client_info": {
            "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
            "android_client_info": { "package_name": "$packageName" }
          },
          "oauth_client": [],
          "api_key": [ { "current_key": "AIzaSyStubStubStubStubStubStubStubStub0000" } ],
          "services": { "appinvite_service": { "other_platform_oauth_client": [] } }
        }
      ],
      "configuration_version": "1"
    }
""".trimIndent()

fun writeGoogleServices(buildTypeDir: String, packageName: String, allowReal: Boolean) {
    val outDir = File(projectDir, "src/$buildTypeDir")
    outDir.mkdirs()
    val out = File(outDir, "google-services.json")
    val base64 = integrationSecret("GOOGLE_SERVICES_JSON")
    val real = if (allowReal && base64.isNotBlank()) {
        runCatching { String(Base64.getMimeDecoder().decode(base64.trim())) }
            .getOrNull()
            ?.takeIf { it.contains("\"project_number\"") }
    } else {
        null
    }
    if (real != null) {
        out.writeText(real)
    } else {
        if (allowReal) {
            logger.warn("GOOGLE_SERVICES_JSON is absent or unparsable - writing a push-less stub for $buildTypeDir.")
        }
        out.writeText(stubGoogleServices(packageName))
    }
}

// Written at configuration time so both files exist before the google-services plugin reads them.
writeGoogleServices(buildTypeDir = "debug", packageName = "com.arcticfishtrail.game.debug", allowReal = false)
writeGoogleServices(buildTypeDir = "release", packageName = "com.arcticfishtrail.game", allowReal = true)

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
        versionCode = 6
        versionName = "1.0.5"
        vectorDrawables { useSupportLibrary = true }

        // buildConfigField pastes the value into generated Java verbatim, hence the escaped quotes.
        buildConfigField("String", "ADJUST_APP_TOKEN", "\"${integrationSecret("ADJUST_APP_TOKEN")}\"")
        buildConfigField("String", "OFFER_BASE_URL", "\"${integrationSecret("OFFER_BASE_URL")}\"")
        // aso | prily - the campaign group decides the link shape (sub1 in the path for prily).
        buildConfigField(
            "String",
            "OFFER_ROUTING",
            "\"${integrationSecret("OFFER_ROUTING").ifBlank { "aso" }}\"",
        )
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
        buildConfig = true
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

    // Integration layer: attribution (Adjust v5) and push (Firebase Cloud Messaging).
    implementation(libs.adjust)
    implementation(libs.installreferrer)
    implementation(libs.play.services.ads.identifier)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    // Re-enables the X-Requested-With header the WebView stopped sending in M108.
    implementation(libs.androidx.webkit)

    testImplementation(libs.junit)
}
