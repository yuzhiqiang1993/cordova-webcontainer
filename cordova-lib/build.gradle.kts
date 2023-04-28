plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "org.apache.cordova"
    compileSdk = 32
    defaultConfig {
        minSdk = 23
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs("src")
            resources.srcDirs("src")
            aidl.srcDirs("src")
            renderscript.srcDirs("src")
            res.srcDirs("res")
            assets.srcDirs("assets")
        }
    }


    packaging {
        resources.excludes.apply {
            add("META-INF/LICENSE")
            add("META-INF/LICENSE.txt")
            add("META-INF/DEPENDENCIES")
            add("META-INF/NOTICE")
        }

    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.webkit:webkit:1.6.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
}
