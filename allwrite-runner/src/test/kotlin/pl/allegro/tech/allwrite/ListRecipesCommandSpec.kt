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
                jackson/upgrade 2 3
                spring-boot/upgrade 2 3
                spring-boot/upgrade 3 4
                workflows/introduceSetupCi

            """.trimIndent()
        }

        test("should list all recipes including internal when --all flag is used") {
            val result = listRecipesCommand.test("--all")

            result.statusCode shouldBe 0
            result.output shouldBe """
                jackson/upgrade 2 3 -> pl.allegro.tech.allwrite.recipes.jackson
                spring-boot/upgrade 2 3 -> pl.allegro.tech.allwrite.recipes.spring-boot-3
                spring-boot/upgrade 3 4 -> pl.allegro.tech.allwrite.recipes.spring-boot-4
                workflows/introduceSetupCi -> pl.allegro.tech.allwrite.recipes.setup-ci
                pl.allegro.tech.allwrite.recipes.yaml.ExpandMappings
                
                org.openrewrite.java.format.AutoFormat
                
                tech.picnic.errorprone.SomeRule

            """.trimIndent()
        }

        test("should list all recipes including internal when -a flag is used") {
            val result = listRecipesCommand.test("-a")

            result.statusCode shouldBe 0
            result.output shouldBe """
                jackson/upgrade 2 3 -> pl.allegro.tech.allwrite.recipes.jackson
                spring-boot/upgrade 2 3 -> pl.allegro.tech.allwrite.recipes.spring-boot-3
                spring-boot/upgrade 3 4 -> pl.allegro.tech.allwrite.recipes.spring-boot-4
                workflows/introduceSetupCi -> pl.allegro.tech.allwrite.recipes.setup-ci
                pl.allegro.tech.allwrite.recipes.yaml.ExpandMappings
                
                org.openrewrite.java.format.AutoFormat
                
                tech.picnic.errorprone.SomeRule

            """.trimIndent()
        }
    }
}
