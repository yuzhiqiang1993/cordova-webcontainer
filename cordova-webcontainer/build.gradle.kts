plugins {
    alias(libs.plugins.xeonyu.library)
    alias(libs.plugins.vanniktechPublish)
}

android {
    namespace = "com.yzq.cordova_webcontainer"
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    api(libs.xeonyu.cordova.lib)
//    api("com.xeonyu.cordova-lib:12.0.2")
}