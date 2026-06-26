package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.groovy
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.recipes.kotlin
import pl.allegro.tech.allwrite.runtime.util.withRecipeClasspath

class ReplaceStatusCodeValueTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(ReplaceStatusCodeValue())
            .withRecipeClasspath()
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
    fun `should not change getStatusCodeValue() when not from ResponseEntity in Java`() {
        rewriteRun(
            java(
                beforeAndAfter = """
                class NotResponseEntity {
                    int getStatusCodeValue() { return 200; }
                }

                class Example {
                    void test() {
                        NotResponseEntity response = new NotResponseEntity();
                        int status = response.getStatusCodeValue();
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should handle chained calls with getStatusCodeValue() in Java`() {
        rewriteRun(
            java(
                before = """
                import org.springframework.http.ResponseEntity;

                class Example {
                    boolean test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello");
                        return response.getStatusCodeValue() == 200;
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity;

                class Example {
                    boolean test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello");
                        return response.getStatusCode().value() == 200;
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
    fun `should not change statusCodeValue when not from ResponseEntity in Groovy`() {
        rewriteRun(
            groovy(
                beforeAndAfter = """
                class NotResponseEntity {
                    int statusCodeValue = 200
                }

                class Example {
                    void test() {
                        NotResponseEntity response = new NotResponseEntity()
                        int status = response.statusCodeValue
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should not change getStatusCodeValue() when not from ResponseEntity in Groovy`() {
        rewriteRun(
            groovy(
                beforeAndAfter = """
                class NotResponseEntity {
                    int getStatusCodeValue() { return 200 }
                }

                class Example {
                    void test() {
                        NotResponseEntity response = new NotResponseEntity()
                        int status = response.getStatusCodeValue()
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should handle chained calls with statusCodeValue in Groovy`() {
        rewriteRun(
            groovy(
                before = """
                import org.springframework.http.ResponseEntity

                class Example {
                    boolean test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello")
                        return response.statusCodeValue == 200
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity

                class Example {
                    boolean test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello")
                        return response.statusCode.value() == 200
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should handle chained calls with getStatusCodeValue() in Groovy`() {
        rewriteRun(
            groovy(
                before = """
                import org.springframework.http.ResponseEntity

                class Example {
                    boolean test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello")
                        return response.getStatusCodeValue() == 200
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity

                class Example {
                    boolean test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello")
                        return response.getStatusCode().value() == 200
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

    @Test
    fun `should not change statusCodeValue when not from ResponseEntity in Kotlin`() {
        rewriteRun(
            kotlin(
                beforeAndAfter = """
                class NotResponseEntity {
                    val statusCodeValue: Int = 200
                }

                class Example {
                    fun test() {
                        val response = NotResponseEntity()
                        val status: Int = response.statusCodeValue
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should not change getStatusCodeValue() when not from ResponseEntity in Kotlin`() {
        rewriteRun(
            kotlin(
                beforeAndAfter = """
                class NotResponseEntity {
                    fun getStatusCodeValue(): Int = 200
                }

                class Example {
                    fun test() {
                        val response = NotResponseEntity()
                        val status: Int = response.getStatusCodeValue()
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should handle chained calls with statusCodeValue in Kotlin`() {
        rewriteRun(
            kotlin(
                before = """
                import org.springframework.http.ResponseEntity

                class Example {
                    fun test(): Boolean {
                        val response: ResponseEntity<String> = ResponseEntity.ok("hello")
                        return response.statusCodeValue == 200
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity

                class Example {
                    fun test(): Boolean {
                        val response: ResponseEntity<String> = ResponseEntity.ok("hello")
                        return response.statusCode.value() == 200
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should handle chained calls with getStatusCodeValue() in Kotlin`() {
        rewriteRun(
            kotlin(
                before = """
                import org.springframework.http.ResponseEntity

                class Example {
                    fun test(): Boolean {
                        val response: ResponseEntity<String> = ResponseEntity.ok("hello")
                        return response.getStatusCodeValue() == 200
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity

                class Example {
                    fun test(): Boolean {
                        val response: ResponseEntity<String> = ResponseEntity.ok("hello")
                        return response.getStatusCode().value() == 200
                    }
                }
                """.trimIndent(),
            ),
        )
    }
}
