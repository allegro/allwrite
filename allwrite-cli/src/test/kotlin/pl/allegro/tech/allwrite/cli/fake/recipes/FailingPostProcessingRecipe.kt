package pl.allegro.tech.allwrite.cli.fake.recipes

import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.PostprocessingRecipe
import pl.allegro.tech.allwrite.PostprocessingResult

open class FailingPostProcessingRecipe :
    AllwriteRecipe(),
    PostprocessingRecipe {

    override fun postprocess(): PostprocessingResult = PostprocessingResult.Failure("Something went wrong")

    companion object : FailingPostProcessingRecipe()
}
