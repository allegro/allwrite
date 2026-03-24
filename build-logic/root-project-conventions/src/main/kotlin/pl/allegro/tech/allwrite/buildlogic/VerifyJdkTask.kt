package pl.allegro.tech.allwrite.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.pgpainless.sop.SOPImpl
import java.nio.file.Files
import java.nio.file.Path

@CacheableTask
abstract class VerifyJdkTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract var distributionPath: Provider<Path>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract var signaturePath: Provider<Path>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract var certPath: Provider<Path>

    @TaskAction
    fun verify() {
        try {
            pgp.verify()
                .cert(Files.readAllBytes(certPath.get()))
                .signatures(Files.readAllBytes(signaturePath.get()))
                .data(Files.readAllBytes(distributionPath.get()))
        } catch (e: Exception) {
            logger.error("Can't verify JDK dist signature: distribution at ${distributionPath.get().toAbsolutePath()} does not match signature at ${signaturePath.get().toAbsolutePath()}")
            throw e
        }
    }

    companion object {
        private val pgp = SOPImpl()
    }
}
