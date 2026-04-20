package pl.allegro.tech.allwrite.cli.application

import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.cli.application.port.outgoing.ExternalRecipeStore

@Single
internal class AddExternalRecipeCommand(
    private val externalRecipeStore: ExternalRecipeStore,
) : ExternalSubCommand(name = COMMAND_NAME, help = "Adds an external recipe JAR from a URL") {

    private val name: String by argument(help = "Name for the external recipe source")
    private val url: String by argument(help = "URL of the recipe JAR to fetch")

    override fun runSubCommand(): ExecutionResult {
        try {
            externalRecipeStore.add(name, url)
        } catch (e: IllegalArgumentException) {
            throw PrintMessage(e.message ?: "Failed to add external recipe", statusCode = 1)
        }
        return ExecutionResult(emptyList())
    }

    companion object {
        const val COMMAND_NAME: String = "add"
    }
}
