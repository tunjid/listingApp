import ext.configureKotlinJvm
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

fun org.gradle.api.Project.configureKotlinAndroid(
    kotlinAndroidProjectExtension: KotlinAndroidProjectExtension
) {
    kotlinAndroidProjectExtension.apply {
        sourceSets.apply {
            all {
                languageSettings.apply {
                    optIn("androidx.compose.animation.ExperimentalAnimationApi")
                    optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                    optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                    optIn("androidx.compose.material.ExperimentalMaterialApi")
                    optIn("androidx.compose.ui.ExperimentalComposeUiApi")
                    optIn("kotlinx.serialization.ExperimentalSerializationApi")
                    optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                    optIn("kotlinx.coroutines.FlowPreview")
                }
            }
        }
        configureKotlinJvm()
    }
}
