plugins {
    alias(libs.plugins.xeonyu.application)
}

android {
    namespace = "com.yzq.demo"

    defaultConfig {
        applicationId = "com.yzq.demo"
        versionCode = 1
        versionName = "1.0"
        targetSdk = libs.versions.targetSdk.get().toInt()
        minSdk = 24
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "LOG_DEBUG", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "LOG_DEBUG", "false")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.xeonyu.logger)
//    implementation("com.xeonyu:cordova-webcontainer:1.0.5")
    implementation(project(":cordova-webcontainer"))
//    implementation(project(":cordova-webcontainer"))

}
