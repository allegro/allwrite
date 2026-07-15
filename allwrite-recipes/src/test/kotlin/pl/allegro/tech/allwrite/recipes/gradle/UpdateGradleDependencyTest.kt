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
        spec.recipe(recipe())
    }

    private fun updateSpockWithCustomSourceVersionPatternRecipe(): UpdateGradleDependency {
        return recipe(
            groupId = "org.spockframework",
            artifactId = "spock-bom",
            targetVersion = "2.4-groovy-5.0",
            sourceVersionPattern = "\\d+\\.\\d+.*",
        )
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

    private fun noChangeSpec(targetVersion: String): RecipeSpec.() -> Unit =
        {
            recipe(recipe(targetVersion = targetVersion))
                .expectedCyclesThatMakeChanges(0)
        }

    private fun groovyDependency(version: String) =
        buildGradle(
            """
            dependencies {
                implementation "com.fasterxml.jackson.module:jackson-module-afterburner:$version"
            }
            """.trimIndent(),
        ) { path("build.gradle") }

    private fun kotlinDependency(version: String) =
        buildGradleKts(
            """
            dependencies {
                implementation("com.fasterxml.jackson.module:jackson-module-afterburner:$version")
            }
            """.trimIndent(),
        ) { path("build.gradle.kts") }

    private fun tomlVersionRef(version: String) =
        toml(
            """
            [versions]
            jackson = "$version"

            [libraries]
            jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version.ref = "jackson" }
            """.trimIndent(),
        ) { path("gradle/libs.versions.toml") }

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
        fun `should trim whitespace around target version in build gradle`() {
            rewriteRun(
                { spec ->
                    spec.recipe(recipe(targetVersion = " 3.1.4 "))
                },
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
        fun `should update dependency version in build gradle with custom sourceVersionPattern`() {
            rewriteRun(
                { spec -> spec.recipe(updateSpockWithCustomSourceVersionPatternRecipe()) },
                buildGradle(
                    before = """
                    dependencies {
                        testImplementation platform(group: 'org.spockframework', name: 'spock-bom', version: '2.4-M4-groovy-4.0')
                    }
                    """.trimIndent(),
                    after = """
                    dependencies {
                        testImplementation platform(group: 'org.spockframework', name: 'spock-bom', version: '2.4-groovy-5.0')
                    }
                    """.trimIndent(),
                ) { path("build.gradle") },
            )
        }

        @Test
        fun `should skip major downgrade in build gradle`() {
            rewriteRun(noChangeSpec("3.9.9"), groovyDependency("4.1.0"))
        }

        @Test
        fun `should skip minor downgrade in build gradle`() {
            rewriteRun(noChangeSpec("4.0.0"), groovyDependency("4.1.0"))
        }

        @Test
        fun `should skip patch downgrade in build gradle`() {
            rewriteRun(noChangeSpec("4.1.0"), groovyDependency("4.1.1"))
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

        @Test
        fun `should update dependency version in build gradle kts with custom sourceVersionPattern`() {
            rewriteRun(
                { spec -> spec.recipe(updateSpockWithCustomSourceVersionPatternRecipe()) },
                buildGradleKts(
                    before = """
                    dependencies {
                        testImplementation(platform(group = 'org.spockframework', name = 'spock-bom', version = '2.4-M4-groovy-4.0'))
                    }
                    """.trimIndent(),
                    after = """
                    dependencies {
                        testImplementation(platform(group = 'org.spockframework', name = 'spock-bom', version = '2.4-groovy-5.0'))
                    }
                    """.trimIndent(),
                ) { path("build.gradle.kts") },
            )
        }

        @Test
        fun `should skip major downgrade in build gradle kts`() {
            rewriteRun(noChangeSpec("3.9.9"), kotlinDependency("4.1.0"))
        }

        @Test
        fun `should skip minor downgrade in build gradle kts`() {
            rewriteRun(noChangeSpec("4.0.0"), kotlinDependency("4.1.0"))
        }

        @Test
        fun `should skip patch downgrade in build gradle kts`() {
            rewriteRun(noChangeSpec("4.1.0"), kotlinDependency("4.1.1"))
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
        fun `should trim whitespace around toml version ref value`() {
            rewriteRun(
                toml(
                    before = """
                    [versions]
                    jackson = " 2.17.2 "

                    [libraries]
                    jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version.ref = "jackson" }
                    """.trimIndent(),
                    after = """
                    [versions]
                    jackson = " 3.1.4"

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

        @Test
        fun `should update dependency version in toml with custom sourceVersionPattern`() {
            rewriteRun(
                { spec -> spec.recipe(updateSpockWithCustomSourceVersionPatternRecipe()) },
                toml(
                    before = """
                    [libraries]
                    spockframework-bom = { group = "org.spockframework", name = "spock-bom", version = "2.4-M4-groovy-4.0" }
                    """.trimIndent(),
                    after = """
                    [libraries]
                    spockframework-bom = { group = "org.spockframework", name = "spock-bom", version = "2.4-groovy-5.0" }
                    """.trimIndent(),
                ) { path("gradle/libs.versions.toml") },
            )
        }

        @Test
        fun `should skip major downgrade in toml version ref`() {
            rewriteRun(noChangeSpec("3.9.9"), tomlVersionRef("4.1.0"))
        }

        @Test
        fun `should skip minor downgrade in toml version ref`() {
            rewriteRun(noChangeSpec("4.0.0"), tomlVersionRef("4.1.0"))
        }

        @Test
        fun `should skip patch downgrade in toml version ref`() {
            rewriteRun(noChangeSpec("4.1.0"), tomlVersionRef("4.1.1"))
        }
    }
}
