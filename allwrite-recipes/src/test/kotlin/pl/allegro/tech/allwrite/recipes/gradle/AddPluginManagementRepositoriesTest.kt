package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.buildGradleKts

class AddPluginManagementRepositoriesTest : RewriteTest {
    private val repositoryUrl = "https://repo.example.com/maven"

    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(AddPluginManagementRepositories(repositoryUrl))
            .validateRecipeSerialization(false)
    }

    @Test
    fun `should add plugin management repositories to Kotlin settings`() {
        rewriteRun(
            buildGradleKts(
                before = """
                    rootProject.name = "example"
                """.trimIndent(),
                after = """
                    pluginManagement {
                        repositories {
                            maven {
                                url = uri("https://repo.example.com/maven")
                            }
                            gradlePluginPortal()
                        }
                    }

                    rootProject.name = "example"
                """.trimIndent(),
            ) { path("settings.gradle.kts") },
        )
    }

    @Test
    fun `should add plugin management repositories to Groovy settings`() {
        rewriteRun(
            buildGradle(
                before = """
                    rootProject.name = "example"
                """.trimIndent(),
                after = """
                    pluginManagement {
                        repositories {
                            maven {
                                url = "https://repo.example.com/maven"
                            }
                            gradlePluginPortal()
                        }
                    }

                    rootProject.name = "example"
                """.trimIndent(),
            ) { path("settings.gradle") },
        )
    }

    @Test
    fun `should add missing repository to an existing plugin management block`() {
        rewriteRun(
            buildGradleKts(
                before = """
                    pluginManagement {
                        repositories {
                            gradlePluginPortal()
                        }
                    }
                """.trimIndent(),
                after = """
                    pluginManagement {
                        repositories {
                            gradlePluginPortal()
                            maven {
                                url = uri("https://repo.example.com/maven")
                            }
                        }
                    }
                """.trimIndent(),
            ) { path("settings.gradle.kts") },
        )
    }

    @Test
    fun `should not duplicate plugin management repositories`() {
        rewriteRun(
            { spec -> spec.expectedCyclesThatMakeChanges(0) },
            buildGradleKts(
                beforeAndAfter = """
                    pluginManagement {
                        repositories {
                            maven {
                                url = uri("https://repo.example.com/maven")
                            }
                            gradlePluginPortal()
                        }
                    }
                """.trimIndent(),
            ) { path("settings.gradle.kts") },
        )
    }
}
