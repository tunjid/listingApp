plugins {
    `kotlin-dsl`
}

group = "com.tunjid.me.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.com.android.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.android.gradle.plugin)
    implementation(libs.org.jetbrains.kotlin.serialization.plugin)
    implementation(libs.com.google.devtools.ksp.plugin)
    implementation(libs.hilt.gradle.plugin)
}