package pl.allegro.tech.allwrite.runtime

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.koin.core.component.inject
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import pl.allegro.tech.allwrite.runtime.base.BaseRuntimeSpec
import pl.allegro.tech.allwrite.runtime.fake.FakeClasspathAwareRecipe
import pl.allegro.tech.allwrite.runtime.fake.FakeCompositeRecipe
import pl.allegro.tech.allwrite.runtime.fake.FakeParsingAwareRecipe
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipe
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
                Paths.get("src/testFixtures/inputFilesForTests/java-dir/MyConfig.java"),
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

        test("should resolve classpath from a direct ClasspathAwareRecipe") {
            // given
            val recipe = FakeClasspathAwareRecipe(classpath = listOf("spring-context-6"))
            val inputFiles = listOf(Paths.get("src/testFixtures/inputFilesForTests/java-dir/MyConfig.java"))

            // when
            val parsedFiles = sourceFilesParser.parseSourceFiles(recipe, inputFiles, InMemoryExecutionContext())

            // then
            val compilationUnit = parsedFiles.single().shouldBeInstanceOf<J.CompilationUnit>()
            val annotationType = compilationUnit.classes.single().leadingAnnotations.single().annotationType
            val resolvedType = (annotationType as J.Identifier).type
            resolvedType.shouldNotBeNull()
            val classType = resolvedType.shouldBeInstanceOf<JavaType.Class>()
            classType.fullyQualifiedName shouldBe "org.springframework.context.annotation.Configuration"
        }

        test("should resolve classpath from ClasspathAwareRecipe nested in a composite recipe") {
            // given
            val classpathAwareSubRecipe = FakeClasspathAwareRecipe(classpath = listOf("spring-context-6"))
            val compositeRecipe = FakeCompositeRecipe(classpathAwareSubRecipe)
            val inputFiles = listOf(Paths.get("src/testFixtures/inputFilesForTests/java-dir/MyConfig.java"))

            // when
            val parsedFiles = sourceFilesParser.parseSourceFiles(compositeRecipe, inputFiles, InMemoryExecutionContext())

            // then
            val compilationUnit = parsedFiles.single().shouldBeInstanceOf<J.CompilationUnit>()
            val annotationType = compilationUnit.classes.single().leadingAnnotations.single().annotationType
            val resolvedType = (annotationType as J.Identifier).type
            resolvedType.shouldNotBeNull()
            val classType = resolvedType.shouldBeInstanceOf<JavaType.Class>()
            classType.fullyQualifiedName shouldBe "org.springframework.context.annotation.Configuration"
        }

        test("should resolve classpath from ClasspathAwareRecipe deeply nested in composite recipes") {
            // given
            val classpathAwareSubRecipe = FakeClasspathAwareRecipe(classpath = listOf("spring-context-6"))
            val innerComposite = FakeCompositeRecipe(FakeRecipe(), classpathAwareSubRecipe)
            val outerComposite = FakeCompositeRecipe(innerComposite)
            val inputFiles = listOf(Paths.get("src/testFixtures/inputFilesForTests/java-dir/MyConfig.java"))

            // when
            val parsedFiles = sourceFilesParser.parseSourceFiles(outerComposite, inputFiles, InMemoryExecutionContext())

            // then
            val compilationUnit = parsedFiles.single().shouldBeInstanceOf<J.CompilationUnit>()
            val annotationType = compilationUnit.classes.single().leadingAnnotations.single().annotationType
            val resolvedType = (annotationType as J.Identifier).type
            resolvedType.shouldNotBeNull()
            val classType = resolvedType.shouldBeInstanceOf<JavaType.Class>()
            classType.fullyQualifiedName shouldBe "org.springframework.context.annotation.Configuration"
        }
    }
}
