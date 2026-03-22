package pl.allegro.tech.allwrite.cli.fake.recipes

import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.PostprocessingRecipe
import pl.allegro.tech.allwrite.PostprocessingResult
import pl.allegro.tech.allwrite.RecipeVisibility

open class FailingPostProcessingRecipe : AllwriteRecipe(visibility = RecipeVisibility.INTERNAL), PostprocessingRecipe {

    override fun postprocess(): PostprocessingResult {
        return PostprocessingResult.Failure("Something went wrong")
    }

    companion object : FailingPostProcessingRecipe()
}
