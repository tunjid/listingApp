plugins {
    id("android-library-convention")
    id("android-compose-convention")
    id("kotlin-library-convention")
    id("hilt-convention")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.tunjid.listing.scaffold"
}

dependencies {
    implementation(project(":data"))
    implementation(project(":data-model"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.core)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.material3)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.datastore.core.okio)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.window.core)
    implementation(libs.androidx.window.window)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.protobuf)

    implementation(libs.squareup.okio)

    implementation(libs.coil.kt.compose)
    implementation(libs.coil.kt.ktor)
    implementation(libs.ktor.client)
    implementation(libs.coil.kt.ktor)
    implementation(libs.ktor.client)
    implementation(libs.coil.kt.video)

    implementation(libs.tunjid.composables)

    implementation(libs.tunjid.treenav.compose.threepane)
    implementation(libs.tunjid.treenav.compose)
    implementation(libs.tunjid.treenav.core)
    implementation(libs.tunjid.treenav.strings)

    implementation(libs.tunjid.mutator.core.common)
    implementation(libs.tunjid.mutator.coroutines.common)
}