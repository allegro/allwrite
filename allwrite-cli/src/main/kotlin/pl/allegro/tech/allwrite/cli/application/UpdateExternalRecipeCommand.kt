package pl.allegro.tech.allwrite.cli.application

import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.cli.infrastructure.os.ExternalRecipeStore

@Single
internal class UpdateExternalRecipeCommand(
    private val externalRecipeStore: ExternalRecipeStore
) : SubCommand(name = COMMAND_NAME, help = "Updates the URL of an external recipe JAR and re-downloads it") {

    private val name: String by argument(help = "Name of the external recipe source to update")
    private val url: String by argument(help = "New URL of the recipe JAR to download")

    override fun runSubCommand(): ExecutionResult {
        try {
            externalRecipeStore.update(name, url)
        } catch (e: IllegalArgumentException) {
            throw PrintMessage(e.message ?: "Failed to update external recipe", statusCode = 1)
        }
        echo("Updated external recipe '$name' to $url")
        return ExecutionResult(emptyList())
    }

    companion object {
        const val COMMAND_NAME: String = "update"
    }
}
