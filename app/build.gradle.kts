import java.io.FileInputStream
import java.util.*

plugins {
    id("android-application-convention")
    id("kotlin-android")
    alias(libs.plugins.compose.compiler)
    id("hilt-convention")
    kotlin("plugin.serialization")
    id("kotlin-parcelize")
}

android {
    namespace = "com.tunjid.airbnb"

    signingConfigs {
        getByName("debug") {
            if (file("debugKeystore.properties").exists()) {
                val props = Properties()
                props.load(FileInputStream(file("debugKeystore.properties")))
                storeFile = file(props["keystore"] as String)
                storePassword = props["keystore.password"] as String
                keyAlias = props["keyAlias"] as String
                keyPassword = props["keyPassword"] as String
            }
        }
        create("release") {
            if (file("debugKeystore.properties").exists()) {
                val props = Properties()
                props.load(FileInputStream(file("debugKeystore.properties")))
                storeFile = file(props["keystore"] as String)
                storePassword = props["keystore.password"] as String
                keyAlias = props["keyAlias"] as String
                keyPassword = props["keyPassword"] as String
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.com.android.desugarJdkLibs)

    implementation(project(":scaffold"))

    implementation(project(":feature:favorites"))
    implementation(project(":feature:listing:detail"))
    implementation(project(":feature:listing:feed"))
    implementation(project(":feature:listing:gallery"))
    implementation(project(":feature:messages"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:explore"))

    implementation(project(":app:workmanager"))

    implementation(libs.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.compose.ui.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.window.window)

    implementation(libs.coil.kt.compose)
    implementation(libs.coil.kt.video)

    implementation(libs.tunjid.treenav.core.common)
    implementation(libs.tunjid.treenav.strings.common)

    implementation(libs.tunjid.mutator.core.common)
    implementation(libs.tunjid.mutator.coroutines.common)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}