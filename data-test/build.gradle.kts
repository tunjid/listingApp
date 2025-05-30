plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
}

android {
    namespace = "com.tunjid.listing.data.test"
}

dependencies {
    api(project(":data"))
    api(project(":data-model"))
    api(project(":data-sync"))

    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.coroutines.test)
    api(libs.kotlinx.serialization.protobuf)

}