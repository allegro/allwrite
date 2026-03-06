package pl.allegro.tech.allwrite

import com.github.ajalt.clikt.testing.test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import org.koin.ksp.generated.module
import org.koin.test.get
import pl.allegro.tech.allwrite.base.BaseRunnerSpec
import pl.allegro.tech.allwrite.common.fake.FakeRecipeExecutor
import pl.allegro.tech.allwrite.common.fake.FakeRecipeSource.Companion.SPRING_BOOT_3_TEST_RECIPE
import pl.allegro.tech.allwrite.common.fake.FakeRecipeSource.Companion.SPRING_BOOT_4_TEST_RECIPE
import pl.allegro.tech.allwrite.common.fake.FakeRecipeSource.Companion.JACKSON_TEST_RECIPE
import pl.allegro.tech.allwrite.common.fake.FakeRuntimeModule
import pl.allegro.tech.allwrite.common.util.injectEagerly
import pl.allegro.tech.allwrite.fake.github.FakeGithubModule
import pl.allegro.tech.allwrite.fake.github.FakePullRequestContext
import pl.allegro.tech.allwrite.fake.github.FakePullRequestContext.Companion.ORIGINAL_DESCRIPTION
import pl.allegro.tech.allwrite.runner.application.RunWithDependabotCommand
import pl.allegro.tech.allwrite.runner.application.RunWithDependabotCommand.Companion.ENV_VAR_RUN_DEPENDABOT_PAYLOAD_NAME
import pl.allegro.tech.allwrite.runner.util.JSON

class RunWithDependabotCommandSpec : BaseRunnerSpec() {

    private val runWithDependabotCommand: RunWithDependabotCommand by injectEagerly()
    private val fakeRecipeExecutor: FakeRecipeExecutor by injectEagerly()
    private val fakePullRequestContext: FakePullRequestContext by injectEagerly()

    override fun additionalModules() = listOf(
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
                    envvars = mapOf("PR_MANAGER_EXTRA_PARAMS" to "test")
                )
            }
        }

        test("should finish successfully when dependabot metadata is not mapped to any recipe") {
            val result = runWithDependabotCommand.test(
                envvars = mapOf(
                    ENV_VAR_RUN_DEPENDABOT_PAYLOAD_NAME to """
                        {
                          "dependabot": [
                             {
                               "artifact":"com.example:some-library",
                               "from":{"normalVersion": "1.5.1", "major": "1"},
                               "to": {"normalVersion": "2.0.0", "major": "2"}
                             }
                          ]
                        }
                        """.trimIndent()
                )
            )

            result.statusCode shouldBe 0
            result.output.trim() shouldBeEqual "No matching recipes found."
            fakeRecipeExecutor.executedRecipes.shouldBeEmpty()
        }
    }
}
