package pl.allegro.tech.allwrite.cli.fake.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.infrastructure.os.port.outgoing.JarFetcher
import java.nio.file.Path
import kotlin.io.path.createDirectories

@Single
internal class FakeJarFetcher : JarFetcher {

    val fetchedJars = mutableListOf<FetchedJar>()

    override fun fetch(url: String, destination: Path) {
        fetchedJars.add(FetchedJar(url, destination))
        destination.parent.createDirectories()
        destination.toFile().writeBytes(ByteArray(0))
    }

    data class FetchedJar(val url: String, val destination: Path)
}
