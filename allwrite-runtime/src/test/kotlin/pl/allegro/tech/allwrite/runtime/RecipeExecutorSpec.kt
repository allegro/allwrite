package pl.allegro.tech.allwrite.runtime

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.data.Row1
import io.kotest.data.forAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import pl.allegro.tech.allwrite.spi.PostprocessingResult.Failure
import pl.allegro.tech.allwrite.runtime.base.BaseRuntimeSpec
import pl.allegro.tech.allwrite.runtime.fake.FakeCompositeRecipe
import pl.allegro.tech.allwrite.runtime.fake.FakePostProcessingRecipe
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipe
import pl.allegro.tech.allwrite.runtime.fake.FakeThrowingRecipe
import pl.allegro.tech.allwrite.runtime.port.outgoing.fake.FakeUserProblemReporter
import pl.allegro.tech.allwrite.api.RecipeExecutor
import pl.allegro.tech.allwrite.runtime.port.outgoing.Problem
import pl.allegro.tech.allwrite.runtime.util.injectEagerly
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.walk

class RecipeExecutorSpec : BaseRuntimeSpec() {

    private val recipeExecutor: RecipeExecutor by injectEagerly()
    private val fakeProblemReporter: FakeUserProblemReporter by injectEagerly()

    init {
        test("should execute recipe") {
            forAll(Row1(true), Row1(false)) { failOnError ->
                // given
                val recipe = FakeRecipe()

                // when
                recipeExecutor.execute(recipe, inputFiles(), failOnError)

                // then
                recipe.visitedSourceFiles.map { it.sourcePath } shouldContainExactlyInAnyOrder inputFiles()
            }
        }

        test("should rethrow exception thrown by executed recipe when configured to do so") {
            // given
            val recipe = FakeThrowingRecipe()

            // expect
            shouldThrowAny {
                recipeExecutor.execute(recipe, inputFiles(), true)
            }
        }

        test("should swallow exception thrown by executed recipe when configured to do so") {
            // given
            val recipe = FakeThrowingRecipe()

            // expect
            shouldNotThrow<Throwable> {
                recipeExecutor.execute(recipe, inputFiles(), false)
            }
        }

        test("should execute post-processing recipe") {
            // given
            val recipe = FakePostProcessingRecipe()

            // when
            recipeExecutor.execute(recipe, inputFiles(), true)

            // then
            recipe.executionCount shouldBe 1
        }

        test("should output PR comment when failed to execute post-processing recipe") {
            // given
            val recipe = FakePostProcessingRecipe(mockedResult = Failure("Unable to execute recipe"))

            // when
            recipeExecutor.execute(recipe, inputFiles(), true)

            // then
            recipe.executionCount shouldBe 1

            // and
            fakeProblemReporter.reportedProblems shouldContainExactly listOf(
                Problem("Unable to execute recipe")
            )
        }

        test("should output PR comment from multiple post-processing recipes") {
            // given
            val recipe1 = FakePostProcessingRecipe(mockedResult = Failure("Unable to execute recipe1"))
            val recipe2 = FakePostProcessingRecipe(mockedResult = Failure("Unable to execute recipe2"))
            val parentRecipe = FakeCompositeRecipe(recipe1, recipe2)

            // when
            recipeExecutor.execute(parentRecipe, inputFiles(), true)

            // then
            recipe1.executionCount shouldBe 1
            recipe2.executionCount shouldBe 1

            // and
            fakeProblemReporter.reportedProblems shouldContainExactly listOf(
                Problem("Unable to execute recipe1"),
                Problem("Unable to execute recipe2")
            )
        }

        // TODO make writing modified files an outgoing port (implemented by OperatingSystemModule) that can be mocked and write tests for that
    }

    private fun inputFiles(): List<Path> =
        Paths.get("src/testFixtures/inputFilesForTests").walk().toList()
}
