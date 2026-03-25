package pl.allegro.tech.allwrite.kapt.generators

import com.github.mustachejava.Mustache
import com.squareup.kotlinpoet.PropertySpec
import org.koin.core.annotation.Single
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.runtime.port.incoming.toRecipeCoordinatesOrNull
import pl.allegro.tech.allwrite.kapt.util.MustacheUtils.executeToString
import pl.allegro.tech.allwrite.kapt.util.MustacheUtils.resourceTemplate

@Single
internal class RecipeToVersionCompletionGenerator : CompletionGenerator {

    companion object {
        const val TEMPLATE_NAME = "/to-version-completion.mustache"
    }

    private val mustache: Mustache = resourceTemplate(TEMPLATE_NAME) ?:
        throw RuntimeException("Resource $TEMPLATE_NAME can't be read")

    override fun generate(recipeDescriptors: List<RecipeDescriptor>): PropertySpec {
        val options = generateFromTemplate(recipeDescriptors)
        return PropertySpec.builder("recipeToVersionCompletion", String::class)
            .initializer("%S", options)
            .build()
    }

    // todo: support short versions
    private fun generateFromTemplate(descriptors: List<RecipeDescriptor>): String {
        // Map<RecipeGroup, Map<FromVersion, List<ToVersion>>>
        val coordinatesByName: Map<String, Map<String, List<String>>> = descriptors
            .mapNotNull(RecipeDescriptor::toRecipeCoordinatesOrNull)
            .filter { coordinates -> coordinates.fromVersion != null && coordinates.toVersion != null }
            .groupBy { c -> c.canonicalName }
            .mapValues { (_, v) ->
                v.groupBy({ it.fromVersion.toString() }, { it.toVersion.toString() })
            }

        return mustache.executeToString(coordinatesByName)
    }
}
