plugins {
    alias(libs.plugins.xeonyu.library)
    alias(libs.plugins.vanniktechPublish)
}

android {
    namespace = "com.yzq.cordova_webcontainer"
}

dependencies {
    implementation(libs.kotlin.bom.stable)
    implementation(libs.androidx.activity.ktx.stable)
    implementation(libs.androidx.core.ktx.stable)
    implementation(libs.androidx.appcompat.stable)
    api(libs.xeonyu.cordova.lib)
//    api("com.xeonyu.cordova-lib:12.0.2")
}