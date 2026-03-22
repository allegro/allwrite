package pl.allegro.tech.allwrite.cli.infrastructure.os

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.cli.application.port.outgoing.InputFilesProvider
import pl.allegro.tech.allwrite.cli.util.WORKDIR
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.walk

@Single
internal class GitManagedInputFilesProvider(
    private val commandExecutor: SystemCommandExecutor,
) : InputFilesProvider {

    override fun getInputFilesFor(recipe: Recipe): List<Path> {
        try {
            val inputFiles = commandExecutor.exec("git ls-tree -r HEAD --name-only")
                .lines()
                .map { WORKDIR.resolve(it) }
                .filter(Path::exists)

            logger.info { "Found ${inputFiles.size} input files" }

            return inputFiles

        } catch (e: Exception) {
            val message = "Not a git repository or git is not installed, falling back to processing all files"

            if (logger.isDebugEnabled()) {
                logger.warn(e) { message }
            } else {
                logger.warn { message }
            }
            return WORKDIR.walk().toList()
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
