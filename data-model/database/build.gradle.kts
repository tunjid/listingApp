plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("room-database-convention")
    id("hilt-convention")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.tunjid.listing.data.favorite.database"
}

dependencies {
    implementation(project(":data:model:listing:database"))

    implementation(libs.kotlinx.serialization.protobuf)
}