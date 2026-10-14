import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

val playUploadKeystorePath = providers.gradleProperty("PLAY_UPLOAD_KEYSTORE_PATH").orNull
    ?: providers.environmentVariable("PLAY_UPLOAD_KEYSTORE_PATH").orNull
val playUploadStorePassword = providers.gradleProperty("PLAY_UPLOAD_STORE_PASSWORD").orNull
    ?: providers.environmentVariable("PLAY_UPLOAD_STORE_PASSWORD").orNull
val playUploadKeyAlias = providers.gradleProperty("PLAY_UPLOAD_KEY_ALIAS").orNull
    ?: providers.environmentVariable("PLAY_UPLOAD_KEY_ALIAS").orNull
val playUploadKeyPassword = providers.gradleProperty("PLAY_UPLOAD_KEY_PASSWORD").orNull
    ?: providers.environmentVariable("PLAY_UPLOAD_KEY_PASSWORD").orNull
val hasPlayUploadSigning = listOf(
    playUploadKeystorePath,
    playUploadStorePassword,
    playUploadKeyAlias,
    playUploadKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.moasseum.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.moasseum.app"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("VERSION_CODE").orNull?.toIntOrNull() ?: 1
        versionName = providers.gradleProperty("VERSION_NAME").orNull ?: "0.1.0"
        buildConfigField("boolean", "PLAY_STORE_DISTRIBUTION", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val aiBaseUrl = providers.gradleProperty("AI_API_BASE_URL").orNull?.takeIf { it.isNotBlank() }
            ?: "https://moasseum-ai-worker.blossom0948.workers.dev"
        val escapedAiBaseUrl = aiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "AI_API_BASE_URL", "\"$escapedAiBaseUrl\"")
        for (name in listOf("SUPABASE_URL", "SUPABASE_PUBLISHABLE_KEY")) {
            val raw = providers.gradleProperty(name).orNull.orEmpty()
            if (name == "SUPABASE_PUBLISHABLE_KEY" && raw.isNotBlank()) {
                require(!raw.startsWith("sb_secret_")) { "Never package a Supabase secret key in the APK." }
                if (!raw.startsWith("sb_publishable_")) {
                    val payload = runCatching { String(Base64.getUrlDecoder().decode(raw.split('.')[1])) }.getOrDefault("")
                    require(Regex("\"role\"\\s*:\\s*\"anon\"").containsMatchIn(payload)) { "Use only a publishable key or legacy anon key in the APK." }
                }
            }
            val value = raw.replace("\\", "\\\\").replace("\"", "\\\"")
            buildConfigField("String", name, "\"$value\"")
        }
    }

    buildTypes {
        debug { applicationIdSuffix = ".qa" }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("playStore") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "PLAY_STORE_DISTRIBUTION", "true")
            // Never inherit the sideload/debug certificate for a Play upload.
            signingConfig = null
        }
    }

    val releaseKeystorePath = providers.gradleProperty("RELEASE_KEYSTORE_PATH").orNull
        ?: providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
    val releaseStorePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").orNull
        ?: providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
    val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").orNull
        ?: providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").orNull
        ?: providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull

    if (releaseKeystorePath != null && releaseStorePassword != null &&
        releaseKeyAlias != null && releaseKeyPassword != null
    ) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
    } else {
        // Keep updates compatible with the already-published APK, which uses this same debug certificate.
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("debug")
    }

    if (hasPlayUploadSigning) {
        signingConfigs {
            create("playUpload") {
                storeFile = file(checkNotNull(playUploadKeystorePath))
                storePassword = checkNotNull(playUploadStorePassword)
                keyAlias = checkNotNull(playUploadKeyAlias)
                keyPassword = checkNotNull(playUploadKeyPassword)
            }
        }
        buildTypes.getByName("playStore").signingConfig = signingConfigs.getByName("playUpload")
    } else {
        buildTypes.getByName("playStore").signingConfig = null
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
}

tasks.matching { it.name == "bundlePlayStore" || it.name == "assemblePlayStore" }.configureEach {
    doFirst {
        check(hasPlayUploadSigning) {
            "Play distribution needs a dedicated upload keystore. Set PLAY_UPLOAD_KEYSTORE_PATH, PLAY_UPLOAD_STORE_PASSWORD, PLAY_UPLOAD_KEY_ALIAS, and PLAY_UPLOAD_KEY_PASSWORD without committing them."
        }
    }
}

androidComponents {
    onVariants(selector().withBuildType("playStore")) { variant ->
        val mergedManifest = variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.MERGED_MANIFEST)
        val verifyPlayManifest = tasks.register("verifyPlayStoreManifest") {
            group = "verification"
            description = "Checks Play target API and excludes sideload-only install permission."
            inputs.file(mergedManifest)
            doLast {
                val manifest = mergedManifest.get().asFile.readText()
                check(Regex("""android:targetSdkVersion\s*=\s*["']36["']""").containsMatchIn(manifest)) {
                    "The Play manifest must target Android 16 (API 36) or higher."
                }
                check("android.permission.REQUEST_INSTALL_PACKAGES" !in manifest) {
                    "The Play manifest must not request sideload installation permission."
                }
            }
        }
        tasks.named("check").configure { dependsOn(verifyPlayManifest) }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.mlkit:text-recognition-korean:16.0.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.2")
}

kapt {
    correctErrorTypes = true
}
