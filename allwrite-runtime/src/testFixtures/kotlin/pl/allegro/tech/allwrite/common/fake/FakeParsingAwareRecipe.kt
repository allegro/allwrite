package pl.allegro.tech.allwrite.common.fake

import pl.allegro.tech.allwrite.recipes.ParsingAwareRecipe
import java.nio.file.Path
import kotlin.io.path.pathString

class FakeParsingAwareRecipe : FakeRecipe(), ParsingAwareRecipe {

    override fun selectFilesToParse(inputFiles: List<Path>): List<Path> =
        inputFiles.filter { it.pathString.contains("interesting-dir") }
}
