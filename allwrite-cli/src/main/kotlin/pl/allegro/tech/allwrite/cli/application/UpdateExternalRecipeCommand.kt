package pl.allegro.tech.allwrite.cli.application

import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.cli.application.port.outgoing.ExternalRecipeStore

@Single
internal class UpdateExternalRecipeCommand(
    private val externalRecipeStore: ExternalRecipeStore
) : ExternalSubCommand(name = COMMAND_NAME, help = "Updates an external recipe JAR. Re-fetches from a new URL, or from the stored URL if omitted") {

    private val name: String by argument(help = "Name of the external recipe source to update")
    private val url: String? by argument(help = "New URL of the recipe JAR to fetch").optional()

    override fun runSubCommand(): ExecutionResult {
        try {
            val url = url
            if (url != null) {
                externalRecipeStore.update(name, url)
            } else {
                externalRecipeStore.refresh(name)
            }
        } catch (e: IllegalArgumentException) {
            throw PrintMessage(e.message ?: "Failed to update external recipe", statusCode = 1)
        }
        return ExecutionResult(emptyList())
    }

    companion object {
        const val COMMAND_NAME: String = "update"
    }
}
