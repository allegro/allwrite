package pl.allegro.tech.allwrite.cli.application

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.koin.core.component.KoinComponent
import org.slf4j.LoggerFactory
import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.TimeMeasuredExecutionResult
import pl.allegro.tech.allwrite.cli.util.catchingMeasureTime
import pl.allegro.tech.allwrite.cli.util.injectAll

internal abstract class SubCommand(
    name: String,
    private val help: String,
) : CliktCommand(name = name),
    KoinComponent {

    private val commandListeners: List<CommandListener> by injectAll()

    protected val verbose by option("-v", "--verbose").flag(default = false)

    private val logLevel by option("--log-level")
        .convert { Level.toLevel(it) }
        .defaultLazy { if (verbose) Level.DEBUG else Level.INFO }

    override fun help(context: Context): String = help

    final override fun run() {
        setLogLevel()
        val result = catchingMeasureTime { runSubCommand() }
        notifyListeners(result)
        result.throwable?.let { throw it }
    }

    fun notifyListeners(commandExecutionResult: TimeMeasuredExecutionResult) {
        val event = CommandExecutedEvent(
            commandName,
            commandExecutionResult.executionTime,
            commandExecutionResult.throwable,
            commandExecutionResult.executedRecipes,
        )
        commandListeners.forEach { it.onCommandExecuted(event) }
    }

    protected abstract fun runSubCommand(): ExecutionResult

    private fun setLogLevel() {
        val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        logger.level = logLevel
    }
}
