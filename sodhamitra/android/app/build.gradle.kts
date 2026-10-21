import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "org.tribhasa.sodhamitra"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.tribhasa.sodhamitra"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // TESTING key: a fixed, non-secret key (committed on purpose) so that every testing APK, whether built on a computer or by
    // GitHub Actions, is signed identically and a newer testing APK installs over an older one without losing data.
    // It is NOT the release key. Anybody can read it in the repository, so it must never be used for a release.
    //
    // RELEASE key: a private key that is NEVER in the repository or in the source ZIP. Its location and passwords are read from the
    // git-ignored file android/release-signing.properties or, for automation, from environment variables:
    //   storeFile / SODHAMITRA_RELEASE_STORE_FILE, storePassword / SODHAMITRA_RELEASE_STORE_PASSWORD,
    //   keyAlias / SODHAMITRA_RELEASE_KEY_ALIAS, keyPassword / SODHAMITRA_RELEASE_KEY_PASSWORD
    // Without them `assembleRelease` still works but produces an UNSIGNED APK (app-release-unsigned.apk), which cannot be installed.
    val releaseProps = Properties().apply {
        val f = rootProject.file("release-signing.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun releaseSetting(property: String, environment: String): String? =
        (releaseProps.getProperty(property) ?: System.getenv(environment))?.takeIf { it.isNotBlank() }
    val releaseStore = releaseSetting("storeFile", "SODHAMITRA_RELEASE_STORE_FILE")
    val releaseStorePassword = releaseSetting("storePassword", "SODHAMITRA_RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = releaseSetting("keyAlias", "SODHAMITRA_RELEASE_KEY_ALIAS")
    val releaseKeyPassword = releaseSetting("keyPassword", "SODHAMITRA_RELEASE_KEY_PASSWORD")

    signingConfigs {
        create("testing") {
            storeFile = file("../testing.keystore")
            storePassword = "sodhamitra-test"
            keyAlias = "sodhamitra-testing"
            keyPassword = "sodhamitra-test"
        }
        if (releaseStore != null && releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null) {
            create("release") {
                storeFile = rootProject.file(releaseStore)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true    // JAR signature: readable by every installer and tool
                enableV2Signing = true    // APK Signature Scheme v2 (Android 7+)
                enableV3Signing = true    // v3 (Android 9+): supports later key rotation
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("testing")
        }
        release {
            isMinifyEnabled = false
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Only the languages of this app are kept from libraries, so that no foreign-language strings are shipped.
    androidResources {
        localeFilters += listOf("en", "kn", "sa", "te", "hi", "ta", "bn", "gu", "ml", "or")
    }

    // The language of the interface is chosen inside the app, so a Play bundle must keep every language in the base APK.
    bundle {
        language { enableSplit = false }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    val room = "2.8.3"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.test:core-ktx:1.7.0")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("androidx.test.espresso:espresso-core:3.7.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.room:room-testing:$room")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
