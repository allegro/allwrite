package pl.allegro.tech.allwrite.cli.application

import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.koalaql.markout.md.markdown
import org.koin.core.annotation.Single
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.RecipeVisibility.PUBLIC
import pl.allegro.tech.allwrite.api.RecipeSource
import pl.allegro.tech.allwrite.api.tagPropertyOrNull
import pl.allegro.tech.allwrite.api.toCompactString
import pl.allegro.tech.allwrite.api.toRecipeCoordinatesOrNull
import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.ExecutionResult
import com.github.ajalt.mordant.markdown.Markdown as MdWidget

@Single
internal class ListRecipesCommand(
    private val recipeSource: RecipeSource,
) : SubCommand(name = COMMAND_NAME, help = "Lists all recipes") {

    private val all by option("-a", "--all", help = "Show all recipes including internal ones").flag(default = false)

    override fun runSubCommand(): ExecutionResult {
        val recipes = recipeSource.findAll(includeInternal = all).sortedBy { it.name }

        if (!verbose) {
            echo(if (all) renderAllRecipesListing(recipes) else renderRecipesListing(recipes))
        } else {
            val markdown = markdown(recipes)
            terminal.println(MdWidget(markdown))
        }

        return ExecutionResult(emptyList())
    }

    private fun markdown(recipes: List<RecipeDescriptor>) =
        markdown {
            recipes.map { recipe ->
                h3(recipe.displayName)
                ul {
                    li {
                        p("ID: ${recipe.name}")
                    }
                    li {
                        p("Description: ${recipe.description}")
                    }
                    li {
                        p("Execution: `${MainCommand.COMMAND_NAME} ${RunCommand.COMMAND_NAME} ${recipe.toRecipeCoordinatesOrNull()}`")
                    }
                }
                hr()
            }
        }

    private fun renderRecipesListing(recipes: List<RecipeDescriptor>): String =
        recipes
            .mapNotNull { it.toRecipeCoordinatesOrNull() }
            .sortedBy { it.group }
            .joinToString(separator = "\n") {
                buildString {
                    append("${it.group}/${it.action}")
                    it.fromVersion?.let { append(" ${it.toCompactString()}") }
                    it.toVersion?.let { append(" ${it.toCompactString()}") }
                }
            }

    private fun renderAllRecipesListing(recipes: List<RecipeDescriptor>): String {
        val (allwriteRecipes, otherRecipes) = recipes.partition {
            it.name.startsWith(ALLWRITE_RECIPES_PREFIX)
        }

        val allwriteGroup = renderAllwriteGroup(allwriteRecipes)

        val otherGroups = otherRecipes
            .groupBy { it.name.basePackage() }
            .toSortedMap()
            .map { (_, groupRecipes) ->
                groupRecipes.sortedBy { it.name }.joinToString("\n") { it.name }
            }

        return (listOf(allwriteGroup) + otherGroups)
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n\n")
    }

    private fun renderAllwriteGroup(recipes: List<RecipeDescriptor>): String {
        val (publicRecipes, internalRecipes) = recipes.partition {
            PUBLIC.name.equals(it.tagPropertyOrNull("visibility"), ignoreCase = true)
        }

        val publicLines = publicRecipes
            .mapNotNull { descriptor ->
                descriptor.toRecipeCoordinatesOrNull()?.let { it to descriptor }
            }
            .sortedBy { it.first.group }
            .map { (coords, descriptor) ->
                buildString {
                    append("${coords.group}/${coords.action}")
                    coords.fromVersion?.let { append(" ${it.toCompactString()}") }
                    coords.toVersion?.let { append(" ${it.toCompactString()}") }
                    append(" -> ${descriptor.name}")
                }
            }

        val internalLines = internalRecipes
            .sortedBy { it.name }
            .map { it.name }

        return (publicLines + internalLines).joinToString("\n")
    }

    private fun String.basePackage(): String = split(".").take(2).joinToString(".")

    companion object {

        const val COMMAND_NAME: String = "ls"
        private const val ALLWRITE_RECIPES_PREFIX = "pl.allegro.tech.allwrite.recipes"
    }
}
