plugins {
    alias(libs.plugins.xeonyu.library)
    alias(libs.plugins.vanniktechPublish)
}

android {
    namespace = "com.yzq.cordova_webcontainer"
    defaultConfig {
        minSdk = 24
    }
}

dependencies {
    implementation(libs.kotlin.bom.stable)
    implementation(libs.androidx.activity.ktx.stable)
    implementation(libs.androidx.core.ktx.stable)
    implementation(libs.androidx.appcompat.stable)
//    api(project(":cordova-lib"))
    api(libs.xeonyu.cordova.android)
}

mavenPublishing {

    publishToMavenCentral()

    val versionName = project.findProperty("VERSION_NAME")?.toString() ?: ""
    val isSnapshot = versionName.endsWith("SNAPSHOT", ignoreCase = true)
    if (!isSnapshot) {
        signAllPublications()
    }
}