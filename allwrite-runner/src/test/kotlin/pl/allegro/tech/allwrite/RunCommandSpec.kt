package pl.allegro.tech.allwrite

import com.github.ajalt.clikt.testing.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import org.koin.ksp.generated.module
import org.koin.test.get
import pl.allegro.tech.allwrite.base.BaseRunnerSpec
import pl.allegro.tech.allwrite.common.fake.FakeRuntimeModule
import pl.allegro.tech.allwrite.common.fake.FakeRecipeExecutor
import pl.allegro.tech.allwrite.common.fake.FakeRecipeSource.Companion.SPRING_BOOT_3_TEST_RECIPE
import pl.allegro.tech.allwrite.common.fake.FakeRecipeSource.Companion.SPRING_BOOT_4_TEST_RECIPE
import pl.allegro.tech.allwrite.common.fake.FakeRecipeSource.Companion.SETUP_CI_TEST_RECIPE
import pl.allegro.tech.allwrite.common.util.injectEagerly
import pl.allegro.tech.allwrite.runner.application.Messages.LIST_RECIPES_HINT
import pl.allegro.tech.allwrite.runner.application.RunCommand

class RunCommandSpec : BaseRunnerSpec() {

    private val runCommand: RunCommand by injectEagerly()
    private val fakeRecipeExecutor: FakeRecipeExecutor by injectEagerly()

    override fun additionalModules() = listOf(
        FakeRuntimeModule().module
    )

    init {
        test("should fail when no arguments provided") {
            val result = runCommand.test()

            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            Usage: run [<options>] <recipe> [<from-version>] [<to-version>]
            
            Error: missing argument <recipe>
            """.trimIndent()
        }

        test("should fail when incorrect group/type provided") {
            val result = runCommand.test("test")

            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            Invalid group or type: test. The expected syntax is <group>/<type>. $LIST_RECIPES_HINT
            """.trimIndent()
        }

        test("should proceed when version not specified and there is a single matching recipe") {
            val result = runCommand.test("workflows/introduceSetupCi")

            result.statusCode shouldBe 0
            fakeRecipeExecutor.executedRecipes shouldContainExactly listOf(
                SETUP_CI_TEST_RECIPE
            )
        }

        test("should run all matching recipes when version not specified and the name is ambiguous") {
            val result = runCommand.test("spring-boot/upgrade")

            result.statusCode shouldBe 0
            result.output.trim().shouldBeEmpty()

            fakeRecipeExecutor.executedRecipes shouldContainExactly listOf(
                SPRING_BOOT_3_TEST_RECIPE,
                SPRING_BOOT_4_TEST_RECIPE
            )
        }

        test("should do nothing when version not specified and there is no matching recipe") {
            val result = runCommand.test("no-such-group/no-such-type")

            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            No matching recipes found. $LIST_RECIPES_HINT
            """.trimIndent()

            fakeRecipeExecutor.executedRecipes.shouldBeEmpty()
        }

        test("should proceed when version is specified and there is a matching recipe") {
            val result = runCommand.test("spring-boot/upgrade", "2", "3")

            result.statusCode shouldBe 0

            fakeRecipeExecutor.executedRecipes shouldContainExactly listOf(
                SPRING_BOOT_3_TEST_RECIPE
            )
        }

        test("should fail when odd arguments are present") {
            val result = runCommand.test("spring-boot/upgrade", "2", "3", "a", "b", "c")

            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            Usage: run [<options>] <recipe> [<from-version>] [<to-version>]
            
            Error: got unexpected extra arguments (a b c)
            """.trimIndent()
        }

    }
}
