plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("ui-module-convention")
    id("hilt-convention")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.tunjid.feature.profile"
}

dependencies {
    implementation(project(":data"))
    implementation(project(":data-model"))
    implementation(project(":data:sync"))

    implementation(libs.coil.kt.compose)
    implementation(libs.coil.kt.ktor)
    implementation(libs.ktor.client)
    implementation(libs.tunjid.tiler.compose)
    implementation(libs.tunjid.tiler.tiler)

    testImplementation(project(":data:test"))
}