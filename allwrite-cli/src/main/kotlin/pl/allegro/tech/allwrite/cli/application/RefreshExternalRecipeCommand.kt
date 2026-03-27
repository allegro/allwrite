package pl.allegro.tech.allwrite.cli.application

import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.cli.infrastructure.os.ExternalRecipeStore

@Single
internal class RefreshExternalRecipeCommand(
    private val externalRecipeStore: ExternalRecipeStore
) : SubCommand(name = COMMAND_NAME, help = "Re-downloads an external recipe JAR from its stored URL") {

    private val name: String by argument(help = "Name of the external recipe source to refresh")

    override fun runSubCommand(): ExecutionResult {
        try {
            externalRecipeStore.refresh(name)
        } catch (e: IllegalArgumentException) {
            throw PrintMessage(e.message ?: "Failed to refresh external recipe", statusCode = 1)
        }
        echo("Refreshed external recipe '$name'")
        return ExecutionResult(emptyList())
    }

    companion object {
        const val COMMAND_NAME: String = "refresh"
    }
}
