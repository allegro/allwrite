package pl.allegro.tech.allwrite.runtime

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.RecipeRun
import org.openrewrite.SourceFile
import org.openrewrite.internal.InMemoryLargeSourceSet
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.PostprocessingRecipe
import pl.allegro.tech.allwrite.PostprocessingResult
import pl.allegro.tech.allwrite.api.RecipeExecutor
import pl.allegro.tech.allwrite.runtime.port.outgoing.Problem
import pl.allegro.tech.allwrite.runtime.port.outgoing.UserProblemReporter
import pl.allegro.tech.allwrite.runtime.util.WORKDIR
import pl.allegro.tech.allwrite.runtime.util.withNestedRecipes
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.writeText

@Single
internal class OpenrewriteRecipeExecutor(
    private val sourceFilesParser: SourceFilesParser,
    private val userProblemReporter: UserProblemReporter?,
) : RecipeExecutor {

    override fun execute(recipe: Recipe, inputFiles: List<Path>, failOnError: Boolean) {
        logger.info { "Running recipe ${recipe.name}" }
        val context = errorLoggingExecutionContext(failOnError)
        val phases = splitIntoPhases(recipe)

        if (phases.size <= 1) {
            executePhase(recipe, inputFiles, context)
            return
        }

        logger.info { "Detected ClasspathAware recipes, splitting into ${phases.size} isolated phases" }
        phases.forEachIndexed { idx, p ->
            val phaseNumber = idx + 1
            if (p.size == 1) {
                logger.info { "Phase $phaseNumber: ${p.single().javaClass.simpleName} (classpath-aware recipe)" }
            } else {
                logger.info { "Phase $phaseNumber: ${p.size} non-classpath-aware recipes" }
                logger.debug { "Grouped recipes: ${p.map { it.javaClass.simpleName }}" }
            }

            val phaseRecipe = toPhaseRecipe(p)
            val existingInputFiles = inputFiles.filter { it.exists() }
            executePhase(phaseRecipe, existingInputFiles, context)
        }
    }

    private fun splitIntoPhases(recipe: Recipe): List<List<Recipe>> {
        if (recipe !is AllwriteRecipe) return listOf(listOf(recipe))

        val subRecipes = recipe.recipeList
        if (subRecipes.none { it is ClasspathAwareRecipe || it.needsExpansion() }) return listOf(listOf(recipe))

        val phases = mutableListOf<List<Recipe>>()
        var currentGroup = mutableListOf<Recipe>()
        for (subRecipe in subRecipes) {
            when {
                subRecipe is ClasspathAwareRecipe -> {
                    if (currentGroup.isNotEmpty()) {
                        phases.add(currentGroup)
                        currentGroup = mutableListOf()
                    }

                    phases.add(listOf(subRecipe))
                }

                subRecipe.needsExpansion() -> {
                    if (currentGroup.isNotEmpty()) {
                        phases.add(currentGroup)
                        currentGroup = mutableListOf()
                    }

                    val nestedPhases = splitIntoPhases(subRecipe)
                    logger.info { "Expanding nested recipe ${subRecipe.javaClass.simpleName} (${nestedPhases.size} sub-phases)" }
                    phases.addAll(nestedPhases)
                }

                else -> currentGroup.add(subRecipe)
            }
        }

        if (currentGroup.isNotEmpty()) {
            phases.add(currentGroup)
        }

        return phases
    }

    private fun Recipe.needsExpansion(): Boolean = this is AllwriteRecipe && recipeList.any { it is ClasspathAwareRecipe || it.needsExpansion() }

    private fun toPhaseRecipe(phaseRecipes: List<Recipe>): Recipe = phaseRecipes.singleOrNull() ?: PhaseRecipe(phaseRecipes)

    private fun executePhase(recipe: Recipe, inputFiles: List<Path>, context: ExecutionContext) {
        val sourceFiles = parseInputFiles(recipe, inputFiles, context)
        val recipeRun = runRecipe(recipe, sourceFiles, context)
        applyChanges(recipeRun)
        postProcess(recipe)
    }

    private fun parseInputFiles(recipe: Recipe, inputFiles: List<Path>, context: ExecutionContext): List<SourceFile> =
        sourceFilesParser.parseSourceFiles(recipe, inputFiles, context)

    private fun runRecipe(recipe: Recipe, sourceFiles: List<SourceFile>, context: ExecutionContext): RecipeRun =
        recipe.run(InMemoryLargeSourceSet(sourceFiles), context)

    private fun applyChanges(recipeRun: RecipeRun) {
        val modifiedFiles = recipeRun.changeset.allResults
            .filter { it.after != null }
            .mapNotNull { it.after }

        val deletedFiles = recipeRun.changeset.allResults
            .filter { it.before != null && it.after == null }
            .mapNotNull { it.before }

        modifiedFiles.forEach(::overwriteSourceFile)
        deletedFiles.forEach(::deleteSourceFile)

        logger.info { "Run finished with ${modifiedFiles.size} files modified and ${deletedFiles.size} files deleted" }
    }

    private fun overwriteSourceFile(sourceFile: SourceFile) {
        logger.info { "Modified file: ${sourceFile.sourcePath.absolutePathString()}" }
        val outputFile = WORKDIR.resolve(sourceFile.sourcePath)
        outputFile.writeText(sourceFile.printAll())
    }

    private fun deleteSourceFile(sourceFile: SourceFile) {
        logger.info { "Deleted file: ${sourceFile.sourcePath.absolutePathString()}" }
        val fileToDelete = WORKDIR.resolve(sourceFile.sourcePath)
        fileToDelete.deleteIfExists()
    }

    private fun postProcess(recipe: Recipe) {
        recipe.withNestedRecipes()
            .filterIsInstance<PostprocessingRecipe>()
            .map { postprocessingRecipe ->
                logger.info { "Applying post-processing recipe: ${postprocessingRecipe.javaClass.simpleName}" }
                postprocessingRecipe.postprocess()
            }
            .filterIsInstance<PostprocessingResult.Failure>()
            .forEach { result -> userProblemReporter?.reportProblem(Problem(result.errorMessage)) }
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

/**
 * Wraps a group of recipes belonging to the same isolated phase so that they can be run together via [Recipe.run].
 */
internal class PhaseRecipe(
    private val recipes: List<Recipe>,
) : Recipe() {

    override fun getDisplayName(): String = "Isolated execution phase"

    override fun getDescription(): String = "Groups recipes that share a single isolated parsing/execution pass."

    override fun getRecipeList(): List<Recipe> = recipes
}
