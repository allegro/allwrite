package pl.allegro.tech.allwrite.runtime

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.runtime.port.outgoing.ExternalRecipeJarsProvider
import java.nio.file.Path

@Single
internal class NoOpExternalRecipeJarsProvider : ExternalRecipeJarsProvider {
    override fun getJarPaths(): List<Path> = emptyList()
}
