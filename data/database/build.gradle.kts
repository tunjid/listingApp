plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    id("room-database-convention")
    id("hilt-convention")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.tunjid.listing.data.database"
}

dependencies {
    implementation(project(":data:model:listing"))
    implementation(project(":data:model:listing:database"))

    implementation(project(":data:model:media"))
    implementation(project(":data:model:media:database"))

    implementation(project(":data:model:user"))
}