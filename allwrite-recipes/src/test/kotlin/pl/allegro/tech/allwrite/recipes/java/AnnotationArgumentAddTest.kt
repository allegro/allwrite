package pl.allegro.tech.allwrite.recipes.java

import io.kotest.matchers.equals.shouldBeEqual
import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.java.tree.J
import pl.allegro.tech.allwrite.recipes.util.JavaStringLiteral
import pl.allegro.tech.allwrite.recipes.util.ParsingTest

class AnnotationArgumentAddTest : ParsingTest() {

    @Test
    fun `should add argument to the annotation`() {
        val j = parseJava("@SuppressWarnings class A {}")
        val annotation = j.classes[0].leadingAnnotations[0] as J.Annotation

        // then
        val result = annotation.addArgument("x", JavaStringLiteral("a"), Cursor(null, j))
        result.print() shouldBeEqual "@SuppressWarnings(x = \"a\")"
    }

    @Test
    fun `should replace argument if it already exists`() {
        val j = parseJava("@SuppressWarnings(x = \"1\") class A {}")
        val annotation = j.classes[0].leadingAnnotations[0] as J.Annotation

        // then
        val result = annotation.addArgument("x", JavaStringLiteral("2"), Cursor(null, j))
        result.print() shouldBeEqual "@SuppressWarnings(x = \"2\")"
    }

    @Test
    fun `should add argument and explicit name to the default argument to the annotation`() {
        val j = parseJava("""
                    @SuppressWarnings("a")
                    class A {
                    }
                    """.trimIndent())
        val annotation = j.classes[0].leadingAnnotations[0] as J.Annotation

        // then
        val result = annotation.addArgument("x", JavaStringLiteral("b"), Cursor(null, j))
        result.print() shouldBeEqual "@SuppressWarnings(value = \"a\", x = \"b\")"
    }

    @Test
    fun `should add argument and explicit name to the default argument to the annotation and wrap with array literal in kotlin`() {
        val kt = parseKotlin("""
                    @SuppressWarnings("a")
                    class A {
                    }
                    """.trimIndent())
        val annotation = kt.classes[0].leadingAnnotations[0] as J.Annotation
        val cursor = Cursor(null, kt)

        // then
        val result = annotation.addArgument("x", JavaStringLiteral("b"), cursor)
        result.print(cursor) shouldBeEqual "@SuppressWarnings(value = [\"a\"], x = \"b\")"
    }
}
