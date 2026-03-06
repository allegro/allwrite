package pl.allegro.tech.allwrite

import com.github.ajalt.clikt.testing.test
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.koin.ksp.generated.module
import org.koin.test.get
import pl.allegro.tech.allwrite.base.BaseRunnerSpec
import pl.allegro.tech.allwrite.common.fake.FakeRuntimeModule
import pl.allegro.tech.allwrite.common.fake.FakeRecipeExecutor
import pl.allegro.tech.allwrite.common.fake.FakeRecipeSource.Companion.SPRING_BOOT_3_TEST_RECIPE
import pl.allegro.tech.allwrite.common.fake.FakeRecipeSource.Companion.SPRING_BOOT_4_TEST_RECIPE
import pl.allegro.tech.allwrite.common.util.injectEagerly
import pl.allegro.tech.allwrite.runner.application.Messages.LIST_RECIPES_HINT
import pl.allegro.tech.allwrite.runner.application.RunRecipeCommand

class RunRecipeCommandSpec : BaseRunnerSpec() {

    private val runRecipeCommand: RunRecipeCommand by injectEagerly()
    private val fakeRecipeExecutor: FakeRecipeExecutor by injectEagerly()

    override fun additionalModules() = listOf(
        FakeRuntimeModule().module
    )

    init {
         test("should fail when no recipe provided") {
            val result = runRecipeCommand.test()

            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            Usage: run-recipe [<options>]
            
            Error: must provide one of --recipe, --file
            """.trimIndent()
        }

        test("should fail when non-existing recipe provided") {
            val result = runRecipeCommand.test("--recipe no.such.recipe")

            result.statusCode shouldBe 1
            result.output.trim() shouldBeEqual """
            Recipe 'no.such.recipe' not found. $LIST_RECIPES_HINT
            """.trimIndent()
        }

        test("should fail when non-existing file provided") {
            val result = runRecipeCommand.test("--file no.such.file")

            result.statusCode shouldBe 1
            result.output.trim() shouldContain "Specified file not found"
        }

        test("should run recipe when correct recipe provided") {
            val result = runRecipeCommand.test("--recipe pl.allegro.tech.allwrite.recipes.spring-boot-3")

            result.statusCode shouldBe 0

            fakeRecipeExecutor.executedRecipes shouldContainExactly listOf(
                SPRING_BOOT_3_TEST_RECIPE
            )
        }

        test("should run recipe when correct file provided") {
            val result = runRecipeCommand.test("--file src/test/resources/recipes.json")

            result.statusCode shouldBe 0

            fakeRecipeExecutor.executedRecipes shouldContainExactly listOf(
                SPRING_BOOT_3_TEST_RECIPE,
                SPRING_BOOT_4_TEST_RECIPE
            )
        }
    }
}
