plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("ui-module-convention")
    id("hilt-convention")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.tunjid.listing.feature.listing.feed"
}

dependencies {
    implementation(project(":data"))
    implementation(project(":data-model"))
    implementation(project(":data-sync"))
    implementation(project(":ui"))

    implementation(libs.coil.kt.compose)
    implementation(libs.coil.kt.ktor)
    implementation(libs.ktor.client)
    implementation(libs.tunjid.tiler.compose)
    implementation(libs.tunjid.tiler.tiler)
    implementation(libs.tunjid.composables)

    testImplementation(project(":data-test"))
}