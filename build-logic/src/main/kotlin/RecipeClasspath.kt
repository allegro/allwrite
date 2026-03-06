import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import javax.inject.Inject

abstract class RecipeClasspath(
    val name: String
) {
    val dependencyScopeConfigurationName = name
    val resolvableConfigurationName = "$name.classpath"

    @get:Inject
    abstract val project: Project

    fun classpath(dependencyNotation: String) {
        project.dependencies {
            add(name, dependencyNotation)
        }
    }
}
