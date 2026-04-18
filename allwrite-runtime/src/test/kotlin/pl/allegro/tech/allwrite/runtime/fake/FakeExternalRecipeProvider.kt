package pl.allegro.tech.allwrite.runtime.fake

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.runtime.port.outgoing.ExternalRecipeProvider
import java.nio.file.Path

@Single
class FakeExternalRecipeProvider(
    private val jarPaths: List<Path> = emptyList(),
) : ExternalRecipeProvider {
    override fun get(): List<Path> = jarPaths
}
