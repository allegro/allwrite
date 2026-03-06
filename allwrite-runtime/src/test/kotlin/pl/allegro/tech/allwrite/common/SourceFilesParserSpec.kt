package pl.allegro.tech.allwrite.common

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.koin.core.component.inject
import org.openrewrite.InMemoryExecutionContext
import pl.allegro.tech.allwrite.common.SourceFilesParser
import pl.allegro.tech.allwrite.common.base.BaseRuntimeSpec
import pl.allegro.tech.allwrite.common.fake.FakeParsingAwareRecipe
import pl.allegro.tech.allwrite.common.fake.FakeRecipe
import java.nio.file.Paths
import kotlin.io.path.walk

class SourceFilesParserSpec : BaseRuntimeSpec() {

    private val sourceFilesParser: SourceFilesParser by inject()

    init {
        test("should parse all files") {
            // given
            val recipe = FakeRecipe()
            val inputFiles = Paths.get("src/testFixtures/inputFilesForTests").walk().toList()

            // when
            val parsedFiles = sourceFilesParser.parseSourceFiles(recipe, inputFiles, InMemoryExecutionContext())

            // then
            parsedFiles.map { it.sourcePath } shouldContainExactlyInAnyOrder listOf(
                Paths.get("src/testFixtures/inputFilesForTests/interesting-dir/some-file.yaml"),
                Paths.get("src/testFixtures/inputFilesForTests/interesting-dir/another-file.yaml"),
                Paths.get("src/testFixtures/inputFilesForTests/large-file.yaml"),
            )
        }

        test("should parse only files selected by recipe") {
            // given
            val recipe = FakeParsingAwareRecipe()
            val inputFiles = Paths.get("src/testFixtures/inputFilesForTests").walk().toList()

            // when
            val parsedFiles = sourceFilesParser.parseSourceFiles(recipe, inputFiles, InMemoryExecutionContext())

            // then
            parsedFiles.map { it.sourcePath } shouldContainExactlyInAnyOrder listOf(
                Paths.get("src/testFixtures/inputFilesForTests/interesting-dir/some-file.yaml"),
                Paths.get("src/testFixtures/inputFilesForTests/interesting-dir/another-file.yaml"),
            )
        }
    }
}
