package pl.allegro.tech.allwrite.runner.application

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.zafarkhaja.semver.Version
import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.common.port.incoming.RecipeCoordinates
import pl.allegro.tech.allwrite.common.port.incoming.RecipeExecutor
import pl.allegro.tech.allwrite.runner.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.runner.application.Messages.LIST_RECIPES_HINT
import pl.allegro.tech.allwrite.runner.application.port.outgoing.InputFilesProvider
import pl.allegro.tech.allwrite.runner.recipeCanonicalNameOptions
import pl.allegro.tech.allwrite.runner.recipeFromVersionCompletion
import pl.allegro.tech.allwrite.runner.recipeToVersionCompletion
import pl.allegro.tech.allwrite.runner.util.customCompletion

private val RECIPE_CANONICAL_NAME_COMPLETION: CompletionCandidates = CompletionCandidates.Fixed(recipeCanonicalNameOptions)
private val FROM_VERSION_COMPLETION: CompletionCandidates = customCompletion(recipeFromVersionCompletion)
private val TO_VERSION_COMPLETION: CompletionCandidates = customCompletion(recipeToVersionCompletion)

@Single
internal class RunCommand(
    private val recipeMatcher: RecipeMatcher,
    private val recipeInstantiator: RecipeInstantiator,
    private val inputFilesProvider: InputFilesProvider,
    private val recipeExecutor: RecipeExecutor
) : SubCommand(
    name = COMMAND_NAME,
    help = "Runs a recipe, recipe should be specified in the following format: <group>/<type> [<fromVersion> <toVersion>]"
) {

    private val recipeCanonicalName: String by argument(name = "recipe", completionCandidates = RECIPE_CANONICAL_NAME_COMPLETION)

    private val failOnError: Boolean by option(
        names = arrayOf("--fail-on-error"),
        help = "Fail the execution if an error occurs. Default behavior is to continue running visitors after an error in one.",
        completionCandidates = CompletionCandidates.None
    ).flag("--continue-on-error", default = false)

    private val fromVersion: Version? by argument(name = "from-version", completionCandidates = FROM_VERSION_COMPLETION)
        .convert { Version.parse(it, false) }
        .optional()

    private val toVersion: Version? by argument(name = "to-version", completionCandidates = TO_VERSION_COMPLETION)
        .convert { Version.parse(it, false) }
        .optional()

    override fun runSubCommand(): ExecutionResult {
        val (group, type) = decomposeCanonicalName(recipeCanonicalName)
        if (group.isNullOrEmpty() || type.isNullOrEmpty()) {
            throw PrintMessage(
                message = "Invalid group or type: $recipeCanonicalName. The expected syntax is <group>/<type>. $LIST_RECIPES_HINT",
                statusCode = 1
            )
        }

        val coordinates = RecipeCoordinates(group, type, fromVersion, toVersion)
        val matching = recipeMatcher.findMatching(coordinates).map { it.name }

        if (matching.isEmpty()) {
            throw PrintMessage(
                message = "No matching recipes found. $LIST_RECIPES_HINT",
                statusCode = 1
            )
        }

        val recipes = recipeInstantiator.instantiateAll(matching)

        recipes.forEach { recipe ->
            val inputFiles = inputFilesProvider.getInputFilesFor(recipe)
            recipeExecutor.execute(recipe, inputFiles, failOnError)
        }

        return ExecutionResult(recipes.map { recipe -> recipe.name })
    }

    private fun decomposeCanonicalName(canonicalName: String?): Pair<String?, String?> =
        Pair(
            canonicalName?.substringBeforeLast('/'),
            canonicalName?.substringAfterLast('/', missingDelimiterValue = "")
        )

    companion object {

        const val COMMAND_NAME: String = "run"
    }
}
