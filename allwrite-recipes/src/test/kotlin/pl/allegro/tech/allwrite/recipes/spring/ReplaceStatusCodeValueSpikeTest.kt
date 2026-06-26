package pl.allegro.tech.allwrite.recipes.spring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.java.JavaParser
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.api.RecipeSource
import pl.allegro.tech.allwrite.recipes.groovy
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.recipes.kotlin
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipeSource

class ReplaceStatusCodeValueSpikeTest : RewriteTest {

    private val upstreamEnvironment = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.spring")
        .build()

    private lateinit var springBoot4Recipe: Recipe

    @BeforeEach
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<RecipeSource> {
                        FakeRecipeSource(
                            listOf(upstreamEnvironment.activateRecipes("org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0")),
                        )
                    }
                },
            )
        }
        springBoot4Recipe = object : Recipe() {
            override fun getDisplayName() = "SpringBoot4_0 integration test"
            override fun getDescription() = "Runs all recipes from SpringBoot4_0."
            override fun getRecipeList(): List<Recipe> = SpringBoot4_0().recipeList.filterIsInstance<ReplaceStatusCodeValue>()
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    override fun defaults(spec: RecipeSpec) {
        val ctx = InMemoryExecutionContext()

        spec
            .recipe(springBoot4Recipe)
            .parser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "spring-web-6", "spring-core-6", "spring-beans-6"))
            .parser(KotlinParser.builder().classpathFromResources(ctx, "spring-web-6", "spring-core-6", "spring-beans-6"))
            .parser(GroovyParser.builder().classpathFromResource(ctx, "spring-web-6", "spring-core-6", "spring-beans-6"))
    }

    @Test
    fun `SpringBoot4_0 recipe list contains ReplaceStatusCodeValue`() {
        val recipeNames = SpringBoot4_0().recipeList.map { it::class.simpleName }
        assertThat(recipeNames).contains("ReplaceStatusCodeValue")
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
