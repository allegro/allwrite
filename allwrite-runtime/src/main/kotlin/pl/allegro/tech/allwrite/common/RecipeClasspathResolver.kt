package pl.allegro.tech.allwrite.common

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.walk

@Single
internal class RecipeClasspathResolver {

    init {
        cleanupTempFiles()
    }

    /**
     * Get classpath for the [recipe] and for all its children recursively
     */
    fun resolveClasspathTransitive(recipe: Recipe): List<Path> {
        val classpath = resolveClasspathNonTransitive(recipe.name)
        val childClasspaths = recipe.recipeList.flatMap(::resolveClasspathTransitive)
        return (classpath + childClasspaths).distinctBy { it.fileName }
    }

    /**
     * Get classpath for the recipe named [recipeName] without classpaths of its children
     */
    fun resolveClasspathNonTransitive(recipeName: String): List<Path> {
        val recipeClasspathUri = javaClass.getResource("/$recipeName")?.toURI()
        return recipeClasspathUri?.toPath()?.walk()?.toList()?.map(Path::zipEntryToTempFile).orEmpty()
    }

    /**
     * Delete temp files just in case the previous run crashed
     */
    private fun cleanupTempFiles() {
        try {
            Paths.get(System.getProperty("java.io.tmpdir"))
                .listDirectoryEntries()
                .filter { it.isRegularFile() && it.name.startsWith(TEMP_FILE_REFIX) }
                .forEach { it.deleteIfExists() }
        } catch (e: Exception) {
            logger.warn { "Failed to clean up temporary files, cause: ${e.message}" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

private fun URI.toPath(): Path =
    try {
        Paths.get(this)
    } catch (_: FileSystemNotFoundException) {
        FileSystems.newFileSystem(this, emptyMap<String, Any>())
        Paths.get(this)
    }

private fun Path.zipEntryToTempFile(): Path {
    val tempFile = File.createTempFile(TEMP_FILE_REFIX, "-$fileName")
    tempFile.deleteOnExit()
    FileOutputStream(tempFile).use { out -> Files.copy(this, out) }
    return tempFile.toPath()
}

private const val TEMP_FILE_REFIX = "allwrite-"
