package pl.allegro.tech.allwrite.cli.application

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import pl.allegro.tech.allwrite.cli.application.port.incoming.AppEntrypoint
import pl.allegro.tech.allwrite.cli.application.port.outgoing.ShutdownListener
import pl.allegro.tech.allwrite.cli.util.injectAll

@Single
internal class MainCommand : CliktCommand(name = COMMAND_NAME), AppEntrypoint, KoinComponent {

    private val subCommands: List<SubCommand> by injectAll()
    private val shutdownListeners: List<ShutdownListener> by injectAll()

    init {
        val topLevelSubCommands = subCommands.filterNot { it is ExternalSubCommand }
        val externalCommand: ExternalCommand = getKoin().get()
        subcommands(topLevelSubCommands + externalCommand)
    }

    override fun execute(args: Array<String>) {
        main(args)
        shutdownListeners.forEach { it.onAppShutdown() }
    }

    override fun run() {
    }

    companion object {
        const val COMMAND_NAME = "allwrite"
    }
}
