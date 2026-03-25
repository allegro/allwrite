package pl.allegro.tech.allwrite.cli.application

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.runtime.port.incoming.RecipeExecutor
import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.cli.application.port.outgoing.InputFilesProvider
import pl.allegro.tech.allwrite.cli.util.JSON
import java.io.File
import java.nio.file.Files

@Single
internal class RunWithDependabotCommand(
    private val recipeMatcher: RecipeMatcher,
    private val recipeInstantiator: RecipeInstantiator,
    private val inputFilesProvider: InputFilesProvider,
    private val recipeExecutor: RecipeExecutor,
    private val pullRequestDescriptionEnricher: PullRequestDescriptionEnricher,
) : SubCommand(
    name = COMMAND_NAME,
    help = "Finds recipe by dependabot metadata and runs it"
) {

    override val hiddenFromHelp: Boolean = true

    private val pullRequestManagerExtraParams by option(
        names = arrayOf("--prm-extra"),
        envvar = ENV_VAR_RUN_DEPENDABOT_PAYLOAD_NAME
    ).required()

    private val dumpExecutionResult by option(
        names = arrayOf("--dump-execution-result"),
        completionCandidates = CompletionCandidates.Path
    ).convert { File(it) }

    override fun runSubCommand(): ExecutionResult {
        val matching = getRecipesFromDependabotMetadata()
        if (matching.isEmpty()) {
            throw PrintMessage(
                message = "No matching recipes found.",
                statusCode = 0
            )
        }

        val recipes = recipeInstantiator.instantiateAll(matching)
        pullRequestDescriptionEnricher.addRewriteDisclaimerToPullRequest(recipes)
        recipes.forEach { recipe ->
            val inputFiles = inputFilesProvider.getInputFilesFor(recipe)
            recipeExecutor.execute(recipe, inputFiles, false)
        }
        dumpExecutionResult(recipes)

        return ExecutionResult(recipes.map { recipe -> recipe.name })
    }

    private fun getRecipesFromDependabotMetadata(): List<String> {
        val dependabotMetadata = JSON.decodeFromString<PullRequestManagerExtras>(pullRequestManagerExtraParams).dependabot
        return dependabotMetadata
            .mapNotNull { it.toRecipeCoordinates() }
            .flatMap { recipeMatcher.findMatching(it) }
            .map { it.name }
            .distinct()
    }

    private fun dumpExecutionResult(recipes: List<Recipe>) {
        val dumpFile = dumpExecutionResult
        if (dumpFile != null) {
            val recipe = recipes.last()
            val executionInfo = ExecutionInfo(recipe.displayName, recipes.map { ExecutionInfoRecipe(it.name, it.displayName, it.description) })
            Files.writeString(dumpFile.toPath(), JSON.encodeToString(ExecutionInfo.serializer(), executionInfo))
        }
    }

    @Serializable
    internal data class ExecutionInfo(val title: String, val recipes: List<ExecutionInfoRecipe>)
    @Serializable
    internal data class ExecutionInfoRecipe(val id: String, val name: String, val description: String)

    companion object {

        const val ENV_VAR_RUN_DEPENDABOT_PAYLOAD_NAME: String = "PR_MANAGER_EXTRA_PARAMS"
        const val COMMAND_NAME: String = "run-dependabot"
    }
}
