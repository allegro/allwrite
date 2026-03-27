package pl.allegro.tech.allwrite.buildlogic

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.text.format

@CacheableTask
abstract class FetchJdkTask : DefaultTask() {

    @Input
    val target: Property<JdkDistribution> = project.objects.property(JdkDistribution::class.java)

    @get:OutputFile
    abstract var distributionPath: Provider<Path>

    @get:OutputFile
    abstract var signaturePath: Provider<Path>

    @get:OutputFile
    abstract var certPath: Provider<Path>

    @TaskAction
    fun fetchJdk() {
        val target = target.get()
        val fileName = JDK_FILE_NAME_TEMPLATE.format(target.temurinName, target.archiveFormat)
        val jdkUrl = "$JDK_REPOSITORY_BASE_URL/$fileName"

        fetchFileIfNotExists(jdkUrl, distributionPath.get())
        fetchFileIfNotExists("$jdkUrl.sig", this.signaturePath.get())
        fetchFileIfNotExists(CERT_URL, this.certPath.get())
    }

    private fun fetchFileIfNotExists(url: String, destination: Path) {
        // the task is reported as not UP-TO-DATE everytime we edit any file in buildSrc
        // to mitigate this issue, we manaully check whether the file exists
        if (!destination.exists()) {
            logger.lifecycle("Fetching $url -> $destination")
            Files.newOutputStream(destination).use { out ->
                runBlocking { client.get(url).bodyAsChannel().copyTo(out) }
            }
        }
    }

    companion object {
        const val JDK_FILE_NAME_TEMPLATE = "OpenJDK21U-jdk_%s_hotspot_21.0.3_9.%s"
        const val JDK_REPOSITORY_BASE_URL =
            "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.3%2B9"
        const val CERT_URL =
            "http://keyserver.ubuntu.com/pks/lookup?op=get&search=0x3B04D753C9050D9A5D343F39843C48A565F8F04B"

        private val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 300_000 // 5 minutes
            }
        }
    }
}
