package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.buildGradleKts
import pl.allegro.tech.allwrite.recipes.toml

class ChangeGradleDependencyTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(recipe()).validateRecipeSerialization(false)
    }

    private fun recipe(newVersion: String? = "3.1.4"): ChangeGradleDependency =
        ChangeGradleDependency(
            oldGroupId = "com.fasterxml.jackson.module",
            oldArtifactId = "jackson-module-afterburner",
            newGroupId = "tools.jackson.module",
            newArtifactId = "jackson-module-blackbird",
            newVersion = newVersion,
        )

    @Test
    fun `should change dependency in build gradle`() {
        rewriteRun(
            buildGradle(
                before = """
                dependencies {
                    implementation "com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2"
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation "tools.jackson.module:jackson-module-blackbird:3.1.4"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should change dependency in build gradle kts`() {
        rewriteRun(
            buildGradleKts(
                before = """
                dependencies {
                    implementation("com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2")
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation("tools.jackson.module:jackson-module-blackbird:3.1.4")
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should drop version when new version is null in build gradle`() {
        rewriteRun(
            { spec ->
                spec.recipe(recipe(newVersion = null)).validateRecipeSerialization(false)
            },
            buildGradle(
                before = """
                dependencies {
                    implementation "com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2"
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation "tools.jackson.module:jackson-module-blackbird"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should drop version when new version is null in build gradle kts`() {
        rewriteRun(
            { spec ->
                spec.recipe(recipe(newVersion = null)).validateRecipeSerialization(false)
            },
            buildGradleKts(
                before = """
                dependencies {
                    implementation("com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2")
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation("tools.jackson.module:jackson-module-blackbird")
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should change versionless dependency in build gradle`() {
        rewriteRun(
            buildGradle(
                before = """
                dependencies {
                    implementation "com.fasterxml.jackson.module:jackson-module-afterburner"
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation "tools.jackson.module:jackson-module-blackbird"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should change versionless map dependency in build gradle`() {
        rewriteRun(
            buildGradle(
                before = """
                dependencies {
                    implementation group: 'com.fasterxml.jackson.module', name: 'jackson-module-afterburner'
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation group: 'tools.jackson.module', name: 'jackson-module-blackbird'
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should change versionless dependency in build gradle kts`() {
        rewriteRun(
            buildGradleKts(
                before = """
                dependencies {
                    implementation("com.fasterxml.jackson.module:jackson-module-afterburner")
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation("tools.jackson.module:jackson-module-blackbird")
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should change dependency with version expression in build gradle`() {
        rewriteRun(
            buildGradle(
                before = """
                dependencies {
                    implementation group: 'com.fasterxml.jackson.module', name: 'jackson-module-afterburner', version: versions.kotlin
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation group: 'tools.jackson.module', name: 'jackson-module-blackbird', version: "3.1.4"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should change quoted dependency version in build gradle`() {
        rewriteRun(
            buildGradle(
                before = """
                dependencies {
                    implementation group: 'com.fasterxml.jackson.module', name: 'jackson-module-afterburner', version: '2.17.2'
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation group: 'tools.jackson.module', name: 'jackson-module-blackbird', version: '3.1.4'
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should change double quoted dependency version in build gradle`() {
        rewriteRun(
            buildGradle(
                before = """
                dependencies {
                    implementation group: 'com.fasterxml.jackson.module', name: 'jackson-module-afterburner', version: "2.17.2"
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation group: 'tools.jackson.module', name: 'jackson-module-blackbird', version: "3.1.4"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should change dependency with version expression in build gradle kts`() {
        rewriteRun(
            buildGradleKts(
                before = """
                dependencies {
                    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version = versions.kotlin)
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation(group = "tools.jackson.module", name = "jackson-module-blackbird", version = "3.1.4")
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should drop version from version expression in build gradle kts`() {
        rewriteRun(
            { spec ->
                spec.recipe(recipe(newVersion = null)).validateRecipeSerialization(false)
            },
            buildGradleKts(
                before = """
                dependencies {
                    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version = versions.kotlin)
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation(group = "tools.jackson.module", name = "jackson-module-blackbird")
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should quote hyphenated version in build gradle`() {
        rewriteRun(
            { spec ->
                spec.recipe(recipe(newVersion = "3-SNAPSHOT")).validateRecipeSerialization(false)
            },
            buildGradle(
                before = """
                dependencies {
                    implementation group: 'com.fasterxml.jackson.module', name: 'jackson-module-afterburner', version: versions.kotlin
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation group: 'tools.jackson.module', name: 'jackson-module-blackbird', version: "3-SNAPSHOT"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should change dependency with interpolated version in build gradle`() {
        rewriteRun(
            buildGradle(
                before = """
                buildscript {
                    ext {
                        jackson = '2.18.3'
                    }
                }

                dependencies {
                    implementation group: 'com.fasterxml.jackson.module', name: 'jackson-module-afterburner', version: "${'$'}{jackson}"
                }
                """.trimIndent(),
                after = """
                buildscript {
                    ext {
                        jackson = '2.18.3'
                        jackson_module_blackbird = '3.1.4'
                    }
                }

                dependencies {
                    implementation group: 'tools.jackson.module', name: 'jackson-module-blackbird', version: "${'$'}{jackson_module_blackbird}"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should change all matching interpolated dependencies in build gradle`() {
        rewriteRun(
            buildGradle(
                before = """
                buildscript {
                    ext {
                        jackson = '2.18.3'
                    }
                }

                dependencies {
                    implementation group: 'com.fasterxml.jackson.module', name: 'jackson-module-afterburner', version: "${'$'}{jackson}"
                    testImplementation group: 'com.fasterxml.jackson.module', name: 'jackson-module-afterburner', version: "${'$'}{jackson}"
                }
                """.trimIndent(),
                after = """
                buildscript {
                    ext {
                        jackson = '2.18.3'
                        jackson_module_blackbird = '3.1.4'
                    }
                }

                dependencies {
                    implementation group: 'tools.jackson.module', name: 'jackson-module-blackbird', version: "${'$'}{jackson_module_blackbird}"
                    testImplementation group: 'tools.jackson.module', name: 'jackson-module-blackbird', version: "${'$'}{jackson_module_blackbird}"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should drop version from interpolated dependency in build gradle`() {
        rewriteRun(
            { spec ->
                spec.recipe(recipe(newVersion = null)).validateRecipeSerialization(false)
            },
            buildGradle(
                before = """
                buildscript {
                    ext {
                        jackson = '2.18.3'
                    }
                }

                dependencies {
                    implementation group: 'com.fasterxml.jackson.module', name: 'jackson-module-afterburner', version: "${'$'}{jackson}"
                }
                """.trimIndent(),
                after = """
                buildscript {
                    ext {
                        jackson = '2.18.3'
                    }
                }

                dependencies {
                    implementation group: 'tools.jackson.module', name: 'jackson-module-blackbird'
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should change all matching dependencies in build gradle`() {
        rewriteRun(
            buildGradle(
                before = """
                dependencies {
                    implementation "com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2"
                    testImplementation "com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2"
                }
                """.trimIndent(),
                after = """
                dependencies {
                    implementation "tools.jackson.module:jackson-module-blackbird:3.1.4"
                    testImplementation "tools.jackson.module:jackson-module-blackbird:3.1.4"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should keep the rest of build gradle unchanged`() {
        rewriteRun(
            buildGradle(
                before = """
                plugins {
                    id 'java'
                }

                ext {
                    jackson = '2.18.3'
                }

                dependencies {
                    implementation "com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2"
                    testImplementation "org.junit.jupiter:junit-jupiter:5.10.0"
                }
                """.trimIndent(),
                after = """
                plugins {
                    id 'java'
                }

                ext {
                    jackson = '2.18.3'
                }

                dependencies {
                    implementation "tools.jackson.module:jackson-module-blackbird:3.1.4"
                    testImplementation "org.junit.jupiter:junit-jupiter:5.10.0"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should keep the rest of build gradle kts unchanged`() {
        rewriteRun(
            buildGradleKts(
                before = """
                plugins {
                    id("java")
                }

                extra["jackson"] = "2.18.3"

                dependencies {
                    implementation("com.fasterxml.jackson.module:jackson-module-afterburner:2.17.2")
                    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
                }
                """.trimIndent(),
                after = """
                plugins {
                    id("java")
                }

                extra["jackson"] = "2.18.3"

                dependencies {
                    implementation("tools.jackson.module:jackson-module-blackbird:3.1.4")
                    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should keep the rest of toml unchanged`() {
        rewriteRun(
            toml(
                before = """
                [versions]
                jackson = "2.17.2"
                junit = "5.10.0"

                [plugins]
                ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "junit" }

                [libraries]
                jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version.ref = "jackson" }
                junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit" }
                """.trimIndent(),
                after = """
                [versions]
                jackson-module-blackbird = "3.1.4"
                junit = "5.10.0"

                [plugins]
                ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "junit" }

                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version.ref = "jackson-module-blackbird" }
                junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should change dependency in toml version value`() {
        rewriteRun(
            toml(
                before = """
                [libraries]
                jackson-module-blackbird = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version = "2.17.2" }
                """.trimIndent(),
                after = """
                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version = "3.1.4" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should change dependency in toml module value`() {
        rewriteRun(
            toml(
                before = """
                [libraries]
                jackson-module-blackbird = { module = "com.fasterxml.jackson.module:jackson-module-afterburner", version = "2.17.2" }
                """.trimIndent(),
                after = """
                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version = "3.1.4" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should not change dependency when target already exists in toml`() {
        rewriteRun(
            toml(
                """
                [libraries]
                jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version = "2.17.2" }
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version = "3.1.4" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should keep versionless dependency versionless in toml`() {
        rewriteRun(
            toml(
                before = """
                [libraries]
                jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner" }
                """.trimIndent(),
                after = """
                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should change dependency in toml version ref`() {
        rewriteRun(
            toml(
                before = """
                [versions]
                jackson-module-afterburner = "2.17.2"

                [libraries]
                jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version.ref = "jackson-module-afterburner" }
                """.trimIndent(),
                after = """
                [versions]
                jackson-module-blackbird = "3.1.4"

                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version.ref = "jackson-module-blackbird" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should split shared version ref in toml`() {
        rewriteRun(
            toml(
                before = """
                [versions]
                jackson = "2.17.2"

                [libraries]
                jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version.ref = "jackson" }
                jackson-module-kotlin = { group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version.ref = "jackson" }
                """.trimIndent(),
                after = """
                [versions]
                jackson = "2.17.2"
                jackson-module-blackbird = "3.1.4"

                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version.ref = "jackson-module-blackbird" }
                jackson-module-kotlin = { group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version.ref = "jackson" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should update existing version entry in toml`() {
        rewriteRun(
            toml(
                before = """
                [versions]
                jackson = "2.17.2"
                jackson-module-blackbird = "1.0.0"

                [libraries]
                jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version.ref = "jackson" }
                jackson-module-kotlin = { group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version.ref = "jackson" }
                """.trimIndent(),
                after = """
                [versions]
                jackson = "2.17.2"
                jackson-module-blackbird = "3.1.4"

                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version.ref = "jackson-module-blackbird" }
                jackson-module-kotlin = { group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version.ref = "jackson" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should not change non matching dependency in build gradle`() {
        rewriteRun(
            buildGradle(
                """
                dependencies {
                    implementation "org.junit.jupiter:junit-jupiter:5.10.0"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should not change non matching dependency in toml`() {
        rewriteRun(
            toml(
                """
                [libraries]
                junit = { group = "org.junit.jupiter", name = "junit-jupiter", version = "5.10.0" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should rename only targeted version refs in toml`() {
        rewriteRun(
            toml(
                before = """
                [versions]
                jackson-module-afterburner = "2.17.2"
                jackson-bom = "2.17.2"

                [libraries]
                jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version.ref = "jackson-module-afterburner" }
                jackson-bom = { group = "com.fasterxml.jackson", name = "jackson-bom", version.ref = "jackson-bom" }
                """.trimIndent(),
                after = """
                [versions]
                jackson-module-blackbird = "3.1.4"
                jackson-bom = "2.17.2"

                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version.ref = "jackson-module-blackbird" }
                jackson-bom = { group = "com.fasterxml.jackson", name = "jackson-bom", version.ref = "jackson-bom" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should drop version when new version is null in toml`() {
        rewriteRun(
            { spec ->
                spec.recipe(recipe(newVersion = null)).validateRecipeSerialization(false)
            },
            toml(
                before = """
                [libraries]
                jackson-module-blackbird = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version = "2.17.2" }
                """.trimIndent(),
                after = """
                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should rename plugin version ref in toml`() {
        rewriteRun(
            toml(
                before = """
                [versions]
                jackson-module-afterburner = "2.17.2"

                [plugins]
                some-plugin = { id = "com.example.plugin", version.ref = "jackson-module-afterburner" }
                """.trimIndent(),
                after = """
                [versions]
                jackson-module-blackbird = "3.1.4"

                [plugins]
                some-plugin = { id = "com.example.plugin", version.ref = "jackson-module-blackbird" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `should split shared plugin version ref in toml`() {
        rewriteRun(
            toml(
                before = """
                [versions]
                jackson-module-afterburner = "2.17.2"

                [plugins]
                some-plugin = { id = "com.example.plugin", version.ref = "jackson-module-afterburner" }

                [libraries]
                jackson-module-afterburner = { group = "com.fasterxml.jackson.module", name = "jackson-module-afterburner", version.ref = "jackson-module-afterburner" }
                jackson-module-kotlin = { group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version.ref = "jackson-module-afterburner" }
                """.trimIndent(),
                after = """
                [versions]
                jackson-module-afterburner = "2.17.2"
                jackson-module-blackbird = "3.1.4"

                [plugins]
                some-plugin = { id = "com.example.plugin", version.ref = "jackson-module-blackbird" }

                [libraries]
                jackson-module-blackbird = { group = "tools.jackson.module", name = "jackson-module-blackbird", version.ref = "jackson-module-blackbird" }
                jackson-module-kotlin = { group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version.ref = "jackson-module-afterburner" }
                """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }
}
