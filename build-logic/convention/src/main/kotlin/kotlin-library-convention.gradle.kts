plugins {
    kotlin("android")
//    id("com.google.devtools.ksp")
}

kotlin {
    configureKotlinAndroid(this)
    sourceSets.all {
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
    }
}
