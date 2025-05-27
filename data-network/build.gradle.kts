plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("hilt-convention")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.tunjid.listing.data.network"
}

dependencies {
    implementation(project(":data-model"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
}