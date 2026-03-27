package pl.allegro.tech.allwrite.cli.infrastructure.http

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.infrastructure.os.port.outgoing.JarFetcher
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

@Single
internal class KtorJarFetcher : JarFetcher {

    private val client = HttpClient(CIO) {
        install(HttpTimeout.Plugin) {
            requestTimeoutMillis = 300_000
        }
    }

    override fun fetch(url: String, destination: Path) {
        logger.info { "Fetching $url" }
        Files.createDirectories(destination.parent)
        runBlocking {
            client.get(url).bodyAsChannel().toInputStream().use { input ->
                Files.copy(input, destination, REPLACE_EXISTING)
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
