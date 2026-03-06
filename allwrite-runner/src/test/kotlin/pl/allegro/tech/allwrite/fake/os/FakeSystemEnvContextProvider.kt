package pl.allegro.tech.allwrite.fake.os

import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.runner.application.port.outgoing.AdditionalContextProvider

@Single
class FakeSystemEnvContextProvider : AdditionalContextProvider {

    override fun extractFromSystemEnvs(): Map<String, String> =
        mapOf("firstTestVariable" to "firstTestValue", "secondTestVariable" to "secondTestValue")
}
