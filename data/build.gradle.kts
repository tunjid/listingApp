plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("hilt-convention")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.tunjid.listing.data"
}

dependencies {
    implementation(project(":data-database"))
    implementation(project(":data-model"))
    implementation(project(":data-network"))
    implementation(project(":data-sync"))

    implementation(libs.androidx.datastore.core.okio)

    implementation(libs.squareup.okio)

    implementation(libs.kotlinx.serialization.protobuf)
}