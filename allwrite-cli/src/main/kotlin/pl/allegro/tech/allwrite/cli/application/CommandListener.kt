package pl.allegro.tech.allwrite.cli.application

import kotlin.time.Duration

internal interface CommandListener {
    fun onCommandExecuted(event: CommandExecutedEvent)
}

internal data class CommandExecutedEvent(
    val command: String,
    val executionTime: Duration,
    val throwable: Throwable?,
    val recipes: List<String> = emptyList(),
)
