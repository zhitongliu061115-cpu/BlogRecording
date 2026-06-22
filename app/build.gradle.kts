plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.blogrecording"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.blogrecording"
        minSdk = 29
        targetSdk = 36
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

val requiredBundledModelFiles = mapOf(
    "SenseVoice" to file("src/main/assets/models/sensevoice/model.int8.onnx"),
    "SenseVoice tokens" to file("src/main/assets/models/sensevoice/tokens.txt"),
    "VAD" to file("src/main/assets/models/vad/silero_vad.onnx"),
    "Speaker segmentation" to file("src/main/assets/models/diarization/segmentation.onnx"),
    "Speaker embedding" to file("src/main/assets/models/diarization/embedding.onnx"),
)

tasks.register("verifyBundledModels") {
    group = "verification"
    description = "Fails the build when required bundled sherpa-onnx model files are missing."
    doLast {
        val missing = requiredBundledModelFiles.filterValues { !it.exists() || it.length() == 0L }
        if (missing.isNotEmpty()) {
            val details = missing.entries.joinToString(separator = "\n") { (name, file) ->
                "- $name: ${file.path}"
            }
            throw GradleException(
                "Required bundled model files are missing:\n$details\n" +
                    "Do not ship this APK until real model files are placed under app/src/main/assets/models/."
            )
        }
    }
}

tasks.named("preBuild") {
    dependsOn("verifyBundledModels")
}

dependencies {
    implementation(files("libs/sherpa-onnx-static-link-onnxruntime-1.13.2.aar"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
