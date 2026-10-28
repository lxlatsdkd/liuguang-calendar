plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.util.Properties

android {
    namespace = "com.example.liuguangcalendar"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.example.liuguangcalendar"
        minSdk = 26
        targetSdk = 36
        versionCode = 12
        versionName = "1.0.2"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    val signingProperties = Properties()
    val signingPropertiesFile = rootProject.file("keystore.properties")
    if (signingPropertiesFile.isFile) {
        signingPropertiesFile.inputStream().use(signingProperties::load)
    }

    val releaseStoreFile = signingProperties.getProperty("storeFile")
        ?: System.getenv("LIUGUANG_STORE_FILE")
    val releaseStorePassword = signingProperties.getProperty("storePassword")
        ?: System.getenv("LIUGUANG_STORE_PASSWORD")
    val releaseKeyAlias = signingProperties.getProperty("keyAlias")
        ?: System.getenv("LIUGUANG_KEY_ALIAS")
    val releaseKeyPassword = signingProperties.getProperty("keyPassword")
        ?: System.getenv("LIUGUANG_KEY_PASSWORD")
    val releaseSigningConfigured = listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword
    ).all { !it.isNullOrBlank() }

    signingConfigs {
        create("release") {
            if (releaseSigningConfigured) {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            isDebuggable = false
            isMinifyEnabled = false
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            it.systemProperty(
                "user.home",
                layout.buildDirectory.dir("robolectric-home").get().asFile.absolutePath
            )
            it.systemProperty(
                "java.io.tmpdir",
                layout.buildDirectory.dir("robolectric-tmp").get().asFile.absolutePath
            )
            it.systemProperty(
                "maven.repo.local",
                layout.buildDirectory.dir("robolectric-m2").get().asFile.absolutePath
            )
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation(composeBom)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
}
