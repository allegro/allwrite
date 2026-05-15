package pl.allegro.tech.allwrite.cli

import com.github.ajalt.clikt.testing.test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.cli.application.RunWithDependabotCommand
import pl.allegro.tech.allwrite.cli.base.BaseCliSpec
import pl.allegro.tech.allwrite.cli.fake.github.FakeGithubModule
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipeExecutor
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipeSource
import pl.allegro.tech.allwrite.runtime.fake.FakeRuntimeModule
import pl.allegro.tech.allwrite.runtime.util.injectEagerly

class RunWithDependabotCommandSpec : BaseCliSpec() {

    private val runWithDependabotCommand: RunWithDependabotCommand by injectEagerly()
    private val fakeRecipeExecutor: FakeRecipeExecutor by injectEagerly()

    override fun additionalModules() =
        listOf(
            FakeRuntimeModule().module,
            FakeGithubModule().module,
        )

    init {
        test("should fail when no arguments provided") {
            val result = runWithDependabotCommand.test()

            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            Usage: run-dependabot [<options>]

            Error: missing option --prm-extra
            """.trimIndent()
        }

        test("should fail when incorrect dependabot payload provided") {
            shouldThrow<Exception> {
                runWithDependabotCommand.test(
                    envvars = mapOf("GH_BOT_EXTRA_PARAMS" to "test"),
                )
            }
        }

        test("should finish successfully when dependabot metadata is not mapped to any recipe") {
            val result = runWithDependabotCommand.test(
                envvars = mapOf(
                    RunWithDependabotCommand.ENV_VAR_RUN_DEPENDABOT_PAYLOAD_NAME to """
                        {
                          "dependabot": [
                             {
                               "artifact":"com.example:some-library",
                               "from":{"normalVersion": "1.5.1", "major": "1"},
                               "to": {"normalVersion": "2.0.0", "major": "2"}
                             }
                          ]
                        }
                    """.trimIndent(),
                ),
            )

            result.statusCode shouldBe 0
            result.output.trim() shouldBeEqual "No matching recipes found."
            fakeRecipeExecutor.executedRecipes.shouldBeEmpty()
        }

        test("should match recipe when dependabot artifact tag is present") {
            val result = runWithDependabotCommand.test(
                envvars = mapOf(
                    RunWithDependabotCommand.ENV_VAR_RUN_DEPENDABOT_PAYLOAD_NAME to """
                        {
                          "dependabot": [
                             {
                               "artifact":"org.springframework.boot:spring-boot-starter",
                               "from":{"normalVersion": "2.7.0", "major": "2"},
                               "to": {"normalVersion": "3.0.0", "major": "3"}
                             }
                          ]
                        }
                    """.trimIndent(),
                ),
            )

            result.statusCode shouldBe 0
            fakeRecipeExecutor.executedRecipes.map { it.name } shouldContainExactlyInAnyOrder listOf(
                FakeRecipeSource.DEPENDABOT_SPRING_BOOT_3_TEST_RECIPE.name,
            )
        }

        test("should not match recipe when dependabot artifact tag does not match") {
            val result = runWithDependabotCommand.test(
                envvars = mapOf(
                    RunWithDependabotCommand.ENV_VAR_RUN_DEPENDABOT_PAYLOAD_NAME to """
                        {
                          "dependabot": [
                             {
                               "artifact":"com.example:unrelated",
                               "from":{"normalVersion": "2.0.0", "major": "2"},
                               "to": {"normalVersion": "3.0.0", "major": "3"}
                             }
                          ]
                        }
                    """.trimIndent(),
                ),
            )

            result.statusCode shouldBe 0
            result.output.trim() shouldBeEqual "No matching recipes found."
            fakeRecipeExecutor.executedRecipes.shouldBeEmpty()
        }

        test("should match multiple recipes for different artifacts in same payload") {
            val result = runWithDependabotCommand.test(
                envvars = mapOf(
                    RunWithDependabotCommand.ENV_VAR_RUN_DEPENDABOT_PAYLOAD_NAME to """
                        {
                          "dependabot": [
                             {
                               "artifact":"org.springframework.boot:spring-boot-starter",
                               "from":{"normalVersion": "2.7.0", "major": "2"},
                               "to": {"normalVersion": "3.0.0", "major": "3"}
                             },
                             {
                               "artifact":"com.fasterxml.jackson.core:jackson-databind",
                               "from":{"normalVersion": "2.13.0", "major": "2"},
                               "to": {"normalVersion": "3.0.0", "major": "3"}
                             }
                          ]
                        }
                    """.trimIndent(),
                ),
            )

            result.statusCode shouldBe 0
            fakeRecipeExecutor.executedRecipes.map { it.name } shouldContainExactlyInAnyOrder listOf(
                FakeRecipeSource.DEPENDABOT_SPRING_BOOT_3_TEST_RECIPE.name,
                FakeRecipeSource.DEPENDABOT_JACKSON_TEST_RECIPE.name,
            )
        }
    }
}
