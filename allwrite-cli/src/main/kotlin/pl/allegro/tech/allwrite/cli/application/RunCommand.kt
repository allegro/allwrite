package pl.allegro.tech.allwrite.cli.application

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.zafarkhaja.semver.Version
import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.api.RecipeCoordinates
import pl.allegro.tech.allwrite.api.RecipeExecutor
import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.cli.application.Messages.LIST_RECIPES_HINT
import pl.allegro.tech.allwrite.cli.application.port.outgoing.InputFilesProvider
import pl.allegro.tech.allwrite.cli.recipeCanonicalNameOptions
import pl.allegro.tech.allwrite.cli.recipeFromVersionCompletion
import pl.allegro.tech.allwrite.cli.recipeIdOptions
import pl.allegro.tech.allwrite.cli.recipeToVersionCompletion
import pl.allegro.tech.allwrite.cli.util.customCompletion
import java.io.File

private val RECIPE_CANONICAL_NAME_COMPLETION: CompletionCandidates = CompletionCandidates.Fixed(recipeCanonicalNameOptions)
private val FROM_VERSION_COMPLETION: CompletionCandidates = customCompletion(recipeFromVersionCompletion)
private val TO_VERSION_COMPLETION: CompletionCandidates = customCompletion(recipeToVersionCompletion)
private val RECIPE_ID_COMPLETION: CompletionCandidates = CompletionCandidates.Fixed(recipeIdOptions.toSet())

@Single
internal class RunCommand(
    private val recipeMatcher: RecipeMatcher,
    private val recipeInstantiator: RecipeInstantiator,
    private val inputFilesProvider: InputFilesProvider,
    private val recipeExecutor: RecipeExecutor,
) : SubCommand(
    name = COMMAND_NAME,
    help = "Runs a recipe. Supports positional arguments <group>/<action> [<from> <to>], --recipe <id>, or --file <path>",
) {

    private val recipeFriendlyName: String? by argument(
        name = "recipe",
        completionCandidates = RECIPE_CANONICAL_NAME_COMPLETION,
    ).optional()

    private val recipeId: String? by option(
        names = arrayOf("--recipe"),
        completionCandidates = RECIPE_ID_COMPLETION,
    )

    private val file: File? by option(
        names = arrayOf("--file"),
        help = "Path to a json file with list of recipes. JSON object should contain a single " +
            "array-of-strings field called `recipes`",
        completionCandidates = CompletionCandidates.Path,
    ).convert { File(it) }

    private val failOnError: Boolean by option(
        names = arrayOf("--fail-on-error"),
        help = "Fail the execution if an error occurs. Default behavior is to continue running visitors after an error in one.",
        completionCandidates = CompletionCandidates.None,
    ).flag("--continue-on-error", default = false)

    private val fromVersion: Version? by argument(name = "from-version", completionCandidates = FROM_VERSION_COMPLETION)
        .convert { Version.parse(it, false) }
        .optional()

    private val toVersion: Version? by argument(name = "to-version", completionCandidates = TO_VERSION_COMPLETION)
        .convert { Version.parse(it, false) }
        .optional()

    override fun runSubCommand(): ExecutionResult {
        if (listOfNotNull(recipeFriendlyName, recipeId, file).count() > 1) {
            throw PrintMessage("Cannot combine positional argument with --recipe and --file options. Specify exactly one of them.", statusCode = 1)
        }

        return when {
            recipeFriendlyName != null -> runByFriendlyName(recipeFriendlyName!!)
            recipeId != null -> runById(recipeId!!)
            file != null -> runFromFile(file!!)
            else -> throw PrintMessage("Must provide positional argument, --recipe, or --file option", statusCode = 1)
        }
    }

    private fun runByFriendlyName(canonicalName: String): ExecutionResult {
        val (group, action) = decomposeCanonicalName(canonicalName)
        if (group.isNullOrEmpty() || action.isNullOrEmpty()) {
            throw PrintMessage("Invalid group or action: $canonicalName. The expected syntax is <group>/<action>. $LIST_RECIPES_HINT", statusCode = 1)
        }

        val matching = if (fromVersion != null && toVersion == null) {
            recipeMatcher.findMatchingByTargetVersion(group, action, fromVersion!!).map { it.name }
        } else {
            val coordinates = RecipeCoordinates(group, action, fromVersion, toVersion)
            recipeMatcher.findMatching(coordinates).map { it.name }
        }

        if (matching.isEmpty()) {
            throw PrintMessage(
                message = "No matching recipes found. $LIST_RECIPES_HINT",
                statusCode = 1,
            )
        }
        val recipes = recipeInstantiator.instantiateAll(matching)
        return runRecipes(recipes)
    }

    private fun runById(recipeId: String): ExecutionResult {
        val recipe = recipeInstantiator.instantiate(recipeId)
        return runRecipes(listOf(recipe))
    }

    private fun runFromFile(file: File): ExecutionResult {
        val recipes = recipeInstantiator.instantiateFrom(file)
        return runRecipes(recipes)
    }

    private fun runRecipes(recipes: List<Recipe>): ExecutionResult {
        recipes.forEach { recipe ->
            val inputFiles = inputFilesProvider.getInputFilesFor(recipe)
            recipeExecutor.execute(recipe, inputFiles, failOnError)
        }
        return ExecutionResult(recipes.map { recipe -> recipe.name })
    }

    private fun decomposeCanonicalName(canonicalName: String?): Pair<String?, String?> =
        Pair(
            canonicalName?.substringBeforeLast('/'),
            canonicalName?.substringAfterLast('/', missingDelimiterValue = ""),
        )

    companion object {

        const val COMMAND_NAME: String = "run"
    }
}
