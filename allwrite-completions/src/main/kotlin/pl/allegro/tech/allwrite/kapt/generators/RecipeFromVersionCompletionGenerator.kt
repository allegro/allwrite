package pl.allegro.tech.allwrite.kapt.generators

import com.github.mustachejava.Mustache
import com.squareup.kotlinpoet.PropertySpec
import org.koin.core.annotation.Single
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.runtime.port.incoming.toRecipeCoordinatesOrNull
import pl.allegro.tech.allwrite.kapt.util.MustacheUtils.executeToString
import pl.allegro.tech.allwrite.kapt.util.MustacheUtils.resourceTemplate

@Single
internal class RecipeFromVersionCompletionGenerator : CompletionGenerator {

    companion object {
        const val TEMPLATE_NAME = "/from-version-completion.mustache"
    }

    private val mustache: Mustache = resourceTemplate(TEMPLATE_NAME) ?:
        throw RuntimeException("Resource $TEMPLATE_NAME can't be read")


    override fun generate(recipeDescriptors: List<RecipeDescriptor>): PropertySpec {
        val options = generateFromTemplate(recipeDescriptors)
        return PropertySpec.builder("recipeFromVersionCompletion", String::class)
            .initializer("%S", options)
            .build()
    }

    private fun generateFromTemplate(descriptors: List<RecipeDescriptor>): String {
        val coordinatesByName = descriptors
            .mapNotNull(RecipeDescriptor::toRecipeCoordinatesOrNull)
            .filter { coordinates -> coordinates.fromVersion != null && coordinates.toVersion != null }
            .groupBy({ c -> c.canonicalName }, { c -> c.fromVersion.toString() })

        return mustache.executeToString(coordinatesByName)
    }
}
