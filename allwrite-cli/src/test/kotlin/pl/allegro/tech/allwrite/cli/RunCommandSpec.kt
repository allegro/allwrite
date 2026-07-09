package pl.allegro.tech.allwrite.cli

import com.github.ajalt.clikt.testing.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.cli.application.Messages
import pl.allegro.tech.allwrite.cli.application.RunCommand
import pl.allegro.tech.allwrite.cli.base.BaseCliSpec
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipeExecutor
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipeSource
import pl.allegro.tech.allwrite.runtime.fake.FakeRuntimeModule
import pl.allegro.tech.allwrite.runtime.util.injectEagerly

class RunCommandSpec : BaseCliSpec() {

    private val runCommand: RunCommand by injectEagerly()
    private val fakeRecipeExecutor: FakeRecipeExecutor by injectEagerly()

    override fun additionalModules() =
        listOf(
            FakeRuntimeModule().module,
        )

    init {
        test("should fail when no arguments provided") {
            // when
            val result = runCommand.test()

            // then
            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            Must provide positional argument, --recipe, or --file option
            """.trimIndent()
        }

        test("should fail when incorrect group/type provided") {
            // when
            val result = runCommand.test("test")

            // then
            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            Invalid group or action: test. The expected syntax is <group>/<action>. ${Messages.LIST_RECIPES_HINT}
            """.trimIndent()
        }

        test("should proceed when version not specified and there is a single matching recipe") {
            // when
            val result = runCommand.test("workflows/introduceSetupCi")

            // then
            result.statusCode shouldBe 0
            fakeRecipeExecutor.executedRecipes shouldContainExactly listOf(
                FakeRecipeSource.Companion.SETUP_CI_TEST_RECIPE,
            )
        }

        test("should run all matching recipes when version not specified and the name is ambiguous") {
            // when
            val result = runCommand.test("spring-boot/upgrade")

            // then
            result.statusCode shouldBe 0
            result.output.trim().shouldBeEmpty()

            fakeRecipeExecutor.executedRecipes shouldContainExactly listOf(
                FakeRecipeSource.Companion.SPRING_BOOT_3_TEST_RECIPE,
                FakeRecipeSource.Companion.SPRING_BOOT_4_TEST_RECIPE,
            )
        }

        test("should do nothing when version not specified and there is no matching recipe") {
            // when
            val result = runCommand.test("no-such-group/no-such-type")

            // then
            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            No matching recipes found. ${Messages.LIST_RECIPES_HINT}
            """.trimIndent()

            fakeRecipeExecutor.executedRecipes.shouldBeEmpty()
        }

        test("should proceed when version is specified and there is a matching recipe") {
            // when
            val result = runCommand.test("spring-boot/upgrade", "2", "3")

            // then
            result.statusCode shouldBe 0

            fakeRecipeExecutor.executedRecipes shouldContainExactly listOf(
                FakeRecipeSource.Companion.SPRING_BOOT_3_TEST_RECIPE,
            )
        }

        test("should run chain of recipes when only target version is provided") {
            // when
            val result = runCommand.test("spring-boot/upgrade 4")

            // then
            result.statusCode shouldBe 0

            fakeRecipeExecutor.executedRecipes shouldContainExactly listOf(
                FakeRecipeSource.Companion.SPRING_BOOT_3_TEST_RECIPE,
                FakeRecipeSource.Companion.SPRING_BOOT_4_TEST_RECIPE,
            )
        }

        test("should fail when only target version is provided and no matching recipes found") {
            // when
            val result = runCommand.test("spring-boot/upgrade 999")

            // then
            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            No matching recipes found. ${Messages.LIST_RECIPES_HINT}
            """.trimIndent()

            fakeRecipeExecutor.executedRecipes.shouldBeEmpty()
        }

        test("should fail when odd arguments are present") {
            // when
            val result = runCommand.test("spring-boot/upgrade", "2", "3", "a", "b", "c")

            // then
            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            Usage: run [<options>] [<recipe>] [<from-version>] [<to-version>]

            Error: got unexpected extra arguments (a b c)
            """.trimIndent()
        }

        test("should fail when non-existing recipe provided") {
            // when
            val result = runCommand.test("--recipe no.such.recipe")

            // then
            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            Recipe 'no.such.recipe' not found. ${Messages.LIST_RECIPES_HINT}
            """.trimIndent()
        }

        test("should fail when non-existing file provided") {
            // when
            val result = runCommand.test("--file no.such.file")

            // then
            result.statusCode shouldBe 1
            result.output.trim() shouldContain "Specified file not found"
        }

        test("should run recipe when correct recipe provided") {
            // when
            val result = runCommand.test("--recipe pl.allegro.tech.allwrite.recipes.spring-boot-3")

            // then
            result.statusCode shouldBe 0

            fakeRecipeExecutor.executedRecipes shouldContainExactly listOf(
                FakeRecipeSource.Companion.SPRING_BOOT_3_TEST_RECIPE,
            )
        }

        test("should run recipe when correct file provided") {
            // when
            val result = runCommand.test("--file src/test/resources/recipes.json")

            // then
            result.statusCode shouldBe 0

            fakeRecipeExecutor.executedRecipes shouldContainExactly listOf(
                FakeRecipeSource.Companion.SPRING_BOOT_3_TEST_RECIPE,
                FakeRecipeSource.Companion.SPRING_BOOT_4_TEST_RECIPE,
            )
        }
    }
}
