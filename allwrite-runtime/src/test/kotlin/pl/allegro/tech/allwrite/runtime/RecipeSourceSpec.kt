package pl.allegro.tech.allwrite.runtime

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
import org.koin.test.inject
import pl.allegro.tech.allwrite.runtime.base.BaseRuntimeSpec
import pl.allegro.tech.allwrite.runtime.port.incoming.RecipeSource

class RecipeSourceSpec : BaseRuntimeSpec() {
    init {
        val recipeSource : RecipeSource by inject()

        test("should find all allegro recipes with public visibility") {
            val recipes = recipeSource.findAll()

            val recipeIds = recipes.map { it.name }
            recipeIds shouldContainAll listOf(
                "pl.allegro.tech.allwrite.recipes.KotlinPublicRecipe",
                "pl.allegro.tech.allwrite.recipes.JavaPublicRecipe",
                "pl.allegro.tech.allwrite.recipes.YamlPublicRecipe"
            )
            recipeIds shouldNotContainAnyOf listOf(
                "pl.allegro.tech.allwrite.recipes.KotlinInternalRecipe",
                "pl.allegro.tech.allwrite.recipes.JavaInternalRecipe",
                "pl.allegro.tech.allwrite.recipes.YamlInternalRecipe",
                "pl.allegro.tech.allwrite.recipes.Java3rdPartyRecipe",
            )
        }
    }
}
