package pl.allegro.tech.allwrite.cli

import com.github.ajalt.clikt.testing.test
import io.kotest.matchers.shouldBe
import pl.allegro.tech.allwrite.cli.application.ListRecipesCommand
import pl.allegro.tech.allwrite.cli.base.BaseCliSpec
import pl.allegro.tech.allwrite.runtime.util.injectEagerly

class ListRecipesCommandSpec : BaseCliSpec() {

    private val listRecipesCommand: ListRecipesCommand by injectEagerly()

    override fun additionalModules() =
        listOf(
            TestModules.fakeRuntime,
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

        test("should list all recipes including internal when --all flag is used") {
            // when
            val result = listRecipesCommand.test("--all")

            // then
            result.statusCode shouldBe 0
            result.output shouldBe """
                external-jackson/upgrade 2 3 -> pl.allegro.tech.allwrite.recipes.dependabot-jackson
                external-spring-boot/upgrade 2 3 -> pl.allegro.tech.allwrite.recipes.dependabot-spring-boot-3
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
            // when
            val result = listRecipesCommand.test("-a")

            // then
            result.statusCode shouldBe 0
            result.output shouldBe """
                external-jackson/upgrade 2 3 -> pl.allegro.tech.allwrite.recipes.dependabot-jackson
                external-spring-boot/upgrade 2 3 -> pl.allegro.tech.allwrite.recipes.dependabot-spring-boot-3
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
