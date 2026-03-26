package pl.allegro.tech.allwrite.runtime.fake

import pl.allegro.tech.allwrite.spi.AllwriteRecipe
import pl.allegro.tech.allwrite.spi.PostprocessingRecipe
import pl.allegro.tech.allwrite.spi.PostprocessingResult
import pl.allegro.tech.allwrite.spi.PostprocessingResult.Success
import pl.allegro.tech.allwrite.spi.RecipeVisibility

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
