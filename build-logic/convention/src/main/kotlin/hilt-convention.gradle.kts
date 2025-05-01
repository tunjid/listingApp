import ext.libs

plugins {
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
}

dependencies {
    "implementation"(libs.findLibrary("hilt.android").get())
    "ksp"(libs.findLibrary("hilt.compiler").get())
    "kspAndroidTest"(libs.findLibrary("hilt.compiler").get())
    "kspTest"(libs.findLibrary("hilt.compiler").get())
}
