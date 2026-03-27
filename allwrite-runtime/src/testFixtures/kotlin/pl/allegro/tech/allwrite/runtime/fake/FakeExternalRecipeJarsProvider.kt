package pl.allegro.tech.allwrite.runtime.fake

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.runtime.port.outgoing.ExternalRecipeJarsProvider
import java.nio.file.Path

@Single
class FakeExternalRecipeJarsProvider(
    private val jarPaths: List<Path> = emptyList()
) : ExternalRecipeJarsProvider {
    override fun getJarPaths(): List<Path> = jarPaths
}
