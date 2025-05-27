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

    implementation(project(":data:model:listing"))
    implementation(project(":data:model:listing:database"))

    implementation(project(":data:model:media"))
    implementation(project(":data:model:media:database"))

    implementation(project(":data:model:user"))
    implementation(project(":data:model:user:database"))

    implementation(project(":data:network"))
    implementation(project(":data:sync"))

    implementation(libs.kotlinx.serialization.protobuf)
}