plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// CI'da sürüm numarası çalıştırma sayısından gelir; yerelde 1
val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toIntOrNull() ?: 1
val ciKeystore = file("ci-release.jks")

android {
    namespace = "com.glasspanel.media"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.glasspanel.media"
        minSdk = 26
        targetSdk = 34
        versionCode = runNumber
        versionName = "3.$runNumber"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        // Anahtar gizli kayıttan çözülüp ci-release.jks olarak yazılır; yoksa debug anahtarı kullanılır
        if (ciKeystore.exists()) {
            create("ci") {
                storeFile = ciKeystore
                storePassword = "medyapaneli"
                keyAlias = "medya"
                keyPassword = "medyapaneli"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("ci") ?: signingConfigs.getByName("debug")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
}
