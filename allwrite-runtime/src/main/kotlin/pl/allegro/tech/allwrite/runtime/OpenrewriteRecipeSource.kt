package pl.allegro.tech.allwrite.runtime

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.RecipeVisibility.PUBLIC
import pl.allegro.tech.allwrite.runtime.port.incoming.RecipeSource
import pl.allegro.tech.allwrite.runtime.port.incoming.tagPropertyOrNull
import pl.allegro.tech.allwrite.runtime.port.outgoing.ExternalRecipeProvider
import java.net.URLClassLoader

@Single
internal class OpenrewriteRecipeSource(
    private val externalRecipeProvider: ExternalRecipeProvider?
) : RecipeSource {

    private val allwriteEnvironment = Environment.builder()
        .scanRuntimeClasspath(ALLWRITE_PACKAGE)
        .build()

    private val openrewriteEnvironment = Environment.builder()
        .scanRuntimeClasspath(OPEN_REWRITE_PACKAGE)
        .build()

    private val externalEnvironment: Environment = buildExternalEnvironment()

    override fun findAll(includeInternal: Boolean): List<RecipeDescriptor> {
        val environments = listOfNotNull(allwriteEnvironment, openrewriteEnvironment, externalEnvironment)
        return environments
            .flatMap(Environment::listRecipeDescriptors)
            .distinct()
            .filter { includeInternal || PUBLIC.name.equals(it.tagPropertyOrNull("visibility"), ignoreCase = true) }
    }

    override fun get(recipe: String): Recipe =
        when {
            recipe.startsWith(ALLWRITE_PACKAGE) -> allwriteEnvironment.activateRecipes(recipe)
            else -> externalEnvironment.activateRecipesOrNull(recipe) ?: openrewriteEnvironment.activateRecipes(recipe)
        }

    private fun buildExternalEnvironment(): Environment {
        val jarPaths = externalRecipeProvider?.get().orEmpty()

        val jarUrls = jarPaths.map { it.toUri().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(jarUrls, Thread.currentThread().contextClassLoader)

        val builder = Environment.builder()
        jarPaths.forEach { jarPath ->
            logger.debug { "Scanning external recipe JAR: $jarPath" }
            builder.scanJar(jarPath, emptyList(), classLoader)
        }
        return builder.build()
    }

    companion object {

        private val logger = KotlinLogging.logger { }
        private const val ALLWRITE_PACKAGE = "pl.allegro.tech.allwrite.recipes"
        private const val OPEN_REWRITE_PACKAGE = "org.openrewrite"
    }
}

private fun Environment.activateRecipesOrNull(vararg recipes: String) =
    try {
        activateRecipes(*recipes)
    } catch (_: Exception) {
        null
    }

