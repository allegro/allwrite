package pl.allegro.tech.allwrite.cli

import io.kotest.matchers.shouldBe
import pl.allegro.tech.allwrite.cli.base.BaseCliSpec
import pl.allegro.tech.allwrite.cli.infrastructure.os.SystemEnvContextProvider
import pl.allegro.tech.allwrite.cli.infrastructure.os.port.incoming.SystemEnvironment
import pl.allegro.tech.allwrite.runtime.util.declareFake
import pl.allegro.tech.allwrite.runtime.util.injectEagerly

class SystemEnvContextProviderSpec : BaseCliSpec() {

    private val systemContextProvider: SystemEnvContextProvider by injectEagerly()

    init {
        test("should extract from env and convert additional telemetry keys") {
            // given
            declareFake<SystemEnvironment>(FakeBlankSystemEnvironment())

            // when
            val context = systemContextProvider.extractFromSystemEnvs()

            // then
            context shouldBe mapOf(
                "firstVar" to "firstExampleValue",
                "secondVar" to "secondExampleValue",
            )
        }
    }

    private class FakeBlankSystemEnvironment : SystemEnvironment {

        override fun get(name: String): String = "SAMPLE_SYSTEM_ENVIRONMENT"
        override fun getAll(): Map<String, String> =
            mapOf(
                "SAMPLE_SYSTEM_ENVIRONMENT" to "someSystemValue",
                "TELEMETRY_ADDITIONAL_KEY_FIRST_VAR" to "firstExampleValue",
                "TELEMETRY_ADDITIONAL_KEY_SECOND_VAR" to "secondExampleValue",
            )
    }
}
