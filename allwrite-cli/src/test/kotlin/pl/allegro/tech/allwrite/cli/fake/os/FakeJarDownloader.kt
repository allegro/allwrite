package pl.allegro.tech.allwrite.cli.fake.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.infrastructure.os.JarDownloader
import java.nio.file.Path
import kotlin.io.path.createDirectories

@Single
internal class FakeJarDownloader : JarDownloader {

    val downloadedJars = mutableListOf<DownloadRecord>()

    override fun download(url: String, destination: Path) {
        downloadedJars.add(DownloadRecord(url, destination))
        destination.parent.createDirectories()
        destination.toFile().writeBytes(ByteArray(0))
    }

    data class DownloadRecord(val url: String, val destination: Path)
}
