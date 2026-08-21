package pl.allegro.tech.allwrite.cli

import com.github.ajalt.clikt.testing.test
import io.kotest.matchers.shouldBe
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.cli.application.ListRecipesCommand
import pl.allegro.tech.allwrite.cli.base.BaseCliSpec
import pl.allegro.tech.allwrite.runtime.fake.FakeRuntimeModule
import pl.allegro.tech.allwrite.runtime.util.injectEagerly

class ListRecipesCommandSpec : BaseCliSpec() {

    private val listRecipesCommand: ListRecipesCommand by injectEagerly()

    override fun additionalModules() =
        listOf(
            FakeRuntimeModule().module,
        )

    init {
        test("should list recipes") {
            // when
            val result = listRecipesCommand.test()

            // then
            result.statusCode shouldBe 0
            result.output shouldBe """
                external-jackson/upgrade 2 3
                external-spring-boot/upgrade 2 3
                jackson/upgrade 2 3
                spring-boot/upgrade 2 3
                spring-boot/upgrade 3 4
                workflows/introduceSetupCi

            """.trimIndent()
        }
    }
}
