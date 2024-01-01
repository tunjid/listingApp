/**
 * Precompiled [ui-module-convention.gradle.kts][Ui_module_convention_gradle] script plugin.
 *
 * @see Ui_module_convention_gradle
 */
public
class UiModuleConventionPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Ui_module_convention_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
