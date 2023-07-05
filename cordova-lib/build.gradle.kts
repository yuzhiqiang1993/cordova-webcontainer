plugins {
    alias(libs.plugins.xeonyu.library)
    alias(libs.plugins.vanniktechPublish)
}

android {
    namespace = "org.apache.cordova"
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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.core.splashscreen)

}
