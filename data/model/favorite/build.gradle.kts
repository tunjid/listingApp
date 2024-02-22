plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("hilt-convention")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.tunjid.listing.data.favorite"
}

dependencies {
    implementation(project(":data:model:listing:database"))
    implementation(project(":data:model:favorite:database"))

    implementation(libs.kotlinx.serialization.protobuf)
}