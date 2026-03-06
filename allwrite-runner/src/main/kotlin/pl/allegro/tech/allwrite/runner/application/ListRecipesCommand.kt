package pl.allegro.tech.allwrite.runner.application

import com.github.ajalt.clikt.core.terminal
import io.koalaql.markout.md.markdown
import org.koin.core.annotation.Single
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.common.port.incoming.RecipeSource
import pl.allegro.tech.allwrite.common.port.incoming.toRecipeCoordinatesOrNull
import pl.allegro.tech.allwrite.runner.application.CommandExecutionResult.ExecutionResult
import com.github.ajalt.mordant.markdown.Markdown as MdWidget

@Single
internal class ListRecipesCommand(
    private val recipeSource: RecipeSource
) : SubCommand(name = COMMAND_NAME, help = "Lists all recipes") {

    override fun runSubCommand(): ExecutionResult {
        val recipes = recipeSource.findAll().sortedBy { it.name }

        if (!verbose) {
            echo(renderRecipesListing(recipes))
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
                    append("${it.group}/${it.recipe}")
                    it.fromVersion?.let { append(" $it") }
                    it.toVersion?.let { append(" $it") }
                }
            }

    companion object {

        const val COMMAND_NAME: String = "ls"
    }
}
