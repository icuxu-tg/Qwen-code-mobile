plugins {
    id("com.android.application")
}

android {
    namespace = "com.qwen.mobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.qwen.mobile"
        minSdk = 26
        // targetSdk 28 намеренно: Android разрешает exec/dlop из data dir только
        // приложениям, собранным под API < 29. Termux использует тот же приём.
        targetSdk = 28
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/qwen-release.jks")
            storePassword = "qwen2026"
            keyAlias = "qwen"
            keyPassword = "qwen2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // runtime.zip уже сжат; не сжимаем его повторно, чтобы не тратить RAM/CPU.
        noCompress += "zip"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}
