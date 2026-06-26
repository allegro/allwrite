package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.config.Environment
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.java.JavaParser
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.groovy
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.recipes.kotlin

class ReplaceStatusCodeValueSpikeTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        val environment = Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.spring")
            .build()
        val recipe = environment.activateRecipes("org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0")
        val ctx = InMemoryExecutionContext()

        spec
            .recipe(recipe)
            .parser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "spring-web-6"))
            .parser(KotlinParser.builder().classpathFromResources(ctx, "spring-web-6"))
            .parser(GroovyParser.builder().classpathFromResource(ctx, "spring-web-6"))
    }

    @Test
    fun `should replace getStatusCodeValue() method call in Java`() {
        rewriteRun(
            java(
                before = """
                import org.springframework.http.ResponseEntity;

                class Example {
                    void test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello");
                        int status = response.getStatusCodeValue();
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity;

                class Example {
                    void test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello");
                        int status = response.getStatusCode().value();
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should replace statusCodeValue property access in Groovy`() {
        rewriteRun(
            groovy(
                before = """
                import org.springframework.http.ResponseEntity

                class Example {
                    void test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello")
                        int status = response.statusCodeValue
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity

                class Example {
                    void test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello")
                        int status = response.statusCode.value()
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should replace getStatusCodeValue() method call in Groovy`() {
        rewriteRun(
            groovy(
                before = """
                import org.springframework.http.ResponseEntity

                class Example {
                    void test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello")
                        int status = response.getStatusCodeValue()
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity

                class Example {
                    void test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello")
                        int status = response.getStatusCode().value()
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should replace statusCodeValue property access in Kotlin`() {
        rewriteRun(
            kotlin(
                before = """
                import org.springframework.http.ResponseEntity

                class Example {
                    fun test() {
                        val response: ResponseEntity<String> = ResponseEntity.ok("hello")
                        val status: Int = response.statusCodeValue
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity

                class Example {
                    fun test() {
                        val response: ResponseEntity<String> = ResponseEntity.ok("hello")
                        val status: Int = response.statusCode.value()
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should replace getStatusCodeValue() method call in Kotlin`() {
        rewriteRun(
            kotlin(
                before = """
                import org.springframework.http.ResponseEntity

                class Example {
                    fun test() {
                        val response: ResponseEntity<String> = ResponseEntity.ok("hello")
                        val status: Int = response.getStatusCodeValue()
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity

                class Example {
                    fun test() {
                        val response: ResponseEntity<String> = ResponseEntity.ok("hello")
                        val status: Int = response.getStatusCode().value()
                    }
                }
                """.trimIndent(),
            ),
        )
    }
}
