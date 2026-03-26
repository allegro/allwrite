package pl.allegro.tech.allwrite.runtime

import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.api.RecipeSource
import pl.allegro.tech.allwrite.api.tagPropertyOrNull
import pl.allegro.tech.allwrite.spi.RecipeVisibility.PUBLIC

@Single
internal class OpenrewriteRecipeSource : RecipeSource {

    private val allwriteEnvironment = Environment.builder()
        .scanRuntimeClasspath(ALLWRITE_PACKAGE)
        .build()

    private val openrewriteEnvironment = Environment.builder()
        .scanRuntimeClasspath(OPEN_REWRITE_PACKAGE)
        .build()

    override fun findAll(includeInternal: Boolean): List<RecipeDescriptor> =
        listOf(allwriteEnvironment, openrewriteEnvironment)
            .flatMap(Environment::listRecipeDescriptors)
            .distinct()
            .filter { includeInternal || PUBLIC.name.equals(it.tagPropertyOrNull("visibility"), ignoreCase = true) }

    override fun get(recipe: String): Recipe =
        if (recipe.startsWith(ALLWRITE_PACKAGE)) allwriteEnvironment.activateRecipes(recipe)
        else openrewriteEnvironment.activateRecipes(recipe)

    companion object {
        private const val ALLWRITE_PACKAGE = "pl.allegro.tech.allwrite.recipes"
        private const val OPEN_REWRITE_PACKAGE = "org.openrewrite"
    }
}
