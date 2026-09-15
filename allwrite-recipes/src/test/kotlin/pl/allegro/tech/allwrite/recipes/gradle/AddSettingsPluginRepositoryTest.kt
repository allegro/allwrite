package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.buildGradleKts
import pl.allegro.tech.allwrite.recipes.settingsGradle
import pl.allegro.tech.allwrite.recipes.settingsGradleKts

internal class AddSettingsPluginRepositoryTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec
            .recipe(recipe())
            .expectedCyclesThatMakeChanges(1)
            .validateRecipeSerialization(false)
    }

    @Test
    fun `should add a Maven repository to an ungated Groovy settings file`() {
        rewriteRun(
            settingsGradle(
                before = "",
                after = """
                    pluginManagement {
                        repositories {
                            maven {
                                url = "https://repo.example.com"
                            }
                        }
                    }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should add the Gradle Plugin Portal to an ungated Kotlin settings file`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(recipe(type = "gradlePluginPortal", url = null))
                    .validateRecipeSerialization(false)
            },
            settingsGradleKts(
                before = "",
                after = """
                    pluginManagement {
                        repositories {
                            gradlePluginPortal()
                        }
                    }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should add a repository when a gated build file applies the configured plugin`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(recipe(applyToModulesWithPluginId = "application"))
                    .validateRecipeSerialization(false)
            },
            settingsGradleKts(
                before = "",
                after = """
                    pluginManagement {
                        repositories {
                            maven {
                                url = uri("https://repo.example.com")
                            }
                        }
                    }
                """.trimIndent(),
            ),
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        application
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should add a repository when a gated Groovy build file applies the configured plugin`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(recipe(applyToModulesWithPluginId = "application"))
                    .validateRecipeSerialization(false)
            },
            settingsGradle(
                before = "",
                after = """
                    pluginManagement {
                        repositories {
                            maven {
                                url = "https://repo.example.com"
                            }
                        }
                    }
                """.trimIndent(),
            ),
            buildGradle(
                beforeAndAfter = """
                    plugins {
                        id 'application'
                    }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should leave settings unchanged when no build file applies the configured plugin`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(recipe(applyToModulesWithPluginId = "application"))
                    .expectedCyclesThatMakeChanges(0)
                    .validateRecipeSerialization(false)
            },
            settingsGradleKts(
                beforeAndAfter = """
                    rootProject.name = "example"
                """.trimIndent(),
            ),
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        id("java-library")
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should leave settings unchanged when no Groovy build file applies the configured plugin`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(recipe(applyToModulesWithPluginId = "application"))
                    .expectedCyclesThatMakeChanges(0)
                    .validateRecipeSerialization(false)
            },
            settingsGradle(
                beforeAndAfter = """
                    rootProject.name = 'example'
                """.trimIndent(),
            ),
            buildGradle(
                beforeAndAfter = """
                    plugins {
                        id 'java-library'
                    }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should add a repository when any module applies the configured plugin`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(recipe(applyToModulesWithPluginId = "application"))
                    .validateRecipeSerialization(false)
            },
            settingsGradleKts(
                before = "",
                after = """
                    pluginManagement {
                        repositories {
                            maven {
                                url = uri("https://repo.example.com")
                            }
                        }
                    }
                """.trimIndent(),
            ),
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        id("java-library")
                    }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
            buildGradleKts(
                beforeAndAfter = """
                    plugins {
                        application
                    }
                """.trimIndent(),
            ) { path("app/build.gradle.kts") },
        )
    }

    @Test
    fun `should add a repository when any Groovy module applies the configured plugin`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(recipe(applyToModulesWithPluginId = "application"))
                    .validateRecipeSerialization(false)
            },
            settingsGradle(
                before = "",
                after = """
                    pluginManagement {
                        repositories {
                            maven {
                                url = "https://repo.example.com"
                            }
                        }
                    }
                """.trimIndent(),
            ),
            buildGradle(
                beforeAndAfter = """
                    plugins {
                        id 'java-library'
                    }
                """.trimIndent(),
            ) { path("build.gradle") },
            buildGradle(
                beforeAndAfter = """
                    plugins {
                        id 'application'
                    }
                """.trimIndent(),
            ) { path("app/build.gradle") },
        )
    }

    @Test
    fun `should not duplicate an existing repository`() {
        rewriteRun(
            { spec ->
                spec
                    .recipe(recipe())
                    .expectedCyclesThatMakeChanges(0)
                    .validateRecipeSerialization(false)
            },
            settingsGradleKts(
                beforeAndAfter = """
                    pluginManagement {
                        repositories {
                            maven {
                                url = uri("https://repo.example.com")
                            }
                        }
                    }
                """.trimIndent(),
            ),
        )
    }

    private fun recipe(
        type: String = "maven",
        url: String? = "https://repo.example.com",
        applyToModulesWithPluginId: String? = null,
    ): AddSettingsPluginRepository =
        AddSettingsPluginRepository(
            type = type,
            url = url,
            applyToModulesWithPluginId = applyToModulesWithPluginId,
        )
}
