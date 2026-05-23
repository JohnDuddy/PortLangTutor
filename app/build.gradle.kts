import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun localOrDefault(key: String, default: String): String =
    localProperties.getProperty(key) ?: System.getenv(key) ?: default

android {
    namespace = "com.duddy.portugues"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.duddy.portugues"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── Backend & Supabase configuration ────────────────────────────
        // Emulator: use 10.0.2.2 to reach the host machine.
        // Physical phone over USB: run `adb reverse tcp:8010 tcp:8010` and use 127.0.0.1.
        //
        // Override these via local.properties or env vars:
        //   DUDDY_BACKEND_URL=https://api.duddy.app
        //   SUPABASE_URL=https://YOUR-PROJECT.supabase.co
        //   SUPABASE_ANON_KEY=eyJ...
        buildConfigField(
            "String", "DUDDY_BACKEND_URL",
            "\"${localOrDefault("DUDDY_BACKEND_URL", "http://10.0.2.2:8010")}\""
        )
        buildConfigField(
            "String", "SUPABASE_URL",
            "\"${localOrDefault("SUPABASE_URL", "")}\""
        )
        buildConfigField(
            "String", "SUPABASE_ANON_KEY",
            "\"${localOrDefault("SUPABASE_ANON_KEY", "")}\""
        )
    }

    signingConfigs {
        create("release") {
            storeFile = file(localOrDefault("RELEASE_KEYSTORE_PATH", "release.keystore"))
            storePassword = localOrDefault("KEYSTORE_PASSWORD", "")
            keyAlias = localOrDefault("KEY_ALIAS", "")
            keyPassword = localOrDefault("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val releaseSigning = signingConfigs.getByName("release")
            if (
                releaseSigning.storeFile?.exists() == true &&
                !releaseSigning.storePassword.isNullOrBlank() &&
                !releaseSigning.keyAlias.isNullOrBlank() &&
                !releaseSigning.keyPassword.isNullOrBlank()
            ) {
                signingConfig = releaseSigning
            }
        }
        debug {
            isMinifyEnabled = false
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
}

// Room schema export for migration testing
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val adbExecutable = android.sdkDirectory
    .resolve("platform-tools")
    .resolve(if (System.getProperty("os.name").lowercase().contains("windows")) "adb.exe" else "adb")

tasks.register("reverseDebugBackend", org.gradle.api.tasks.Exec::class) {
    group = "install"
    description = "Refreshes adb reverse for the local Duddy backend after debug installs."
    onlyIf { adbExecutable.exists() }
    commandLine(adbExecutable.absolutePath, "reverse", "tcp:8010", "tcp:8010")
    isIgnoreExitValue = true
}

tasks.matching { it.name == "installDebug" }.configureEach {
    finalizedBy("reverseDebugBackend")
}

dependencies {
    // ── AndroidX / Compose ──
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")
    implementation("androidx.compose.foundation:foundation:1.7.6")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")

    debugImplementation("androidx.compose.ui:ui-tooling:1.7.6")

    // ── Coroutines ──
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ── Room ──
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // ── DataStore (for auth token persistence) ──
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── Supabase Kotlin client ──
    val supabaseVersion = "3.0.2"
    implementation(platform("io.github.jan-tennert.supabase:bom:$supabaseVersion"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-client-android:3.0.2")

    // ── Kotlinx Serialization (required by Supabase) ──
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // ── OkHttp (multipart upload for audio) ──
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ── Tests ──
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.9")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.6")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.6")
}
