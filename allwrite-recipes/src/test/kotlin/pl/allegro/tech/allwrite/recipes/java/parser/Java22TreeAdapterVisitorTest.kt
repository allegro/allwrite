package pl.allegro.tech.allwrite.recipes.java.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.test.RewriteTest
import org.openrewrite.tree.ParseError
import kotlin.jvm.optionals.getOrNull

class Java22TreeAdapterVisitorTest : RewriteTest {

    @BeforeEach
    fun setup() {
        javaParser.reset()
    }

    @Test
    fun `should fill in the name of the unnamed lambda argument`() {
        // given
        val source = "public class A { Function<String, String> dummy = _ -> \"dummy\"; }"
        val error = javaParser.parse(source).findFirst().getOrNull() as ParseError
        val j = error.erroneous as J.CompilationUnit

        // when
        val result = Java22TreeAdapterVisitor().visit(j, InMemoryExecutionContext()) as J.CompilationUnit

        // then
        assertNotEquals(source, j.print(Cursor(null, j)))
        assertEquals(source, result.print(Cursor(null, result)))
    }

    companion object {
        val javaParser: JavaParser = JavaParser.fromJavaVersion().build()
    }
}
