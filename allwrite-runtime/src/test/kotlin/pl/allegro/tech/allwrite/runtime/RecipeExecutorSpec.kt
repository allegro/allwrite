package pl.allegro.tech.allwrite.runtime

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.data.Row1
import io.kotest.data.forAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.TreeVisitor
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.PostprocessingResult.Failure
import pl.allegro.tech.allwrite.api.RecipeExecutor
import pl.allegro.tech.allwrite.runtime.base.BaseRuntimeSpec
import pl.allegro.tech.allwrite.runtime.fake.FakeClasspathAwareRecipe
import pl.allegro.tech.allwrite.runtime.fake.FakeCompositeRecipe
import pl.allegro.tech.allwrite.runtime.fake.FakePostProcessingRecipe
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipe
import pl.allegro.tech.allwrite.runtime.fake.FakeThrowingRecipe
import pl.allegro.tech.allwrite.runtime.fake.FakeUserProblemReporter
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
                Problem("Unable to execute recipe"),
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
                Problem("Unable to execute recipe2"),
            )
        }

        test("should not split into phases when no ClasspathAwareRecipe is present") {
            // given
            val executionOrder = mutableListOf<String>()
            val recipe1 = OrderRecordingRecipe("recipe1", executionOrder)
            val recipe2 = OrderRecordingRecipe("recipe2", executionOrder)
            val composite = FakeCompositeRecipe(recipe1, recipe2)

            // when
            recipeExecutor.execute(composite, inputFiles(), true)

            // then
            executionOrder.distinct() shouldContainExactly listOf("recipe1", "recipe2")
            recipe1.visitedSourceFiles.map { it.sourcePath } shouldContainExactlyInAnyOrder inputFiles()
            recipe2.visitedSourceFiles.map { it.sourcePath } shouldContainExactlyInAnyOrder inputFiles()
        }

        test("should split into isolated phases at ClasspathAwareRecipe boundaries, preserving order") {
            // given
            val executionOrder = mutableListOf<String>()
            val or1 = OrderRecordingRecipe("or1", executionOrder)
            val or2 = OrderRecordingRecipe("or2", executionOrder)
            val classpathRecipe1 = OrderRecordingClasspathAwareRecipe("classpathRecipe1", executionOrder)
            val or3 = OrderRecordingRecipe("or3", executionOrder)
            val classpathRecipe2 = OrderRecordingClasspathAwareRecipe("classpathRecipe2", executionOrder)
            val composite = FakeCompositeRecipe(or1, or2, classpathRecipe1, or3, classpathRecipe2)

            // when
            recipeExecutor.execute(composite, inputFiles(), true)

            // then
            executionOrder.distinct() shouldContainExactly listOf("or1", "or2", "classpathRecipe1", "or3", "classpathRecipe2")
            listOf(or1, or2, classpathRecipe1, or3, classpathRecipe2).forEach {
                it.visitedSourceFiles.map { file -> file.sourcePath } shouldContainExactlyInAnyOrder inputFiles()
            }
        }

        test("should run post-processing recipes from every isolated phase") {
            // given
            val postProcessingRecipe1 = FakePostProcessingRecipe(id = "postProcessingRecipe1")
            val classpathAwareRecipe = FakeClasspathAwareRecipe()
            val postProcessingRecipe2 = FakePostProcessingRecipe(id = "postProcessingRecipe2")
            val composite = FakeCompositeRecipe(postProcessingRecipe1, classpathAwareRecipe, postProcessingRecipe2)

            // when
            recipeExecutor.execute(composite, inputFiles(), true)

            // then
            postProcessingRecipe1.executionCount shouldBe 1
            postProcessingRecipe2.executionCount shouldBe 1
        }

        test("should recursively expand nested composite recipe containing ClasspathAwareRecipe") {
            // given
            val executionOrder = mutableListOf<String>()
            val nestedOr = OrderRecordingRecipe("nestedOr", executionOrder)
            val nestedClasspath = OrderRecordingClasspathAwareRecipe("nestedClasspath", executionOrder)
            val nestedComposite = FakeCompositeRecipe(nestedOr, nestedClasspath)

            val topLevelRecipe = OrderRecordingRecipe("topLevel", executionOrder)
            val outerComposite = FakeCompositeRecipe(nestedComposite, topLevelRecipe)

            // when
            recipeExecutor.execute(outerComposite, inputFiles(), true)

            // then
            executionOrder.distinct() shouldContainExactly listOf("nestedOr", "nestedClasspath", "topLevel")
            listOf(nestedOr, nestedClasspath, topLevelRecipe).forEach {
                it.visitedSourceFiles.map { file -> file.sourcePath } shouldContainExactlyInAnyOrder inputFiles()
            }
        }

        test("should recursively expand deeply nested composite recipe") {
            // given
            val executionOrder = mutableListOf<String>()
            val deepClasspath = OrderRecordingClasspathAwareRecipe("deepClasspath", executionOrder)
            val deepOr = OrderRecordingRecipe("deepOr", executionOrder)
            val innerComposite = FakeCompositeRecipe(deepClasspath, deepOr)

            val middleOr = OrderRecordingRecipe("middleOr", executionOrder)
            val middleComposite = FakeCompositeRecipe(middleOr, innerComposite)

            val topOr = OrderRecordingRecipe("topOr", executionOrder)
            val outerComposite = FakeCompositeRecipe(topOr, middleComposite)

            // when
            recipeExecutor.execute(outerComposite, inputFiles(), true)

            // then
            executionOrder.distinct() shouldContainExactly listOf("topOr", "middleOr", "deepClasspath", "deepOr")
        }

        test("should not expand nested composite recipe without ClasspathAwareRecipe") {
            // given
            val executionOrder = mutableListOf<String>()
            val nestedOr1 = OrderRecordingRecipe("nestedOr1", executionOrder)
            val nestedOr2 = OrderRecordingRecipe("nestedOr2", executionOrder)
            val nestedComposite = FakeCompositeRecipe(nestedOr1, nestedOr2)

            val topLevelRecipe = OrderRecordingRecipe("topLevel", executionOrder)
            val outerComposite = FakeCompositeRecipe(nestedComposite, topLevelRecipe)

            // when
            recipeExecutor.execute(outerComposite, inputFiles(), true)

            // then
            executionOrder.distinct() shouldContainExactlyInAnyOrder listOf("nestedOr1", "nestedOr2", "topLevel")
        }

        test("should split plain composite recipe containing ClasspathAwareRecipe") {
            // given
            val executionOrder = mutableListOf<String>()
            val or1 = OrderRecordingRecipe("or1", executionOrder)
            val classpathRecipe = OrderRecordingClasspathAwareRecipe("classpathRecipe", executionOrder)
            val or2 = OrderRecordingRecipe("or2", executionOrder)
            val plainComposite = PlainCompositeRecipe(or1, classpathRecipe, or2)

            // when
            recipeExecutor.execute(plainComposite, inputFiles(), true)

            // then
            executionOrder.distinct() shouldContainExactly listOf("or1", "classpathRecipe", "or2")
        }

        test("should expand nested ClasspathAwareRecipe inside plain composite") {
            // given
            val executionOrder = mutableListOf<String>()
            val nestedOr = OrderRecordingRecipe("nestedOr", executionOrder)
            val nestedClasspath = OrderRecordingClasspathAwareRecipe("nestedClasspath", executionOrder)
            val nestedComposite = FakeCompositeRecipe(nestedOr, nestedClasspath)

            val topLevelRecipe = OrderRecordingRecipe("topLevel", executionOrder)
            val plainComposite = PlainCompositeRecipe(nestedComposite, topLevelRecipe)

            // when
            recipeExecutor.execute(plainComposite, inputFiles(), true)

            // then
            executionOrder.distinct() shouldContainExactly listOf("nestedOr", "nestedClasspath", "topLevel")
        }

        // TODO make writing modified files an outgoing port (implemented by OperatingSystemModule) that can be mocked and write tests for that
    }

    private fun inputFiles(): List<Path> = Paths.get("src/testFixtures/inputFilesForTests").walk().toList()
}

private open class OrderRecordingRecipe(
    private val id: String,
    private val executionOrder: MutableList<String>,
) : FakeRecipe(id = id) {

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        executionOrder.add(id)
        return super.getVisitor()
    }
}

private class OrderRecordingClasspathAwareRecipe(
    id: String,
    executionOrder: MutableList<String>,
) : OrderRecordingRecipe(id, executionOrder),
    ClasspathAwareRecipe {

    override fun requireOnClasspath(): List<String> = emptyList()
}

private class PlainCompositeRecipe(
    private val children: List<Recipe>,
) : Recipe() {

    constructor(vararg children: Recipe) : this(children.toList())

    override fun getDisplayName(): String = "PlainCompositeRecipe"

    override fun getDescription(): String = "Simulates a DeclarativeRecipe loaded from YAML"

    override fun getRecipeList(): List<Recipe> = children
}
