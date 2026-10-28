import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("com.diffplug.spotless") version "8.5.1"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Firma del release: keystore e password stanno fuori dal repo, in keystore.properties (ignorato da git)
// o nelle variabili d'ambiente della CI. Senza, il release si compila ma resta non firmato.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String): String? = keystoreProperties.getProperty(key) ?: System.getenv(env)

android {
    namespace = "com.expense.management"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.expense.management"
        minSdk = 26
        targetSdk = 36
        versionCode = project.findProperty("VERSION_CODE")?.toString()?.toIntOrNull() ?: 1
        versionName = project.findProperty("VERSION_NAME")?.toString() ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            signingValue("storeFile", "RELEASE_STORE_FILE")?.let { path ->
                storeFile = file(path)
                storePassword = signingValue("storePassword", "RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.useJUnitPlatform()
            // Robolectric su SDK 36 accede a jdk.internal.access (ApplicationSharedMemory)
            it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
        }
    }

    // Schemi Room esportati, letti da MigrationTestHelper nei test delle migration.
    // Robolectric legge solo gli asset della variante testata: vanno nel debug, mai nel release.
    sourceSets.getByName("debug").assets.directories.add("$projectDir/schemas")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
    }
}

spotless {
    kotlin {
        target("**/*.kt")

        // Usa l'ultima versione di ktlint
        ktlint("1.0.1").editorConfigOverride(
            mapOf(
                // Questa è la regola magica per Compose:
                // Dice a ktlint di ignorare il controllo del nome se la funzione è @Composable
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable"
            )
        )

        // Opzionale: Rimuove import non usati e formatta
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.foundation.layout)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    // Test delle migration Room: JUnit 4 + Robolectric, eseguiti dal vintage engine
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.junit)
    // Test dei ViewModel: Dispatchers.Main sostituibile
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.gson)

    // ROOM (Database SQLite)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Biometric
    implementation(libs.androidx.biometric)

    // AppCompat - Necessaria per FragmentActivity e temi
    implementation(libs.androidx.appcompat)

    implementation(libs.coil.compose)

    // Image Cropper (uCrop)
    implementation(libs.ucrop)

    // Play Services Wearable (per WearableDataListenerService)
    implementation(libs.play.services.wearable)

    // ML Kit Text Recognition (per scansione ricevute)
    implementation(libs.mlkit.text.recognition)
}
