package pl.allegro.tech.allwrite.runtime

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrewrite.ExecutionContext
import pl.allegro.tech.allwrite.runtime.port.outgoing.UserProblemReporter

internal data class RecipeExecutionPlan(
    val phases: List<RecipeExecutionPhase>,
) {

    val requiresIsolation: Boolean
        get() = phases.size > 1

    fun execute(
        inputFiles: List<java.nio.file.Path>,
        context: ExecutionContext,
        sourceFilesParser: SourceFilesParser,
        sourceFileStore: WorkdirSourceFileApplier,
        userProblemReporter: UserProblemReporter?,
    ) {
        if (!requiresIsolation) {
            phases.single().execute(inputFiles, context, sourceFilesParser, sourceFileStore, userProblemReporter)
            return
        }

        logger.info { "Detected ClasspathAware recipes, splitting into ${phases.size} isolated phases" }
        phases.forEachIndexed { idx, phase ->
            val phaseNumber = idx + 1
            when {
                phase.isClasspathAware -> logger.info { "Phase $phaseNumber: ${phase.primaryRecipe.javaClass.simpleName} (classpath-aware recipe)" }
                phase.size == 1 -> logger.info { "Phase $phaseNumber: ${phase.primaryRecipe.javaClass.simpleName}" }
                else -> {
                    logger.info { "Phase $phaseNumber: ${phase.size} non-classpath-aware recipes" }
                    logger.debug { "Grouped recipes: ${phase.recipes.map { it.javaClass.simpleName }}" }
                }
            }

            phase.execute(inputFiles, context, sourceFilesParser, sourceFileStore, userProblemReporter)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
