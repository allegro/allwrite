package pl.allegro.tech.allwrite.runtime.port.outgoing

import java.nio.file.Path

public interface ExternalRecipeProvider {
    public fun get(): List<Path>
}
