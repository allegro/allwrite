package pl.allegro.tech.allwrite.cli.infrastructure.os

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.util.JSON
import pl.allegro.tech.allwrite.runtime.port.outgoing.ExternalRecipeJarsProvider
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Single
internal class ExternalRecipeStore(
    private val jarDownloader: JarDownloader,
    private val allwriteHome: AllwriteHome
) : ExternalRecipeJarsProvider {

    override fun getJarPaths(): List<Path> =
        readConfig().recipes.keys
            .map { jarPath(it) }
            .filter { it.exists() }

    fun add(name: String, url: String) {
        val config = readConfig()
        require(name !in config.recipes) { "External recipe '$name' already exists. Use 'update' to change its URL." }
        val updatedConfig = config.copy(recipes = config.recipes + (name to ExternalRecipeEntry(url)))
        writeConfig(updatedConfig)
        jarDownloader.download(url, jarPath(name))
        logger.info { "Added external recipe '$name' from $url" }
    }

    fun update(name: String, url: String) {
        val config = readConfig()
        require(name in config.recipes) { "External recipe '$name' not found. Use 'add' to register it first." }
        val updatedConfig = config.copy(recipes = config.recipes + (name to ExternalRecipeEntry(url)))
        writeConfig(updatedConfig)
        jarDownloader.download(url, jarPath(name))
        logger.info { "Updated external recipe '$name' to $url" }
    }

    fun remove(name: String) {
        val config = readConfig()
        require(name in config.recipes) { "External recipe '$name' not found." }
        val updatedConfig = config.copy(recipes = config.recipes - name)
        writeConfig(updatedConfig)
        jarPath(name).deleteIfExists()
        logger.info { "Removed external recipe '$name'" }
    }

    fun refresh(name: String) {
        val config = readConfig()
        val entry = config.recipes[name]
        require(entry != null) { "External recipe '$name' not found." }
        jarDownloader.download(entry.url, jarPath(name))
        logger.info { "Refreshed external recipe '$name' from ${entry.url}" }
    }

    private fun readConfig(): ExternalRecipesConfig {
        val configFile = configFilePath()
        if (!configFile.exists()) return ExternalRecipesConfig()
        return JSON.decodeFromString<ExternalRecipesConfig>(configFile.readText())
    }

    private fun writeConfig(config: ExternalRecipesConfig) {
        val configFile = configFilePath()
        configFile.parent.createDirectories()
        configFile.writeText(JSON.encodeToString(config))
    }

    private fun jarPath(name: String): Path = allwriteHome.path.resolve("jars").resolve("$name.jar")

    private fun configFilePath(): Path = allwriteHome.path.resolve("external-recipes.json")

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

@Serializable
internal data class ExternalRecipesConfig(
    val recipes: Map<String, ExternalRecipeEntry> = emptyMap()
)

@Serializable
internal data class ExternalRecipeEntry(
    val url: String
)
