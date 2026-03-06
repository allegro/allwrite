package pl.allegro.tech.allwrite

import com.github.ajalt.clikt.testing.test
import io.kotest.matchers.shouldBe
import org.koin.ksp.generated.module
import org.koin.test.get
import pl.allegro.tech.allwrite.base.BaseRunnerSpec
import pl.allegro.tech.allwrite.common.fake.FakeRuntimeModule
import pl.allegro.tech.allwrite.common.util.injectEagerly
import pl.allegro.tech.allwrite.runner.application.ListRecipesCommand

class ListRecipesCommandSpec : BaseRunnerSpec() {

    private val listRecipesCommand: ListRecipesCommand by injectEagerly()

    override fun additionalModules() = listOf(
        FakeRuntimeModule().module
    )

    init {
        test("should list recipes") {
            val result = listRecipesCommand.test()

            result.statusCode shouldBe 0
            result.output shouldBe """
                jackson/upgrade 2.0.0 3.0.0
                spring-boot/upgrade 2.0.0 3.0.0
                spring-boot/upgrade 3.0.0 4.0.0
                workflows/introduceSetupCi

            """.trimIndent()
        }
    }
}
