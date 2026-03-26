package pl.allegro.tech.allwrite.runtime.fake

import pl.allegro.tech.allwrite.spi.ParsingAwareRecipe
import java.nio.file.Path
import kotlin.io.path.pathString

class FakeParsingAwareRecipe : FakeRecipe(), ParsingAwareRecipe {

    override fun selectFilesToParse(inputFiles: List<Path>): List<Path> =
        inputFiles.filter { it.pathString.contains("interesting-dir") }
}
