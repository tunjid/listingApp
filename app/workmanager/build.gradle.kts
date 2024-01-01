plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("hilt-convention")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.tunjid.listing.app.workmanager"
}

dependencies {
    implementation(project(":data"))
    implementation(project(":data:sync"))

    implementation(libs.androidx.work.ktx)
    implementation(libs.hilt.ext.work)

    kapt(libs.hilt.ext.compiler)
}