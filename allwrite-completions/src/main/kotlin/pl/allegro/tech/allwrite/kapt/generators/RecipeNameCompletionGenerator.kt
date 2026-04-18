package pl.allegro.tech.allwrite.kapt.generators

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.STRING
import org.koin.core.annotation.Single
import org.openrewrite.config.RecipeDescriptor

@Single
internal class RecipeNameCompletionGenerator : CompletionGenerator {

    override fun generate(recipeDescriptors: List<RecipeDescriptor>): PropertySpec {
        val recipes = recipeDescriptors.map { it.name }
        return PropertySpec.builder("recipeIdOptions", SET.parameterizedBy(STRING))
            .initializer(
                "%L",
                recipes.joinToString(prefix = "setOf(", postfix = ")", separator = ", ") { "\"$it\"" },
            )
            .build()
    }
}
