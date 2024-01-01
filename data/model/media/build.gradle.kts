plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("hilt-convention")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.tunjid.listing.data.media"
}

dependencies {
    implementation(project(":data:model:listing:database"))
    implementation(project(":data:model:media:database"))

    implementation(libs.kotlinx.serialization.protobuf)
}