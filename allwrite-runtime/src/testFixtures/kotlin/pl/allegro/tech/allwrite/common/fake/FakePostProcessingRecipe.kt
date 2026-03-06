package pl.allegro.tech.allwrite.common.fake

import pl.allegro.tech.allwrite.recipes.AllwriteRecipe
import pl.allegro.tech.allwrite.recipes.PostprocessingRecipe
import pl.allegro.tech.allwrite.recipes.PostprocessingResult
import pl.allegro.tech.allwrite.recipes.PostprocessingResult.Success
import pl.allegro.tech.allwrite.recipes.RecipeVisibility

open class FakePostProcessingRecipe(
    private val id: String = "pl.allegro.tech.allwrite.recipes.fake.postprocessing",
    private val mockedResult: PostprocessingResult = Success
) : AllwriteRecipe(
    displayName = "Fake recipe",
    description = "Fake recipe description.",
    visibility = RecipeVisibility.INTERNAL,
), PostprocessingRecipe {

    var executionCount = 0

    override fun getName(): String = id

    override fun postprocess(): PostprocessingResult {
        println("Postprocessing with result: $mockedResult")
        executionCount++
        return mockedResult
    }
}
