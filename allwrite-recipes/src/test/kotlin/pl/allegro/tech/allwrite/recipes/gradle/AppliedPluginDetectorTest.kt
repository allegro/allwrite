package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Parser
import org.openrewrite.SourceFile
import org.openrewrite.gradle.GradleParser
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Paths
import kotlin.jvm.optionals.getOrNull

class AppliedPluginDetectorTest {
    private val parser = GradleParser.builder().build()
    private val ctx: ExecutionContext = InMemoryExecutionContext()

    @Test
    fun `detects plugin declarations in Kotlin and Groovy build files`() {
        // given
        val cases = listOf(
            "plugins { application }" to "build.gradle.kts",
            "plugins { `application` }" to "build.gradle.kts",
            """plugins { id("application") }""" to "build.gradle.kts",
            """plugins { id("application") version "1.0" }""" to "build.gradle.kts",
            """plugins { id 'application' }""" to "build.gradle",
            """plugins { id 'application' version '1.0' }""" to "build.gradle",
            """plugins { id "application" }""" to "build.gradle",
            """apply(plugin = "application")""" to "build.gradle.kts",
            """apply plugin: 'application'""" to "build.gradle",
        )

        // when
        cases.forEach { (source, path) ->
            // then
            assertTrue(
                AppliedPluginDetector("application").isAppliedIn(parse(source, path), ctx),
                "Expected plugin declaration to be detected in $path: $source",
            )
        }
    }

    @Test
    fun `does not detect unrelated identifiers or plugin ids`() {
        // given
        val cases = listOf(
            "val application = \"value\"" to "build.gradle.kts",
            "application { mainClass.set(\"example.Main\") }" to "build.gradle.kts",
            """plugins { id("application-conventions") }""" to "build.gradle.kts",
            """plugins { id 'application-conventions' }""" to "build.gradle",
            """plugins { convention.id("application") }""" to "build.gradle.kts",
            """convention.apply(plugin = "application")""" to "build.gradle.kts",
            """plugins { convention.id 'application' }""" to "build.gradle",
            """convention.apply plugin: 'application'""" to "build.gradle",
            """plugins { id("application") apply false }""" to "build.gradle.kts",
            """plugins { id 'application' apply false }""" to "build.gradle",
            "repositories { mavenCentral() }" to "build.gradle.kts",
        )

        // when
        cases.forEach { (source, path) ->
            // then
            assertFalse(
                AppliedPluginDetector("application").isAppliedIn(parse(source, path), ctx),
                "Expected plugin declaration not to be detected in $path: $source",
            )
        }
    }

    @Test
    fun `does not inspect non build files`() {
        // given
        val sourceFile = parse("plugins { application }", "settings.gradle.kts")

        // when
        val detected = AppliedPluginDetector("application").isAppliedIn(sourceFile, ctx)

        // then
        assertFalse(detected)
    }

    private fun parse(source: String, path: String): SourceFile =
        parser.parseInputs(
            listOf(
                Parser.Input(Paths.get(path)) {
                    ByteArrayInputStream(source.toByteArray(UTF_8))
                },
            ),
            null,
            ctx,
        ).findFirst().getOrNull()!!
}
