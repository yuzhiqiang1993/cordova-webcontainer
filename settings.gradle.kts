pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        /*发布到snapshot仓库时使用*/
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }

    versionCatalogs {
        create("libs") {
//            from("com.xeonyu:version-catalog:0.0.4-SNAPSHOT")
            from("com.xeonyu:version-catalog:0.0.8")
            version("xeonCordovaLib", "12.0.2")
            version("xeonCordovaWebContainer", "1.0.5")

        }
    }
}

//rootProject.name = "CordovaWebContainer"
include(":app")
include(":cordova-webcontainer")
include(":cordova-lib")
