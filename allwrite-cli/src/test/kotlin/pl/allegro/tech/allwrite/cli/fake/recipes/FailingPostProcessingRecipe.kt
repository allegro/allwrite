package pl.allegro.tech.allwrite.cli.fake.recipes

import pl.allegro.tech.allwrite.spi.AllwriteRecipe
import pl.allegro.tech.allwrite.spi.PostprocessingRecipe
import pl.allegro.tech.allwrite.spi.PostprocessingResult
import pl.allegro.tech.allwrite.spi.RecipeVisibility

open class FailingPostProcessingRecipe : AllwriteRecipe(visibility = RecipeVisibility.INTERNAL), PostprocessingRecipe {

    override fun postprocess(): PostprocessingResult {
        return PostprocessingResult.Failure("Something went wrong")
    }

    companion object : FailingPostProcessingRecipe()
}
