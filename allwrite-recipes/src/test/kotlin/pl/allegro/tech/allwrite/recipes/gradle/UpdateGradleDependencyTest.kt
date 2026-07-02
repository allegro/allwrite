package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.buildGradleKts
import pl.allegro.tech.allwrite.recipes.toml

class UpdateGradleDependencyTest {

    private fun defaultSpec(spec: RecipeSpec) {
        spec.recipe(recipe()).validateRecipeSerialization(false)
    }

    private fun recipe(
        groupId: String = "com.fasterxml.jackson.module",
        artifactId: String = "jackson-module-afterburner",
        targetVersion: String = "3.1.4",
        sourceVersionPattern: String = "\\d+\\.\\d+\\.\\d+",
    ): UpdateGradleDependency =
        UpdateGradleDependency(
            groupId = groupId,
            artifactId = artifactId,
            targetVersion = targetVersion,
            sourceVersionPattern = sourceVersionPattern,
        )

    abstract inner class BaseSpec : RewriteTest {
        override fun defaults(spec: RecipeSpec) {
            defaultSpec(spec)
        }

        protected fun recipe(
            groupId: String = "com.fasterxml.jackson.module",
            artifactId: String = "jackson-module-afterburner",
            targetVersion: String = "3.1.4",
            sourceVersionPattern: String = "\\d+\\.\\d+\\.\\d+",
        ): UpdateGradleDependency = this@UpdateGradleDependencyTest.recipe(groupId, artifactId, targetVersion, sourceVersionPattern)
    }

    @Nested
    inner class Groovy : BaseSpec() {
        @Test
        fun `should update dependency version in build gradle`() {
            rewriteRun(
                buildGradle(
                    before = """
                    dependencies {
                        implementation "com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2"
                    }
                    """.trimIndent(),
                    after = """
                    dependencies {
                        implementation "com.fasterxml.jackson.module:jackson-module-afterburner:3.1.4"
                    }
                    """.trimIndent(),
                ) { path("build.gradle") },
            )
        }

        @Test
        fun `should update dependency version in build gradle variable`() {
            rewriteRun(
                buildGradle(
                    before = """
                    buildscript {
                        ext {
                            jackson = '2.17.2'
                        }
                    }

                    dependencies {
                        implementation group: 'com.fasterxml.jackson.module', name: 'jackson-module-afterburner', version: "${'$'}{jackson}"
                    }
                    """.trimIndent(),
                    after = """
                    buildscript {
                        ext {
                            jackson = '3.1.4'
                        }
                    }

                    dependencies {
                        implementation group: 'com.fasterxml.jackson.module', name: 'jackson-module-afterburner', version: "${'$'}{jackson}"
                    }
                    """.trimIndent(),
                ) { path("build.gradle") },
            )
        }
    }

    @Nested
    inner class Kotlin : BaseSpec() {
        @Test
        fun `should update dependency version in build gradle kts`() {
            rewriteRun(
                buildGradleKts(
                    before = """
                    dependencies {
                        implementation("com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2")
                    }
                    """.trimIndent(),
                    after = """
                    dependencies {
                        implementation("com.fasterxml.jackson.module:jackson-module-afterburner:3.1.4")
                    }
                    """.trimIndent(),
                ) { path("build.gradle.kts") },
            )
        }

        @Test
        fun `should update dependency version in build gradle kts variable`() {
            rewriteRun(
                buildGradleKts(
                    before = """
                    val jackson = "2.17.2"

                    dependencies {
                        implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version = jackson)
                    }
                    """.trimIndent(),
                    after = """
                    val jackson = "3.1.4"

                    dependencies {
                        implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version = jackson)
                    }
                    """.trimIndent(),
                ) { path("build.gradle.kts") },
            )
        }
    }

    @Nested
    inner class Toml : BaseSpec() {
        @Test
        fun `should update dependency version in toml version ref`() {
            rewriteRun(
                toml(
                    before = """
                    [versions]
                    jackson = "2.17.2"

                    [libraries]
                    jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version.ref = "jackson" }
                    """.trimIndent(),
                    after = """
                    [versions]
                    jackson = "3.1.4"

                    [libraries]
                    jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version.ref = "jackson" }
                    """.trimIndent(),
                ) { path("gradle/libs.versions.toml") },
            )
        }

        @Test
        fun `should update dependency version in toml plain version`() {
            rewriteRun(
                toml(
                    before = """
                    [libraries]
                    jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version = "2.17.2" }
                    """.trimIndent(),
                    after = """
                    [libraries]
                    jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version = "3.1.4" }
                    """.trimIndent(),
                ) { path("gradle/libs.versions.toml") },
            )
        }
    }
}
