package pl.allegro.tech.allwrite.kapt.generators

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import org.koin.core.annotation.Single
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.api.RecipeCoordinates
import pl.allegro.tech.allwrite.api.toRecipeCoordinatesOrNull

@Single
internal class RecipeCanonicalNameCompletionGenerator : CompletionGenerator {

    override fun generate(recipeDescriptors: List<RecipeDescriptor>): PropertySpec {
        val recipeCanonicalNames = recipeDescriptors
            .mapNotNull(RecipeDescriptor::toRecipeCoordinatesOrNull)
            .map(RecipeCoordinates::canonicalName)
            .toSet()

        return PropertySpec.builder("recipeCanonicalNameOptions", SET.parameterizedBy(STRING))
            .initializer(
                "%L",
                recipeCanonicalNames.joinToString(prefix = "setOf(", postfix = ")", separator = ", ") { "\"$it\"" },
            )
            .build()
    }
}
