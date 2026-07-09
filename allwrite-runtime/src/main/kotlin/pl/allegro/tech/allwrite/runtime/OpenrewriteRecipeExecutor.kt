package pl.allegro.tech.allwrite.runtime

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.api.RecipeExecutor
import pl.allegro.tech.allwrite.runtime.port.outgoing.UserProblemReporter
import java.nio.file.Path

@Single
internal class OpenrewriteRecipeExecutor(
    private val sourceFilesParser: SourceFilesParser,
    private val sourceFileStore: WorkdirSourceFileApplier,
    private val userProblemReporter: UserProblemReporter?,
    private val recipeExecutionPlanner: RecipeExecutionPlanner,
) : RecipeExecutor {

    override fun execute(recipe: Recipe, inputFiles: List<Path>, failOnError: Boolean) {
        logger.info { "Running recipe ${recipe.name}" }
        val context = errorLoggingExecutionContext(failOnError)
        val plan = recipeExecutionPlanner.plan(recipe)
        plan.execute(inputFiles, context, sourceFilesParser, sourceFileStore, userProblemReporter)
    }

    private fun errorLoggingExecutionContext(failOnError: Boolean) =
        InMemoryExecutionContext {
            logger.debug { it.message }
            if (failOnError) {
                throw it
            }
        }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
