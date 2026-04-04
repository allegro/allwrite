package pl.allegro.tech.allwrite.runtime

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.RecipeRun
import org.openrewrite.SourceFile
import org.openrewrite.internal.InMemoryLargeSourceSet
import pl.allegro.tech.allwrite.runtime.port.incoming.RecipeExecutor
import pl.allegro.tech.allwrite.runtime.port.outgoing.Problem
import pl.allegro.tech.allwrite.runtime.port.outgoing.UserProblemReporter
import pl.allegro.tech.allwrite.runtime.util.WORKDIR
import pl.allegro.tech.allwrite.PostprocessingRecipe
import pl.allegro.tech.allwrite.PostprocessingResult
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

@Single
internal class OpenrewriteRecipeExecutor(
    private val sourceFilesParser: SourceFilesParser,
    private val userProblemReporter: UserProblemReporter?,
) : RecipeExecutor {

    override fun execute(recipe: Recipe, inputFiles: List<Path>, failOnError: Boolean) {
        val context = errorLoggingExecutionContext(failOnError)
        val sourceFiles = parseInputFiles(recipe, inputFiles, context)
        val recipeRun = runRecipe(recipe, sourceFiles, context)
        applyChanges(recipeRun)
        postProcess(recipe)
    }

    private fun parseInputFiles(recipe: Recipe, inputFiles: List<Path>, context: ExecutionContext): List<SourceFile> {
        return sourceFilesParser.parseSourceFiles(recipe, inputFiles, context)
    }

    private fun runRecipe(recipe: Recipe, sourceFiles: List<SourceFile>, context: ExecutionContext): RecipeRun {
        logger.info { "Running recipe ${recipe.name}" }
        return recipe.run(InMemoryLargeSourceSet(sourceFiles), context)
    }

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
        allRecipes(recipe)
            .filterIsInstance<PostprocessingRecipe>()
            .map { postprocessingRecipe ->
                logger.info { "Applying post-processing recipe: ${postprocessingRecipe.javaClass.simpleName}" }
                postprocessingRecipe.postprocess()
            }
            .filterIsInstance<PostprocessingResult.Failure>()
            .forEach { result -> userProblemReporter?.reportProblem(Problem(result.errorMessage)) }
    }

    private fun allRecipes(recipe: Recipe): List<Recipe> =
        listOf(recipe) + recipe.recipeList.flatMap(::allRecipes)

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
