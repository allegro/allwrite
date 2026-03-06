package pl.allegro.tech.allwrite.recipes.java

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.kotlin.tree.K
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass

class AnnotationArgumentGetArgumentTest {

    @BeforeEach
    fun setup() {
        javaParser.reset()
    }

    @Nested
    inner class DefaultAttributeCases {
        @Nested
        inner class JavaCases {
            @Test
            fun `should get scalar value when it is not named explicitly`() {
                val j = parseJava("""
                    @SuppressWarnings("a")
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = j.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<PrimitiveAnnotationArgument>()
                argument.literal.shouldBeTypeOf<J.Literal>()
                argument.literal.value.toString() shouldBeEqual "a"
            }

            @Test
            fun `should get scalar value when it is named explicitly`() {
                val j = parseJava("""
                    @SuppressWarnings(value = "a")
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = j.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<PrimitiveAnnotationArgument>()
                argument.literal.shouldBeTypeOf<J.Literal>()
                argument.literal.value.toString() shouldBeEqual "a"
            }

            @Test
            fun `should get list value when it is not named explicitly`() {
                val j = parseJava("""
                    @SuppressWarnings({"a", "b"})
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = j.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<ListAnnotationArgument>()
                argument.elements shouldHaveSize 2
                argument.elements.map { it as J.Literal }.map { it.value }.shouldContainExactly(listOf("a", "b"))
            }

            @Test
            fun `should get list value when it is named explicitly`() {
                val j = parseJava("""
                    @SuppressWarnings(value = {"a", "b"})
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = j.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<ListAnnotationArgument>()
                argument.elements shouldHaveSize 2
                argument.elements.map { it as J.Literal }.map { it.value }.shouldContainExactly(listOf("a", "b"))
            }
        }

        @Nested
        inner class KotlinCases {
            @Test
            fun `should get scalar value when it is not named explicitly`() {
                val kt = parseKotlin("""
                    @SuppressWarnings("a")
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = kt.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<PrimitiveAnnotationArgument>()
                argument.literal.shouldBeTypeOf<J.Literal>()
                argument.literal.value.toString() shouldBeEqual "a"
            }

            @Test
            fun `should get value as list when it is not named explicitly and initialized via varargs`() {
                val kt = parseKotlin("""
                    @SuppressWarnings("a", "b")
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = kt.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<VarArgAnnotationArgument>()
                argument.elements shouldHaveSize 2
                argument.elements.map { it as J.Literal }.map { it.value }.shouldContainExactly(listOf("a", "b"))
            }

            @Test
            fun `should get value as list literal when it is named explicitly and initialized with array literal`() {
                val kt = parseKotlin("""
                    @SuppressWarnings(value = ["a", "b"])
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = kt.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<ListAnnotationArgument>()
                argument.elements shouldHaveSize 2
                argument.elements.map { it as J.Literal }.map { it.value }.shouldContainExactly(listOf("a", "b"))
            }

            @Test
            fun `should get value as list when it is named explicitly and initialized via arrayOf`() {
                val kt = parseKotlin("""
                    @SuppressWarnings(value = arrayOf("a", "b"))
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = kt.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<ListAnnotationArgument>()
                argument.elements shouldHaveSize 2
                argument.elements.map { it as J.Literal }.map { it.value }.shouldContainExactly(listOf("a", "b"))
            }

            @Test
            fun `should get value as reference`() {
                val kt = parseKotlin("""
                    const val warning = "a"
                    @SuppressWarnings(warning)
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = kt.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<ReferenceAnnotationArgument>()
                argument.source.shouldBeTypeOf<J.Identifier>()
                argument.source.simpleName shouldBeEqual "warning"
            }

            @Test
            fun `should get value as calculated when it is initialized with interpolated string`() {
                val kt = parseKotlin("""
                    const val warning = "a"
                    @SuppressWarnings("a${"$"}warning")
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = kt.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<CalculatedAnnotationArgument>()
                argument.source.shouldBeTypeOf<K.StringTemplate>()
                argument.source.strings shouldHaveSize 2
            }
        }

        @Nested
        inner class GroovyCases {

            @Test
            fun `should get value as groovy scalar literal when it is named explicitly`() {
                val g = parseGroovy("""
                    @SuppressWarnings(value = "a")
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = g.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<PrimitiveAnnotationArgument>()
                argument.literal.shouldBeTypeOf<J.Literal>()
                argument.literal.value.toString() shouldBeEqual "a"
            }

            @Test
            fun `should get value as groovy scalar literal when it is not named explicitly`() {
                val g = parseGroovy("""
                    @SuppressWarnings("a")
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = g.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<PrimitiveAnnotationArgument>()
                argument.literal.shouldBeTypeOf<J.Literal>()
                argument.literal.value.toString() shouldBeEqual "a"
            }

            @Test
            fun `should get value as groovy list literal when it is named explicitly`() {
                val g = parseGroovy("""
                    @SuppressWarnings(value = ["a", "b"])
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = g.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<ListAnnotationArgument>()
                argument.elements shouldHaveSize 2
                argument.elements.map { it as J.Literal }.map { it.value }.shouldContainExactly(listOf("a", "b"))
            }

            @Test
            fun `should get value as groovy list literal when it is not named explicitly`() {
                val g = parseGroovy("""
                    @SuppressWarnings(["a", "b"])
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = g.getArgument("value")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<ListAnnotationArgument>()
                argument.elements shouldHaveSize 2
                argument.elements.map { it as J.Literal }.map { it.value }.shouldContainExactly(listOf("a", "b"))
            }
        }
    }

    @Nested
    inner class NamedAttributeCases {

        @Test
        fun `should get a named argument when it is present`() {
            val j = parseJava("""
                @Deprecated(since = "1.5", forRemoval = true)
                class A {
                }
                """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

            // then
            j.getArgument("since")?.unwrapString()!! shouldBeEqual "1.5"
            j.getArgument("forRemoval")?.unwrapString()!! shouldBeEqual "true"
        }

        @Test
        fun `should return null argument when named argument is not present`() {
            val j = parseJava("""
                @Deprecated(since = "1.5", forRemoval = true)
                class A {
                }
                """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

            // then
            assertNull(j.getArgument("type"))
        }

        @Test
        fun `should return null argument when named argument is not present, but value is`() {
            val j = parseJava("""
                @SuppressWarnings("unchecked")
                class A {
                }
                """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

            // then
            assertNull(j.getArgument("since"))
        }

        @Nested
        inner class JavaCases {
            @Test
            fun `should return scalar argument when named list argument is initialized with scalar value`() {
                val j = parseJava("""
                    @pl.allegro.tech.allwrite.recipes.java.Example(namedArg = "123")
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                j.getArgument("namedArg")?.unwrapString()!! shouldBeEqual "123"
            }

            @Test
            fun `should return list argument when named list argument is initialized with list`() {
                val j = parseJava("""
                    @pl.allegro.tech.allwrite.recipes.java.Example(namedArg = {"a", "b"})
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = j.getArgument("namedArg")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<ListAnnotationArgument>()
                argument.elements shouldHaveSize 2
                argument.elements.map { it as J.Literal }.map { it.value }.shouldContainExactly(listOf("a", "b"))
            }
        }

        @Nested
        inner class KotlinCases {
            @Test
            fun `should return list argument when named list argument is initialized with list literal`() {
                val kt = parseKotlin("""
                    @pl.allegro.tech.allwrite.recipes.java.Example(namedArg = ["123"])
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = kt.getArgument("namedArg")
                argument.shouldBeTypeOf<ListAnnotationArgument>()
                argument.elements.map { it as J.Literal }.map { it.value }.shouldContainExactly(listOf("123"))
            }

            @Test
            fun `should return list argument when named list argument is initialized with arrayOf`() {
                val kt = parseKotlin("""
                    @pl.allegro.tech.allwrite.recipes.java.Example(namedArg = arrayOf("a", "b"))
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = kt.getArgument("namedArg")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<ListAnnotationArgument>()
                argument.elements shouldHaveSize 2
                argument.elements.map { it as J.Literal }.map { it.value }.shouldContainExactly(listOf("a", "b"))
            }

            @Test
            fun `should return appropriate argument type`() {
                val kt = parseKotlin("""
                    import pl.allegro.tech.allwrite.recipes.java.ExampleNested
                    import pl.allegro.tech.allwrite.recipes.java.Example
                    import java.time.DayOfWeek
                    
                    @Example(
                       namedArg = arrayOf("a", "b"),
                       clazz = A::class,
                       enum = DayOfWeek.MONDAY,
                       nested = SuppressWarnings("abc"),
                       int = 1,
                       double = 2.3,
                       bool = true
                    )
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                kt.getArgument("clazz").shouldBeTypeOf<ClassAnnotationArgument>()
                kt.getArgument("enum").shouldBeTypeOf<EnumAnnotationArgument>()
                kt.getArgument("nested").shouldBeTypeOf<AnnotationAnnotationArgument>()
                kt.getArgument("int").shouldBeTypeOf<PrimitiveAnnotationArgument>()
                kt.getArgument("double").shouldBeTypeOf<PrimitiveAnnotationArgument>()
                kt.getArgument("bool").shouldBeTypeOf<PrimitiveAnnotationArgument>()
            }

            @Test
            fun `should return reference argument type when accessing top-level const val`() {
                val kt = parseKotlin("""
                    import pl.allegro.tech.allwrite.recipes.java.ExampleNested
                    import pl.allegro.tech.allwrite.recipes.java.Example
                    import java.time.DayOfWeek
                    
                    const val names = arrayOf("a", "b")
                    const val clazz = A::class
                    const val enum = DayOfWeek.MONDAY
                    const val i = 42
                    const val d = 21.37
                    const val b = true
                    
                    @Example(
                       namedArg = names,
                       clazz = clazz,
                       enum = enum,
                       nested = SuppressWarnings("abc"),
                       int = i,
                       double = d,
                       bool = b
                    )
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                kt.getArgument("namedArg").shouldBeTypeOf<ReferenceAnnotationArgument>()
                kt.getArgument("clazz").shouldBeTypeOf<ReferenceAnnotationArgument>()
                kt.getArgument("enum").shouldBeTypeOf<ReferenceAnnotationArgument>()
                kt.getArgument("int").shouldBeTypeOf<ReferenceAnnotationArgument>()
                kt.getArgument("double").shouldBeTypeOf<ReferenceAnnotationArgument>()
                kt.getArgument("bool").shouldBeTypeOf<ReferenceAnnotationArgument>()
            }

            @Test
            fun `should return reference argument type when accessing object fields`() {
                val kt = parseKotlin("""
                    import pl.allegro.tech.allwrite.recipes.java.ExampleNested
                    import pl.allegro.tech.allwrite.recipes.java.Example
                    import java.time.DayOfWeek
                    
                    
                    @Example(
                       namedArg = A.names,
                       clazz = A.clazz,
                       enum = A.enum,
                       nested = SuppressWarnings("abc"),
                       int = A.i,
                       double = A.d,
                       bool = A.b
                    )
                    object A {
                    const val names = arrayOf("a", "b")
                    const val clazz = A::class
                    const val enum = DayOfWeek.MONDAY
                    const val i = 42
                    const val d = 21.37
                    const val b = true
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                kt.getArgument("namedArg").shouldBeTypeOf<ReferenceAnnotationArgument>()
                kt.getArgument("clazz").shouldBeTypeOf<ReferenceAnnotationArgument>()
                kt.getArgument("int").shouldBeTypeOf<ReferenceAnnotationArgument>()
                kt.getArgument("double").shouldBeTypeOf<ReferenceAnnotationArgument>()
                kt.getArgument("bool").shouldBeTypeOf<ReferenceAnnotationArgument>()

                // enum is still enum!
                kt.getArgument("enum").shouldBeTypeOf<EnumAnnotationArgument>()
            }

            @Test
            fun `should return enum argument when member imported directly`() {
                val kt = parseKotlin("""
                    import pl.allegro.tech.allwrite.recipes.java.Example
                    import java.time.DayOfWeek.MONDAY
                    
                    @Example(enum = MONDAY)
                    object A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                kt.getArgument("enum").shouldBeTypeOf<ReferenceAnnotationArgument>()
            }
        }

        @Nested
        inner class GroovyCases {
            @Test
            fun `should return scalar argument when named list argument is initialized with scalar value`() {
                val g = parseGroovy("""
                    @pl.allegro.tech.allwrite.recipes.java.Example(namedArg = "123")
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                g.getArgument("namedArg")?.unwrapString()!! shouldBeEqual "123"
            }

            @Test
            fun `should return list argument when named list argument is initialized with list`() {
                val g = parseGroovy("""
                    @pl.allegro.tech.allwrite.recipes.java.Example(namedArg = ["a", "b"])
                    class A {
                    }
                    """.trimIndent()).classes[0].leadingAnnotations[0] as J.Annotation

                // then
                val argument = g.getArgument("namedArg")
                argument.shouldNotBeNull()
                argument.shouldBeTypeOf<ListAnnotationArgument>()
                argument.elements shouldHaveSize 2
                argument.elements.map { it as J.Literal }.map { it.value }.shouldContainExactly(listOf("a", "b"))
            }
        }
    }

    fun parseJava(java: String): J.CompilationUnit = javaParser.parse(java).findFirst().getOrNull() as J.CompilationUnit
    fun parseKotlin(kotlin: String): K.CompilationUnit = kotlinParser.parse(kotlin).findFirst().getOrNull() as K.CompilationUnit
    fun parseGroovy(groovy: String): G.CompilationUnit = groovyParser.parse(groovy).findFirst().getOrNull() as G.CompilationUnit


    companion object {
        val javaParser: JavaParser = JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()).build()
        val kotlinParser: KotlinParser = KotlinParser.builder().classpath(JavaParser.runtimeClasspath()).build()
        val groovyParser: GroovyParser = GroovyParser.builder().classpath(JavaParser.runtimeClasspath()).build()
    }

    @Target(CLASS)
    annotation class Example(
        val namedArg: Array<String> = [],
        val clazz: KClass<*> = Any::class,
        val enum: java.time.DayOfWeek = java.time.DayOfWeek.MONDAY,
        val int: Int = 1,
        val double: Double = 2.0,
        val bool: Boolean = false,
        val nested: SuppressWarnings = SuppressWarnings(""),
    )
}
