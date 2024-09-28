import ext.libs

plugins {
    id("android-library-convention")
    id("android-compose-convention")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    configureKotlinAndroid(this)
}

dependencies {
    "implementation"(project(":scaffold"))

    "implementation"(libs.findLibrary("hilt.android").get())

    "implementation"(platform(libs.findLibrary("compose.bom").get()))
    "implementation"(libs.findLibrary("androidx.activity.compose").get())
    "implementation"(libs.findLibrary("androidx.lifecycle.viewmodel.compose").get())
    "implementation"(libs.findLibrary("androidx.compose.runtime").get())
    "implementation"(libs.findLibrary("androidx.compose.animation").get())
    "implementation"(libs.findLibrary("androidx.compose.foundation.foundation").get())
    "implementation"(libs.findLibrary("androidx.compose.foundation.layout").get())
    "implementation"(libs.findLibrary("androidx.compose.runtime").get())
    "implementation"(libs.findLibrary("androidx.compose.ui.ui").get())
    "implementation"(libs.findLibrary("androidx.compose.ui.graphics").get())
    "implementation"(libs.findLibrary("androidx.compose.ui.tooling.preview").get())
    "implementation"(libs.findLibrary("androidx.compose.material.core").get())
    "implementation"(libs.findLibrary("androidx.compose.material.iconsExtended").get())
    "implementation"(libs.findLibrary("androidx.compose.material3.material3").get())
    "implementation"(libs.findLibrary("androidx.window.core").get())
    "implementation"(libs.findLibrary("androidx.window.window").get())
    "implementation"(libs.findLibrary("androidx.datastore.core.okio").get())

    "implementation"(libs.findLibrary("hilt.ext.common").get())

    "implementation"(libs.findLibrary("kotlinx.coroutines.core").get())
    "implementation"(libs.findLibrary("kotlinx.serialization.protobuf").get())

    "implementation"(libs.findLibrary("squareup.okio").get())

    "implementation"(libs.findLibrary("tunjid.treenav.core.common").get())
    "implementation"(libs.findLibrary("tunjid.treenav.strings.common").get())

    "implementation"(libs.findLibrary("tunjid.mutator.core.common").get())
    "implementation"(libs.findLibrary("tunjid.mutator.coroutines.common").get())

    testImplementation(libs.findLibrary("junit").get())
    testImplementation(libs.findLibrary("app.cash.turbine").get())
    testImplementation(libs.findLibrary("kotlinx.coroutines.test").get())

}