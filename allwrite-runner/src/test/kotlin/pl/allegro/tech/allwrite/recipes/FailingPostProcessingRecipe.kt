package pl.allegro.tech.allwrite.recipes

open class FailingPostProcessingRecipe : AllwriteRecipe(visibility = RecipeVisibility.INTERNAL), PostprocessingRecipe {

    override fun postprocess(): PostprocessingResult {
        return PostprocessingResult.Failure("Something went wrong")
    }

    companion object : FailingPostProcessingRecipe()
}
