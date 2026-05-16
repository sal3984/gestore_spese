plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.3.8"
    id("com.diffplug.spotless") version "8.5.1"
}

android {
    namespace = "com.expense.management"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.expense.management"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
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
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("com.google.code.gson:gson:2.14.0")

    // ROOM (Database SQLite)
    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version") // Per usare le coroutine
    ksp("androidx.room:room-compiler:$room_version")
    // Aggiungere queste dipendenze nella sezione 'dependencies' del build.gradle.kts del modulo app
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0") // Oppure la versione che usi
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.10.0") // QUESTA E' CRITICA

    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    // AppCompat - Necessaria per FragmentActivity e temi
    implementation("androidx.appcompat:appcompat:1.7.1")
}
