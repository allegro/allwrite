package pl.allegro.tech.allwrite.runtime

import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.RecipeRun
import org.openrewrite.SourceFile
import org.openrewrite.internal.InMemoryLargeSourceSet
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.PostprocessingRecipe
import pl.allegro.tech.allwrite.PostprocessingResult
import pl.allegro.tech.allwrite.runtime.port.outgoing.UserProblemReporter
import pl.allegro.tech.allwrite.runtime.port.outgoing.Problem
import pl.allegro.tech.allwrite.runtime.util.withNestedRecipes
import java.nio.file.Path
import kotlin.io.path.exists

internal data class RecipeExecutionPhase(
    val recipes: List<Recipe>,
) {

    val size: Int
        get() = recipes.size

    val isClasspathAware: Boolean
        get() = recipes.size == 1 && recipes.single() is ClasspathAwareRecipe

    val primaryRecipe: Recipe
        get() = recipes.single()

    fun toRecipe(): Recipe = recipes.singleOrNull() ?: PhaseRecipe(recipes)

    fun execute(
        inputFiles: List<Path>,
        context: ExecutionContext,
        sourceFilesParser: SourceFilesParser,
        sourceFileStore: WorkdirSourceFileApplier,
        userProblemReporter: UserProblemReporter?,
    ) {
        val recipe = toRecipe()
        val existingInputFiles = inputFiles.filter { it.exists() }
        val sourceFiles = sourceFilesParser.parseSourceFiles(recipe, existingInputFiles, context)
        val recipeRun = runRecipe(recipe, sourceFiles, context)
        sourceFileStore.persist(recipeRun)
        postProcess(recipe, userProblemReporter)
    }

    private fun runRecipe(recipe: Recipe, sourceFiles: List<SourceFile>, context: ExecutionContext): RecipeRun =
        recipe.run(InMemoryLargeSourceSet(sourceFiles), context)

    private fun postProcess(recipe: Recipe, userProblemReporter: UserProblemReporter?) {
        recipe.withNestedRecipes()
            .filterIsInstance<PostprocessingRecipe>()
            .map { postprocessingRecipe ->
                logger.info { "Applying post-processing recipe: ${postprocessingRecipe.javaClass.simpleName}" }
                postprocessingRecipe.postprocess()
            }
            .filterIsInstance<PostprocessingResult.Failure>()
            .forEach { result -> userProblemReporter?.reportProblem(Problem(result.errorMessage)) }
    }

    companion object {
        private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger { }
    }
}

internal class PhaseRecipe(
    private val recipes: List<Recipe>,
) : Recipe() {

    override fun getDisplayName(): String = "Isolated execution phase"

    override fun getDescription(): String = "Groups recipes that share a single isolated parsing/execution pass."

    override fun getRecipeList(): List<Recipe> = recipes
}
