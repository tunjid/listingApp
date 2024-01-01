plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    commonConfiguration(this)
    composeConfiguration(this)

    defaultConfig {
        targetSdk = 31
    }
}