package pl.allegro.tech.allwrite.runner.infrastructure.os

import io.kotest.matchers.shouldBe
import org.koin.test.get
import pl.allegro.tech.allwrite.base.BaseRunnerSpec
import pl.allegro.tech.allwrite.common.util.declareFake
import pl.allegro.tech.allwrite.common.util.injectEagerly
import pl.allegro.tech.allwrite.runner.infrastructure.os.port.incoming.SystemEnvironment

class SystemEnvContextProviderSpec : BaseRunnerSpec() {

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
                "secondVar" to "secondExampleValue"
            )
        }

    }

    private class FakeBlankSystemEnvironment() : SystemEnvironment {

        override fun get(name: String): String = "SAMPLE_SYSTEM_ENVIRONMENT"
        override fun getAll(): Map<String, String> =
            mapOf(
                "SAMPLE_SYSTEM_ENVIRONMENT" to "someSystemValue",
                "TELEMETRY_ADDITIONAL_KEY_FIRST_VAR" to "firstExampleValue",
                "TELEMETRY_ADDITIONAL_KEY_SECOND_VAR" to "secondExampleValue"
            )
    }
}
