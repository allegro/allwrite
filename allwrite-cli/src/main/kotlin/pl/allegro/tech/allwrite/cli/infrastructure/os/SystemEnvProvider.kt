package pl.allegro.tech.allwrite.cli.infrastructure.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.port.outgoing.AdditionalContextProvider
import pl.allegro.tech.allwrite.cli.infrastructure.os.port.incoming.SystemEnvironment

@Single
internal class SystemEnvContextProvider(
    private val systemEnvironment: SystemEnvironment
) : AdditionalContextProvider {

    override fun extractFromSystemEnvs(): Map<String, String> =
        systemEnvironment.getAll()
            .filterKeys { it.startsWith(TELEMETRY_ADDITIONAL_KEY_PREFIX) }
            .mapKeys { (key, _) ->
                key.removePrefix(TELEMETRY_ADDITIONAL_KEY_PREFIX)
                    .let(::snakeToLowerCamelCase)
            }

    private fun snakeToLowerCamelCase(value: String): String =
        value
            .split('_')
            .filter { it.isNotEmpty() }
            .mapIndexed { index, part ->
                val lower = part.lowercase()
                if (index == 0) lower else lower.replaceFirstChar { it.uppercase() }
            }
            .joinToString("")

    private companion object {

        private const val TELEMETRY_ADDITIONAL_KEY_PREFIX = "TELEMETRY_ADDITIONAL_KEY_"
    }
}

