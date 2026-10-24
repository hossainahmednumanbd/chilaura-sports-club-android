plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "bd.chilaura.sportsclub"
    compileSdk = 35
    defaultConfig {
        applicationId = "bd.chilaura.sportsclub"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
