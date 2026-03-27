package pl.allegro.tech.allwrite.cli.application

import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.cli.application.port.outgoing.ExternalRecipeStore

@Single
internal class RemoveExternalRecipeCommand(
    private val externalRecipeStore: ExternalRecipeStore
) : ExternalSubCommand(name = COMMAND_NAME, help = "Removes an external recipe JAR") {

    private val name: String by argument(help = "Name of the external recipe source to remove")

    override fun runSubCommand(): ExecutionResult {
        try {
            externalRecipeStore.remove(name)
        } catch (e: IllegalArgumentException) {
            throw PrintMessage(e.message ?: "Failed to remove external recipe", statusCode = 1)
        }
        return ExecutionResult(emptyList())
    }

    companion object {
        const val COMMAND_NAME: String = "rm"
    }
}
