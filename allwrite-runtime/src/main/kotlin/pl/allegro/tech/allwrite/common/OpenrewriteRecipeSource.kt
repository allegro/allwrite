package pl.allegro.tech.allwrite.common

import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.common.port.incoming.RecipeSource
import pl.allegro.tech.allwrite.common.port.incoming.tagPropertyOrNull
import pl.allegro.tech.allwrite.recipes.RecipeVisibility.PUBLIC

@Single
internal class OpenrewriteRecipeSource : RecipeSource {

    private val openrewriteEnvironment by lazy {
        Environment.builder()
            .scanRuntimeClasspath("pl.allegro.tech.allwrite.recipes")
            .build()
    }

    override fun findAll(): List<RecipeDescriptor> =
        openrewriteEnvironment.listRecipeDescriptors()
            .filter { it.name.startsWith("pl.allegro.tech.allwrite.recipes") }
            .filter { PUBLIC.name.equals(it.tagPropertyOrNull("visibility"), true) }

    override fun activate(recipe: String): Recipe = openrewriteEnvironment.activateRecipes(recipe)
}
