package pl.allegro.tech.allwrite.runtime

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.RecipeVisibility.PUBLIC
import pl.allegro.tech.allwrite.api.RecipeSource
import pl.allegro.tech.allwrite.api.tagPropertyOrNull
import pl.allegro.tech.allwrite.runtime.port.outgoing.ExternalRecipeProvider
import java.net.URLClassLoader

@Single
internal class OpenrewriteRecipeSource(
    private val externalRecipeProvider: ExternalRecipeProvider?,
) : RecipeSource {

    private val environment: Environment = buildEnvironment()

    override fun findAll(includeInternal: Boolean): List<RecipeDescriptor> =
        environment
            .listRecipeDescriptors()
            .distinctBy(RecipeDescriptor::getName)
            .filter { includeInternal || PUBLIC.name.equals(it.tagPropertyOrNull("visibility"), ignoreCase = true) }

    override fun get(recipe: String): Recipe = environment.activateRecipes(recipe)

    private fun buildEnvironment(): Environment {
        val jarPaths = externalRecipeProvider?.get().orEmpty()

        val jarUrls = jarPaths.map { it.toUri().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(jarUrls, Thread.currentThread().contextClassLoader)

        val builder = Environment.builder()
            .scanRuntimeClasspath(ALLWRITE_PACKAGE, OPEN_REWRITE_PACKAGE)
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
