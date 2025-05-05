import com.android.build.api.dsl.CommonExtension
import ext.configureKotlinJvm
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies

/**
 * Sets common values for Android Applications and Libraries
 */
fun org.gradle.api.Project.commonConfiguration(
    extension: CommonExtension<*, *, *, *, *>
) = extension.apply {
    compileSdk = 36

    defaultConfig {
        // Could have been 21, but I need sqlite 3.24.0 for upserts
        minSdk = 30
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    configureKotlinJvm()
}

fun org.gradle.api.Project.composeConfiguration(
    extension: CommonExtension<*, *, *, *, *>
) = extension.apply {
    buildFeatures {
        compose = true
    }
}

fun org.gradle.api.Project.addDesugarDependencies() {
    dependencies {
        add(
            configurationName = "coreLibraryDesugaring",
            dependencyNotation = versionCatalog.findLibrary("com-android-desugarJdkLibs").get()
        )
    }
}

val org.gradle.api.Project.versionCatalog
    get() = extensions.getByType(VersionCatalogsExtension::class.java)
        .named("libs")