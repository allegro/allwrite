package pl.allegro.tech.allwrite.buildlogic

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

@CacheableTask
abstract class ExtractJdkTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract var distributionPath: Provider<Path>

    @Input
    val target: Property<JdkDistribution> = project.objects.property(JdkDistribution::class.java)

    @get:OutputDirectory
    val targetDir: Property<Directory> = project.objects.directoryProperty()

    @TaskAction
    fun extract() {
        val archivePath = distributionPath.get()
        val targetPath = targetDir.get()

        when (target.get().archiveFormat) {
            "tar.gz" -> {
                TarArchiveInputStream(GZIPInputStream(BufferedInputStream(Files.newInputStream(archivePath)))).use { tarIn ->
                    generateSequence { tarIn.nextEntry }
                        .filter { !it.isDirectory }
                        .forEach { entry -> extractEntry(entry.name, targetPath, tarIn) }
                }
            }
            "zip" -> {
                ZipInputStream(BufferedInputStream(Files.newInputStream(archivePath))).use { zipIn ->
                    generateSequence { zipIn.nextEntry }
                        .filter { !it.isDirectory }
                        .forEach { entry -> extractEntry(entry.name, targetPath, zipIn) }
                }
            }
        }
    }

    private fun extractEntry(entryName: String, targetPath: Directory, inputStream: InputStream) {
        val nameWithoutJdkVersion = entryName.replace("jdk[^/]*/".toRegex(), "jdk/")
        val outputFile = File(targetPath.asFile, nameWithoutJdkVersion)
        outputFile.parentFile.mkdirs()
        BufferedOutputStream(outputFile.outputStream()).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }
}
