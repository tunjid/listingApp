plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("hilt-convention")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.tunjid.listing.data.database"
}

dependencies {
    implementation(project(":data:model:listing"))
    implementation(project(":data:model:listing:database"))

    implementation(project(":data:model:image"))
    implementation(project(":data:model:image:database"))

    implementation(project(":data:model:user"))

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    kapt(libs.androidx.room.compiler)
}