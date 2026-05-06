plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.uranium92.wechatbridge"
    compileSdk = 34 // 适配 Android 14

    defaultConfig {
        applicationId = "com.uranium92.wechatbridge"
        minSdk = 28 // Android 9，足以支持 NotificationListener 和 Telecom 框架
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // 基础核心库
    implementation("androidx.core:core-ktx:1.12.0")
}
