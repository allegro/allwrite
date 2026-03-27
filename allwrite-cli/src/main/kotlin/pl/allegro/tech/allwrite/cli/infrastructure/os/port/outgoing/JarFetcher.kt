package pl.allegro.tech.allwrite.cli.infrastructure.os.port.outgoing

import java.nio.file.Path

internal interface JarFetcher {
    fun fetch(url: String, destination: Path)
}
