package pl.allegro.tech.allwrite.cli.fake.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.cli.application.port.outgoing.AdditionalContextProvider

@Single
class FakeSystemEnvContextProvider : AdditionalContextProvider {

    override fun extractFromSystemEnvs(): Map<String, String> = mapOf("firstTestVariable" to "firstTestValue", "secondTestVariable" to "secondTestValue")
}
