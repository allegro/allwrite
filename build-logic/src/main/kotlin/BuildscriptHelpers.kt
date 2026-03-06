import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Creates a dependency to the 'recipeClasspaths' variant of a given project
 */
fun DependencyHandler.recipeClasspaths(project: ProjectDependency) =
    project(mapOf("path" to project.path, "configuration" to "recipeClasspaths"))

