package pl.allegro.tech.allwrite.cli.application

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import pl.allegro.tech.allwrite.cli.util.injectAll

@Single
internal class ExternalCommand : CliktCommand(name = COMMAND_NAME), KoinComponent {

    private val externalSubCommands: List<ExternalSubCommand> by injectAll()

    init {
        subcommands(externalSubCommands)
    }

    override fun help(context: Context): String = "Manage external recipe JARs"

    override fun run() {}

    companion object {
        const val COMMAND_NAME = "external"
    }
}
