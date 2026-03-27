package pl.allegro.tech.allwrite.cli.infrastructure.os

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.koin.core.annotation.Single
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.runBlocking

@Single
internal class KtorJarDownloader : JarDownloader {

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 300_000
        }
    }

    override fun download(url: String, destination: Path) {
        logger.info { "Downloading $url -> $destination" }
        Files.createDirectories(destination.parent)
        runBlocking {
            client.get(url).bodyAsChannel().toInputStream().use { input ->
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        logger.info { "Downloaded $destination" }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
