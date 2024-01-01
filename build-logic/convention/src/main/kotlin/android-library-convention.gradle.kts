plugins {
    id("com.android.library")
}

android {
    commonConfiguration(this)

    defaultConfig {
        targetSdk = 31
    }

    sourceSets {
        named("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.srcDirs("src/androidMain/res")
        }
    }
}

addDesugarDependencies()