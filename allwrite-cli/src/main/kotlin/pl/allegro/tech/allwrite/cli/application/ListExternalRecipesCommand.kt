package pl.allegro.tech.allwrite.cli.application

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.cli.application.port.outgoing.ExternalRecipeStore

@Single
internal class ListExternalRecipesCommand(
    private val externalRecipeStore: ExternalRecipeStore,
) : ExternalSubCommand(name = COMMAND_NAME, help = "Lists all external recipe JARs") {

    override fun runSubCommand(): ExecutionResult {
        val recipes = externalRecipeStore.list()
        if (recipes.isEmpty()) {
            echo("No external recipes configured.")
        } else {
            recipes.forEach { (name, url) ->
                echo("$name -> $url")
            }
        }
        return ExecutionResult(emptyList())
    }

    companion object {
        const val COMMAND_NAME: String = "ls"
    }
}
