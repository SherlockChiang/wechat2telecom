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
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    
    // 因为我们目前没有 UI，所以不需要引入 Material Design 或 Compose 的依赖
}