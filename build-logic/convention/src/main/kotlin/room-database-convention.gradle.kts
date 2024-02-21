import androidx.room.gradle.RoomExtension
import ext.libs
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

plugins {
    id("androidx.room")
    id("com.google.devtools.ksp")
}

extensions.configure<RoomExtension> {
    // The schemas directory contains a schema file for each version of the Room database.
    // This is required to enable Room auto migrations.
    // See https://developer.android.com/reference/kotlin/androidx/room/AutoMigration.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    "implementation"(libs.findLibrary("androidx.room.ktx").get())
    "implementation"(libs.findLibrary("androidx.room.runtime").get())
    "annotationProcessor"(libs.findLibrary("androidx.room.compiler").get())
    "ksp"(libs.findLibrary("androidx.room.compiler").get())
}