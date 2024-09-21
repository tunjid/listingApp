plugins {
    id("android-library-convention")
    id("android-compose-convention")
    id("kotlin-library-convention")
    id("hilt-convention")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tunjid.listing.ui"
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.core)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3.material3)


    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.protobuf)

    implementation(libs.coil.kt.compose)

    implementation(libs.tunjid.composables)
}