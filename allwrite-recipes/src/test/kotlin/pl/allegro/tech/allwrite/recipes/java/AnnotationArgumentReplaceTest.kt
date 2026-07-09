package pl.allegro.tech.allwrite.recipes.java

import io.kotest.matchers.equals.shouldBeEqual
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Space
import org.openrewrite.kotlin.internal.KotlinPrinter
import pl.allegro.tech.allwrite.recipes.util.JavaStringLiteral
import pl.allegro.tech.allwrite.recipes.util.KotlinStringListLiteral
import pl.allegro.tech.allwrite.recipes.util.ParsingTest

class AnnotationArgumentReplaceTest : ParsingTest() {

    @Nested
    inner class ScalarArgumentCases {
        @Test
        fun `should replace default scalar argument with the given value`() {
            val j = parseJava(
                """
                    @SuppressWarnings("a")
                    class A {
                    }
                """.trimIndent(),
            ).classes[0].leadingAnnotations[0] as J.Annotation

            // when
            val argument = j.getArgument("value")!!
            val replacement = stringArray(listOf("a", "b", "c"))
            val newAnnotation = argument.replaceWith(replacement)

            // then
            newAnnotation.print() shouldBeEqual "@SuppressWarnings({\"a\", \"b\", \"c\"})"
        }

        @Test
        fun `should replace default scalar argument with assignment`() {
            val j = parseJava(
                """
                    @SuppressWarnings("a")
                    class A {
                    }
                """.trimIndent(),
            ).classes[0].leadingAnnotations[0] as J.Annotation

            // when
            val argument = j.getArgument("value")!!
            val replacement = stringArray(listOf("a", "b", "c")).withPrefix(Space.SINGLE_SPACE)
            val newAnnotation = argument.replaceWith(assignment("named", replacement))

            // then
            newAnnotation.print() shouldBeEqual "@SuppressWarnings(named = {\"a\", \"b\", \"c\"})"
        }

        @Test
        fun `should replace named default argument scalar with the given value`() {
            val j = parseJava(
                """
                    @SuppressWarnings(value = "a")
                    class A {
                    }
                """.trimIndent(),
            ).classes[0].leadingAnnotations[0] as J.Annotation

            // when
            val argument = j.getArgument("value")!!
            val replacement = stringArray(listOf("a", "b", "c"))
            val newAnnotation = argument.replaceWith(replacement)

            // then
            newAnnotation.print() shouldBeEqual "@SuppressWarnings({\"a\", \"b\", \"c\"})"
        }

        @Test
        fun `should replace named default scalar argument with assignment`() {
            val j = parseJava(
                """
                    @SuppressWarnings(value = "a")
                    class A {
                    }
                """.trimIndent(),
            ).classes[0].leadingAnnotations[0] as J.Annotation

            // when
            val argument = j.getArgument("value")!!
            val replacement = stringArray(listOf("a", "b", "c")).withPrefix(Space.SINGLE_SPACE)
            val newAnnotation = argument.replaceWith(assignment("named", replacement))

            // then
            newAnnotation.print() shouldBeEqual "@SuppressWarnings(named = {\"a\", \"b\", \"c\"})"
        }

        @Test
        fun `should remove default scalar argument when replacement is null`() {
            val j = parseJava(
                """
                    @SuppressWarnings("a")
                    class A {
                    }
                """.trimIndent(),
            ).classes[0].leadingAnnotations[0] as J.Annotation

            // when
            val argument = j.getArgument("value")!!
            val newAnnotation = argument.replaceWith(null)

            // then
            newAnnotation.print() shouldBeEqual "@SuppressWarnings"
        }

        @Test
        fun `should remove named default scalar argument when replacement is null`() {
            val j = parseJava(
                """
                    @SuppressWarnings(value = "a")
                    class A {
                    }
                """.trimIndent(),
            ).classes[0].leadingAnnotations[0] as J.Annotation

            // when
            val argument = j.getArgument("value")!!
            val newAnnotation = argument.replaceWith(null)

            // then
            newAnnotation.print() shouldBeEqual "@SuppressWarnings"
        }

        @Test
        fun `should remove named default scalar argument when replacement is null and other argument is present`() {
            val j = parseJava(
                """
                    @SuppressWarnings(value = "a", other = "b")
                    class A {}
                """.trimIndent(),
            ).classes[0].leadingAnnotations[0] as J.Annotation

            // when
            val argument = j.getArgument("value")!!
            val newAnnotation = argument.replaceWith(null)

            // then
            newAnnotation.print() shouldBeEqual "@SuppressWarnings(other = \"b\")"
        }
    }

    @Nested
    inner class ListValueCases {

        @Nested
        inner class JavaCasse {
            @Test
            fun `should reuse container to keep formatting`() {
                val j = parseJava(
                    """
                    @SuppressWarnings(value = // comment
                               {"a"},     // some weird formatting and comment
                      other = "b"
                    )
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = j.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(listOf(JavaStringLiteral("b")))

                // then
                newAnnotation.print() shouldBeEqual """
                @SuppressWarnings(value = // comment
                           {"b"},     // some weird formatting and comment
                  other = "b"
                )
                """.trimIndent()
            }

            @Test
            fun `should replace elements`() {
                val j = parseJava(
                    """
                    @SuppressWarnings(value = {"a", "b"}, other = "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = j.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(listOf(JavaStringLiteral("1"), JavaStringLiteral("2")))

                // then
                newAnnotation.print() shouldBeEqual "@SuppressWarnings(value = {\"1\",\"2\"}, other = \"b\")"
            }

            @Test
            fun `should replace with a single element`() {
                val j = parseJava(
                    """
                    @SuppressWarnings(value = {"a", "b"}, other = "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = j.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceWith(JavaStringLiteral("a"))

                // then
                newAnnotation.print() shouldBeEqual "@SuppressWarnings(value = \"a\", other = \"b\")"
            }

            @Test
            fun `should remove when replacement elements is null`() {
                val j = parseJava(
                    """
                    @SuppressWarnings(value = {"a", "b"})
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = j.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(null)

                // then
                newAnnotation.print() shouldBeEqual "@SuppressWarnings"
            }

            @Test
            fun `should remove when replacement elements is null and other argument is present`() {
                val j = parseJava(
                    """
                    @SuppressWarnings(value = {"a", "b"}, other = "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = j.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(null)

                // then
                newAnnotation.print() shouldBeEqual "@SuppressWarnings(other = \"b\")"
            }

            @Test
            fun `should remove when replacement elements is empty`() {
                val j = parseJava(
                    """
                    @SuppressWarnings(value = {"a", "b"})
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = j.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(emptyList())

                // then
                newAnnotation.print() shouldBeEqual "@SuppressWarnings"
            }

            @Test
            fun `should remove when replacement elements is empty and other argument is present`() {
                val j = parseJava(
                    """
                    @SuppressWarnings(value = {"a", "b"}, other = "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = j.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(emptyList())

                // then
                newAnnotation.print() shouldBeEqual "@SuppressWarnings(other = \"b\")"
            }
        }

        @Nested
        inner class KotlinCases {
            @Test
            fun `should reuse container to keep formatting`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings(value = // comment
                               ["a"],     // some weird formatting and comment
                      other = "b"
                    )
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(listOf(JavaStringLiteral("1"), JavaStringLiteral("2")))

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual """
                @SuppressWarnings(value = // comment
                           ["1","2"],     // some weird formatting and comment
                  other = "b"
                )
                """.trimIndent()
            }

            @Test
            fun `should replace elements`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings(value = ["a", "b"], other = "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(listOf(JavaStringLiteral("1"), JavaStringLiteral("2")))

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual "@SuppressWarnings(value = [\"1\",\"2\"], other = \"b\")"
            }

            @Test
            fun `should replace with a single element`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings(value = ["a", "b"], other = "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(listOf(JavaStringLiteral("a")))

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual "@SuppressWarnings(value = [\"a\"], other = \"b\")"
            }

            @Test
            fun `should remove when replacement elements is null`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings(value = ["a", "b"])
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(null)

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual "@SuppressWarnings"
            }

            @Test
            fun `should remove when replacement elements is null and other argument is present`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings(value = ["a", "b"], other = "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(null)

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual "@SuppressWarnings(other = \"b\")"
            }

            @Test
            fun `should remove when replacement elements is empty`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings(value = ["a", "b"])
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(emptyList())

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual "@SuppressWarnings"
            }

            @Test
            fun `should remove when replacement elements is empty and other argument is present`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings(value = ["a", "b"], other = "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(emptyList())

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual "@SuppressWarnings(other = \"b\")"
            }

            @Test
            fun `should replace elements in default vararg argument with a single element`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings("a", "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(listOf(JavaStringLiteral("c")))

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual "@SuppressWarnings(\"c\")"
            }

            @Test
            fun `should replace elements in default vararg argument with multiple elements`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings("a", "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(listOf(JavaStringLiteral("c"), JavaStringLiteral("d")))

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual "@SuppressWarnings(\"c\",\"d\")"
            }

            @Test
            fun `should replace elements in default vararg argument with assignment`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings("a", "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceWith(assignment("new", JavaStringLiteral("a")))

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual "@SuppressWarnings(new =\"a\")"
            }

            @Test
            fun `should replace elements in default vararg argument with list literal`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings("a", "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceWith(KotlinStringListLiteral(values = listOf("1", "2")).withPrefix(Space.SINGLE_SPACE))

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual "@SuppressWarnings(value = [\"1\", \"2\"])"
            }

            @Test
            fun `should remove default vararg argument when replacement elements is null`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings("a", "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(null)

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual "@SuppressWarnings"
            }

            @Test
            fun `should remove default vararg argument when replacement elements is empty`() {
                val kt = parseKotlin(
                    """
                    @SuppressWarnings("a", "b")
                    class A {}
                    """.trimIndent(),
                ).classes[0].leadingAnnotations[0] as J.Annotation

                // when
                val argument = kt.getArgument("value") as MultiValueAnnotationArgument
                val newAnnotation = argument.replaceElements(emptyList())

                // then
                newAnnotation.print(KotlinPrinter()) shouldBeEqual "@SuppressWarnings"
            }
        }
    }
}
