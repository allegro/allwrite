package pl.allegro.tech.allwrite.runtime.port.outgoing

import java.nio.file.Path

public interface ExternalRecipeJarsProvider {
    public fun getJarPaths(): List<Path>
}
