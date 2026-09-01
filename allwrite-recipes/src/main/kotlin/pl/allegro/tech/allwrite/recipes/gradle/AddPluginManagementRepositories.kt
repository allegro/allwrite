package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.Option
import org.openrewrite.Recipe
import org.openrewrite.gradle.plugins.AddSettingsPluginRepository
import pl.allegro.tech.allwrite.AllwriteRecipe

public class AddPluginManagementRepositories(
    @Option(description = "URL of the Maven repository to add", example = "https://repo.example.com/maven")
    public val url: String,
) : AllwriteRecipe(
    displayName = "Add plugin management repositories",
    description = "Adds a Maven repository and Gradle Plugin Portal to pluginManagement repositories in settings.gradle(.kts).",
) {
    public override fun getRecipeList(): List<Recipe> =
        listOf(
            AddSettingsPluginRepository("maven", url),
            AddSettingsPluginRepository("gradlePluginPortal", null),
        )
}
