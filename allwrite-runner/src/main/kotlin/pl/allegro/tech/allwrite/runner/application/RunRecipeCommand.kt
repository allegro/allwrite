package pl.allegro.tech.allwrite.runner.application

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.common.port.incoming.RecipeExecutor
import pl.allegro.tech.allwrite.runner.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.runner.application.port.outgoing.InputFilesProvider
import pl.allegro.tech.allwrite.runner.recipeIdOptions
import pl.allegro.tech.allwrite.runner.util.convertLazy
import pl.allegro.tech.allwrite.runner.util.lazy
import java.io.File

private val RECIPE_ID_COMPLETION: CompletionCandidates = CompletionCandidates.Fixed(recipeIdOptions.toSet())

@Single
internal class RunRecipeCommand(
    private val recipeInstantiator: RecipeInstantiator,
    private val inputFilesProvider: InputFilesProvider,
    private val recipeExecutor: RecipeExecutor,
) : SubCommand(name = COMMAND_NAME, help = "Runs a recipe or list of recipes by its id") {

    private val recipeIdOption = option(
        names = arrayOf("--recipe"),
        completionCandidates = RECIPE_ID_COMPLETION
    ).convertLazy { listOf(recipeInstantiator.instantiate(it)) }

    private val fileOption = option(
        names = arrayOf("--file"),
        help = "Path to a json file with list of recipes. JSON object should contain a single " +
            "array-of-strings field called `recipes`",
        completionCandidates = CompletionCandidates.Path
    ).convertLazy { recipeInstantiator.instantiateFrom(File(it)) }

    private val failOnError: Boolean by option(
        names = arrayOf("--fail-on-error"),
        help = "Fail the execution if an error occurs. Default behavior is to continue running visitors after an error in one.",
        completionCandidates = CompletionCandidates.None
    ).flag("--continue-on-error", default = false)

    private val recipeList: List<Recipe> by mutuallyExclusiveOptions(recipeIdOption, fileOption)
        .single()
        .required()
        .lazy()

    override fun runSubCommand(): ExecutionResult {
        recipeList.forEach { recipe ->
            val inputFiles = inputFilesProvider.getInputFilesFor(recipe)
            recipeExecutor.execute(recipe, inputFiles, failOnError)
        }

        return ExecutionResult(recipeList.map { recipe -> recipe.name })
    }

    companion object {

        const val COMMAND_NAME: String = "run-recipe"
    }
}
