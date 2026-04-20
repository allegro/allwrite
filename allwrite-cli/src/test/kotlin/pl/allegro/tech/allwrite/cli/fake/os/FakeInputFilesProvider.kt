package pl.allegro.tech.allwrite.cli.fake.os

import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.cli.application.port.outgoing.InputFilesProvider
import java.nio.file.Path

@Single
class FakeInputFilesProvider : InputFilesProvider {

    override fun getInputFilesFor(recipe: Recipe): List<Path> = emptyList()
}
