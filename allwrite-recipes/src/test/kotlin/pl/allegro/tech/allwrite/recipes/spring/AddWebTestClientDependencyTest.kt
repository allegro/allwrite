package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.recipes.kotlin
import pl.allegro.tech.allwrite.recipes.toml
import pl.allegro.tech.allwrite.runtime.util.withRecipeClasspath

class AddWebTestClientDependencyTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(AddWebTestClientDependency())
            .withRecipeClasspath()
    }

    @Test
    fun `should add web test client dependency`() {
        // given
        rewriteRun(
            java(
                beforeAndAfter = """
                    import org.springframework.test.web.reactive.server.WebTestClient;

                    class Example {
                        WebTestClient webTestClient;
                    }
                """.trimIndent(),
            ) { path("src/test/java/Example.java") },
            toml(
                before = """
                    [versions]
                    spring = "3.3.0"

                    [libraries]
                    spring-core = { module = "org.springframework:spring-core", version.ref = "spring" }
                """.trimIndent(),
                after = """
                    [versions]
                    spring = "3.3.0"

                    [libraries]
                    spring-core = { module = "org.springframework:spring-core", version.ref = "spring" }
                    spring-boot-webtestclient = { group = "org.springframework.boot", name = "spring-boot-webtestclient" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            buildGradle(
                before = "dependencies {\n}\n",
                after = "dependencies {\n    testImplementation(libs.spring.boot.webtestclient)\n}\n",
            ),
        )
    }

    @Test
    fun `should not add dependency without usage`() {
        // given
        rewriteRun(
            toml(
                beforeAndAfter = """
                    [libraries]
                    existing = { module = "com.example:existing" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
            java(beforeAndAfter = "class Example {}") { path("src/main/java/Example.java") },
        )
    }

    @Test
    fun `should add web test client dependency for Kotlin extension`() {
        // given
        rewriteRun(
            kotlin(
                beforeAndAfter = """
                    import org.springframework.test.web.reactive.server.WebTestClient

                    fun WebTestClient.RequestBodySpec.requestId(): WebTestClient.RequestBodySpec = this
                """.trimIndent(),
            ) { path("src/test/kotlin/WebTestClientExtensions.kt") },
            buildGradle(
                before = "dependencies {\n}\n",
                after = """dependencies {
                    |    testImplementation("org.springframework.boot:spring-boot-webtestclient")
                    |}
                """.trimMargin(),
            ),
        )
    }

    @Test
    fun `should not duplicate dependency`() {
        // given
        rewriteRun(
            java(
                beforeAndAfter = """
                    import org.springframework.test.web.reactive.server.WebTestClient;

                    class Example {
                        WebTestClient webTestClient;
                    }
                """.trimIndent(),
            ) { path("src/test/java/Example.java") },
            toml(
                beforeAndAfter = """
                    [libraries]
                    spring-boot-webtestclient = { module = "org.springframework.boot:spring-boot-webtestclient" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }
}
