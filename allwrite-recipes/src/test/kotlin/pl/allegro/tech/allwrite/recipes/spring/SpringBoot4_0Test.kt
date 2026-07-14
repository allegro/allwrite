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
import org.openrewrite.test.TypeValidation
import pl.allegro.tech.allwrite.api.RecipeSource
import pl.allegro.tech.allwrite.recipes.groovy
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.recipes.kotlin
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipeSource

class SpringBoot4_0Test : RewriteTest {

    private val upstreamEnvironment = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.spring")
        .build()

    private lateinit var recipe: Recipe

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
        recipe = object : Recipe() {
            override fun getDisplayName() = "SpringBoot4_0"
            override fun getDescription() = "Runs all recipes from SpringBoot4_0."
            override fun getRecipeList(): List<Recipe> = SpringBoot4_0().recipeList
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    override fun defaults(spec: RecipeSpec) {
        val ctx = InMemoryExecutionContext()
        val classpath = arrayOf("spring-data-commons-3", "spring-data-jpa-3", "spring-data-mongodb-4", "spring-web-6", "spring-core-6")
        spec
            .recipe(recipe)
            .parser(JavaParser.fromJavaVersion().classpathFromResources(ctx, *classpath))
            .parser(KotlinParser.builder().classpathFromResources(ctx, *classpath))
            .parser(GroovyParser.builder().classpathFromResource(ctx, *classpath))
    }

    @Test
    fun `SpringBoot4_0 recipe list contains custom recipes`() {
        val recipeNames = recipe.recipeList.map { it::class.simpleName }
        assertThat(recipeNames).contains("AddNonNullableTypeBoundsToSpringRepositories", "ReplaceStatusCodeValue")
    }

    @Test
    fun `should add Any bounds to Spring Data repository type parameters`() {
        rewriteRun(
            kotlin(
                before = """
                import org.springframework.data.repository.CrudRepository

                interface UserRepository<T, ID> : CrudRepository<T, ID>
                """.trimIndent(),
                after = """
                import org.springframework.data.repository.CrudRepository

                interface UserRepository<T : Any, ID : Any> : CrudRepository<T, ID>
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should replace getStatusCodeValue() with getStatusCode() value() in Java`() {
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
    fun `should replace statusCodeValue property access with statusCode value() in Kotlin`() {
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
    fun `should replace statusCodeValue property access with statusCode value() in Groovy`() {
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
    fun `should relocate Spring Boot web server types`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(ChangeSpringBoot4WebServerTypes())
                    .typeValidationOptions(TypeValidation.builder().identifiers(false).build())
            },
            kotlin(
                before = """
                    package org.springframework.boot.web.embedded.tomcat

                    class TomcatWebServer
                """.trimIndent(),
                after = """
                    package org.springframework.boot.tomcat

                    class TomcatWebServer
                """.trimIndent(),
            ),
            kotlin(
                before = """
                    package org.springframework.boot.web.embedded.jetty

                    class JettyWebServer
                """.trimIndent(),
                after = """
                    package org.springframework.boot.jetty

                    class JettyWebServer
                """.trimIndent(),
            ),
            kotlin(
                before = """
                    package org.springframework.boot.web.servlet.context

                    class ServletWebServerApplicationContext
                """.trimIndent(),
                after = """
                    package org.springframework.boot.web.server.servlet.context

                    class ServletWebServerApplicationContext
                """.trimIndent(),
            ),
            kotlin(
                before = """
                    package org.springframework.boot.web.reactive.context

                    class ReactiveWebServerApplicationContext
                """.trimIndent(),
                after = """
                    package org.springframework.boot.web.server.reactive.context

                    class ReactiveWebServerApplicationContext
                """.trimIndent(),
            ),
            kotlin(
                before = """
                    import org.springframework.boot.web.embedded.tomcat.TomcatWebServer
                    import org.springframework.boot.web.embedded.jetty.JettyWebServer
                    import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
                    import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext

                    class WebServers(
                        val tomcat: TomcatWebServer?,
                        val jetty: JettyWebServer?,
                        val servlet: ServletWebServerApplicationContext?,
                        val reactive: ReactiveWebServerApplicationContext?
                    )
                """.trimIndent(),
                after = """
                    import org.springframework.boot.jetty.JettyWebServer
                    import org.springframework.boot.tomcat.TomcatWebServer
                    import org.springframework.boot.web.server.reactive.context.ReactiveWebServerApplicationContext
                    import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext

                    class WebServers(
                        val tomcat: TomcatWebServer?,
                        val jetty: JettyWebServer?,
                        val servlet: ServletWebServerApplicationContext?,
                        val reactive: ReactiveWebServerApplicationContext?
                    )
                """.trimIndent(),
            ),
        )
    }
}
