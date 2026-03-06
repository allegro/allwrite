package pl.allegro.tech.allwrite.recipes.spring.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.openrewrite.java.tree.J

class ClassDeclarationTest : ParsingTest() {

    @BeforeEach
    fun setup() {
        javaParser.reset()
    }

    @Nested
    inner class GetSpringComponentAnnotation {

        @ParameterizedTest(name = "should return annotation and a qualified name when {0} annotation is present")
        @ValueSource(strings = [
            ANNOTATION_COMPONENT,
            ANNOTATION_SERVICE,
            ANNOTATION_REPOSITORY,
            ANNOTATION_CONTROLLER,
            ANNOTATION_REST_CONTROLLER,
            ANNOTATION_CONFIGURATION,
        ])
        fun `getSpringComponentAnnotation() should return spring component annotation and a qualified name when it is present`(annotation: String) {
            val jclass = parse(
                """
                @Deprecated(since = "1.5")
                @$annotation("c")
                class A {}
                """.trimIndent()).classes[0]

            // when
            val result = jclass.getSpringComponentAnnotation()

            // then
            assertNotNull(result)
            assertEquals("c", result.name)
            assertEquals(jclass.leadingAnnotations[1], result.annotation)
        }

        @Test
        fun `getSpringComponentAnnotation() should select explicit name from stereotype annotation over explicit name from @Named`() {
            val jclass = parse(
                """
                import org.springframework.stereotype.Component;
                import jakarta.inject.Named;
                
                @Named("n")
                @Component("c")
                class A {}
                """.trimIndent()).classes[0]

            // when
            val result = jclass.getSpringComponentAnnotation()

            // then
            assertNotNull(result)
            assertEquals("c", result.name)
            assertEquals(jclass.leadingAnnotations[1], result.annotation)
        }

        @Test
        fun `getSpringComponentAnnotation() should select explicit name from @Named over implicit name`() {
            val jclass = parse(
                """
                import org.springframework.stereotype.Component;
                import jakarta.inject.Named;
                
                @Named("n")
                @Component
                class A {}
                """.trimIndent()).classes[0]

            // when
            val result = jclass.getSpringComponentAnnotation()

            // then
            assertNotNull(result)
            assertEquals("n", result.name)
            assertEquals(jclass.leadingAnnotations[0], result.annotation)
        }

        @Test
        fun `getSpringComponentAnnotation() should select implicit name when neither @Named nor stereotype is present`() {
            val jclass = parse(
                """
                import org.springframework.stereotype.Component;
                
                @Component
                class A {}
                """.trimIndent()).classes[0]

            // when
            val result = jclass.getSpringComponentAnnotation()

            // then
            assertNotNull(result)
            assertEquals("a", result.name)
            assertEquals(jclass.leadingAnnotations[0], result.annotation)
        }

        @Test
        fun `getSpringComponentAnnotation() should select explicit name from stereotype annotation over implicit`() {
            val jclass = parse(
                """
                import org.springframework.stereotype.Component;
                
                @Component("c")
                class A {}
                """.trimIndent()).classes[0]

            // when
            val result = jclass.getSpringComponentAnnotation()

            // then
            assertNotNull(result)
            assertEquals("c", result.name)
            assertEquals(jclass.leadingAnnotations[0], result.annotation)
        }

        @Test
        fun `getSpringComponentAnnotation() should select explicit name @Named over implicit`() {
            val jclass = parse(
                """
                import jakarta.inject.Named;
                
                @Named("n")
                class A {}
                """.trimIndent()).classes[0]

            // when
            val result = jclass.getSpringComponentAnnotation()

            // then
            assertNotNull(result)
            assertEquals("n", result.name)
            assertEquals(jclass.leadingAnnotations[0], result.annotation)
        }

        @Test
        fun `getSpringComponentAnnotation() should return null when there is no component annotation`() {
            val jclass = parse(
                """
                @Deprecated(since = "1.5")
                class A {}
                """.trimIndent()).classes[0]

            // when
            val result = jclass.getSpringComponentAnnotation()

            // then
            assertNull(result)
        }
    }

    @Nested
    inner class GetBeanMethodDeclarations {

        @Test
        fun `should all methods annotated with @Bean`() {
            val jclass = parse(
                """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                
                @Configuration
                class Config {
                  @Bean
                  public String str() { return "str"; }
                
                  public void v() {}
                  public int i() { return 42; }
                
                  @Bean(name = "fiftyTwo")
                  public int i(String str) { return 52; }
                }
                """.trimIndent()).classes[0]

            // when
            val result = jclass.getBeanMethodDeclarations()

            // then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.method }).containsExactly(
                jclass.body.statements[0] as J.MethodDeclaration,
                jclass.body.statements[3] as J.MethodDeclaration
            )
            assertThat(result.map { it.beanName }).containsExactly("str", "fiftyTwo")
        }

        @Test
        fun `should all kotlin methods annotated with @Bean`() {
            val jclass = parseKotlin(
                """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                
                @Configuration
                class Config {
                  @Bean
                  fun str() = "str"
                
                  fun v() {}
                  fun i() = 42
                
                  @Bean(name = "fiftyTwo")
                  fun i(str: String): Int { return 52 }
                }
                """.trimIndent()).classes[0]

            // when
            val result = jclass.getBeanMethodDeclarations()

            // then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.method }).containsExactly(
                jclass.body.statements[0] as J.MethodDeclaration,
                jclass.body.statements[3] as J.MethodDeclaration
            )
            assertThat(result.map { it.beanName }).containsExactly("str", "fiftyTwo")
        }
    }

    @Nested
    inner class GetAutowiredFields {
        @Test
        fun `should return all autowired fields`() {
            val jclass = parse(
                """
                import org.springframework.beans.factory.annotation.Autowired;
                import jakarta.annotation.Resource;
                
                @Configuration
                class Config {
                  @Autowired
                  String str;
                
                  Integer x, y;
                
                  @Resource(name = "int")
                  Integer i;
                }
                """.trimIndent()).classes[0]

            // when
            val result = jclass.getAutowiredFields()

            // then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.declaration }).containsExactly(
                jclass.body.statements[0] as J.VariableDeclarations,
                jclass.body.statements[2] as J.VariableDeclarations
            )
            assertThat(result.map { it.name }).containsExactly("str", "int")
            assertThat(result.map { it.qualifiedName }).containsExactly(null, "int")
            assertThat(result.map { it.variableName }).containsExactly("str", "i")
        }
    }
}
