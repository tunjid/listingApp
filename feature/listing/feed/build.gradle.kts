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

    implementation(project(":data:model:favorite"))
    implementation(project(":data:model:listing"))
    implementation(project(":data:model:media"))
    implementation(project(":data:model:user"))

    implementation(project(":data:sync"))

    implementation(libs.coil.kt.compose)
    implementation(libs.tunjid.tiler.compose)
    implementation(libs.tunjid.tiler.tiler)

    testImplementation(project(":data:test"))
}