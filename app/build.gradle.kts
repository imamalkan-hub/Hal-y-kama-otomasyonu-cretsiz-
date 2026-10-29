plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mimhalikama.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mimhalikama.app"
        minSdk = 24
        targetSdk = 34
        // Sürüm tek kaynaktan (gradle.properties) gelir; CI'da -P ile
        // geçilebilir: gradle assembleRelease -PversionCode=7 -PversionName=1.2
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0"
    }

    // İmzalama: sırlar ortam değişkeninden gelir (CI secrets / ~/.gradle/gradle.properties).
    // Depoya anahtar dosyası girmez (.gitignore'da). CI, secrets ile keystore'u
    // üretir; yoksa release imzasız derlenir (bu durumda da derleme çalışır).
    signingConfigs {
        create("release") {
            val ks = System.getenv("MIM_STORE_FILE")
                ?: (project.findProperty("MIM_STORE_FILE") as String?)
            if (!ks.isNullOrBlank() && file(ks).exists()) {
                storeFile = file(ks)
                storePassword = System.getenv("MIM_STORE_PASSWORD")
                    ?: (project.findProperty("MIM_STORE_PASSWORD") as String?)
                keyAlias = System.getenv("MIM_KEY_ALIAS")
                    ?: (project.findProperty("MIM_KEY_ALIAS") as String?) ?: "mim"
                keyPassword = System.getenv("MIM_KEY_PASSWORD")
                    ?: (project.findProperty("MIM_KEY_PASSWORD") as String?)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val sc = signingConfigs.findByName("release")
            if (sc != null && sc.storeFile != null) {
                signingConfig = sc
            }
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
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.compose.material:material-icons-core")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
