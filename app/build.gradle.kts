plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.bao.calculator"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.bao.calculator"
        minSdk = 24
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.drawerlayout)
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// 将 APK 复制到项目 doc 目录（兼容 AGP 9）
tasks.whenTaskAdded {
    if (name == "assembleDebug" || name == "assembleRelease") {
        doLast {
            val variant = if (name == "assembleDebug") "debug" else "release"
            val apkDir = file("build/outputs/apk/$variant")
            val docDir = file("${rootProject.projectDir}/doc")
            apkDir.listFiles()?.filter { it.extension == "apk" }?.forEach { apk ->
                docDir.mkdirs()
                val dest = file("$docDir/Calculator-$variant-${android.defaultConfig.versionName}.apk")
                apk.copyTo(dest, overwrite = true)
                println("Copied: ${apk.name} -> doc/")
            }
        }
    }
}