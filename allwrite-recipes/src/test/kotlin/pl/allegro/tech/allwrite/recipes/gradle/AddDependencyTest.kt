package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.buildGradleKts
import pl.allegro.tech.allwrite.recipes.toml

class AddDependencyTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(
            AddGradleDependency(
                configuration = "testRuntimeOnly",
                groupId = "org.junit.platform",
                artifactId = "junit-platform-launcher",
                versionCatalogName = "junit-platform-launcher",
            ),
        )
            .validateRecipeSerialization(false)
    }

    @Nested
    inner class TomlVersionCatalogCases {

        @Test
        fun `should add an entry to version catalog and build manifest when it does not exist`() {
            rewriteRun(
                toml(
                    """
                    [versions]
                    mylib = "9.1.0"
                    
                    [libraries]
                    mylib-starter-dependencies = { group = "com.example.lib", name = "mylib-starter-dependencies", version.ref = "mylib" }
                    mylib-starter-webmvc = { group = "com.example.lib", name = "mylib-starter-webmvc" }
                    
                    [plugins]
                    kotlin = { id = "org.jetbrains.kotlin.jvm", version = "2.1.10" }
                    """.trimIndent(),
                    """
                    [versions]
                    mylib = "9.1.0"
                    
                    [libraries]
                    mylib-starter-dependencies = { group = "com.example.lib", name = "mylib-starter-dependencies", version.ref = "mylib" }
                    mylib-starter-webmvc = { group = "com.example.lib", name = "mylib-starter-webmvc" }
                    junit-platform-launcher = { group = "org.junit.platform", name = "junit-platform-launcher" }
                    
                    [plugins]
                    kotlin = { id = "org.jetbrains.kotlin.jvm", version = "2.1.10" }
                    """.trimIndent(),
                    { path("gradle/libs.versions.toml") },
                ),
                buildGradle(
                    before = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                    }
                    """.trimIndent(),
                    after = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                        testRuntimeOnly(libs.junit.platform.launcher)
                    }
                    """.trimIndent(),
                ) { path("submodule1/build.gradle") },
                buildGradleKts(
                    before = """
                    dependencies {
                        implementation(libs.test)
                    }
                    """.trimIndent(),
                    after = """
                    dependencies {
                        implementation(libs.test)
                        testRuntimeOnly(libs.junit.platform.launcher)
                    }
                    """.trimIndent(),
                ) { path("submodule2/build.gradle.kts") },
            )
        }

        @Test
        fun `should add an entry to build manifest when dependency exists in version catalog already`() {
            rewriteRun(
                toml(
                    beforeAndAfter = """
                [versions]
                mylib = "9.1.0"
                
                [libraries]
                mylib-starter-dependencies = { group = "com.example.lib", name = "mylib-starter-dependencies", version.ref = "mylib" }
                mylib-starter-webmvc = { group = "com.example.lib", name = "mylib-starter-webmvc" }
                junit-launcher = { group = "org.junit.platform", name = "junit-platform-launcher" }
                
                [plugins]
                kotlin = { id = "org.jetbrains.kotlin.jvm", version = "2.1.10" }
                    """.trimIndent(),
                    { path("gradle/libs.versions.toml") },
                ),
                buildGradle(
                    before = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                    }
                    """.trimIndent(),
                    after = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                        testRuntimeOnly(libs.junit.launcher)
                    }
                    """.trimIndent(),
                ) { path("submodule1/build.gradle") },
                buildGradleKts(
                    before = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                    }
                    """.trimIndent(),
                    after = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                        testRuntimeOnly(libs.junit.launcher)
                    }
                    """.trimIndent(),
                ) { path("submodule2/build.gradle.kts") },
            )
        }

        @Test
        fun `should not add an entry to build manifest when dependency exists in version catalog and build manifest, but for a different configuration`() {
            rewriteRun(
                toml(
                    beforeAndAfter = """
                [versions]
                mylib = "9.1.0"
                
                [libraries]
                mylib-starter-dependencies = { group = "com.example.lib", name = "mylib-starter-dependencies", version.ref = "mylib" }
                mylib-starter-webmvc = { group = "com.example.lib", name = "mylib-starter-webmvc" }
                junit-platform-launcher = { group = "org.junit.platform", name = "junit-platform-launcher" }
                
                [plugins]
                kotlin = { id = "org.jetbrains.kotlin.jvm", version = "2.1.10" }
                    """.trimIndent(),
                    { path("gradle/libs.versions.toml") },
                ),
                buildGradle(
                    beforeAndAfter = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                        implementation(libs.junit.platform.launcher)
                    }
                    """.trimIndent(),
                    { path("submodule1/build.gradle") },
                ),
                buildGradle(
                    beforeAndAfter = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                        implementation(libs.junit.platform.launcher)
                    }
                    """.trimIndent(),
                    { path("submodule1/build.gradle") },
                ),
            )
        }

        @Test
        fun `should be noop when module has a reference to version catalog entry already`() {
            rewriteRun(
                toml(
                    beforeAndAfter = """
                [versions]
                mylib = "9.1.0"
                
                [libraries]
                mylib-starter-dependencies = { group = "com.example.lib", name = "mylib-starter-dependencies", version.ref = "mylib" }
                mylib-starter-webmvc = { group = "com.example.lib", name = "mylib-starter-webmvc" }
                junit-platform-launcher = { group = "org.junit.platform", name = "junit-platform-launcher" }
                
                [plugins]
                kotlin = { id = "org.jetbrains.kotlin.jvm", version = "2.1.10" }
                    """.trimIndent(),
                    { path("gradle/libs.versions.toml") },
                ),
                buildGradle(
                    beforeAndAfter = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                        testRuntimeOnly(libs.junit.platform.launcher)
                    }
                    """.trimIndent(),
                    { path("build.gradle") },
                ),
                buildGradleKts(
                    beforeAndAfter = """
                    dependencies {
                        testRuntimeOnly(libs.junit.platform.launcher)
                        implementation("com.test:test:1.2.3")
                    }
                    """.trimIndent(),
                    { path("build.gradle.kts") },
                ),
            )
        }

        @Test
        fun `should be noop when module has an explicit dependency already`() {
            rewriteRun(
                toml(
                    beforeAndAfter = """
                [versions]
                mylib = "9.1.0"
                
                [libraries]
                mylib-starter-dependencies = { group = "com.example.lib", name = "mylib-starter-dependencies", version.ref = "mylib" }
                mylib-starter-webmvc = { group = "com.example.lib", name = "mylib-starter-webmvc" }
                junit-platform-launcher = { group = "org.junit.platform", name = "junit-platform-launcher" }
                
                [plugins]
                kotlin = { id = "org.jetbrains.kotlin.jvm", version = "2.1.10" }
                    """.trimIndent(),
                    { path("gradle/libs.versions.toml") },
                ),
                buildGradle(
                    beforeAndAfter = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                    }
                    """.trimIndent(),
                    { path("build.gradle") },
                ),
                buildGradleKts(
                    beforeAndAfter = """
                    dependencies {
                        testRuntimeOnly(group = "org.junit.platform", name = "junit-platform-launcher")
                        implementation("com.test:test:1.2.3")
                    }
                    """.trimIndent(),
                    { path("build.gradle.kts") },
                ),
            )
        }

        @Test
        fun `should add dependency if dependency block is missing`() {
            rewriteRun(
                toml(
                    beforeAndAfter = """
                [versions]
                mylib = "9.1.0"
                
                [libraries]
                mylib-starter-dependencies = { group = "com.example.lib", name = "mylib-starter-dependencies", version.ref = "mylib" }
                mylib-starter-webmvc = { group = "com.example.lib", name = "mylib-starter-webmvc" }
                junit-platform-launcher = { group = "org.junit.platform", name = "junit-platform-launcher" }
                
                [plugins]
                kotlin = { id = "org.jetbrains.kotlin.jvm", version = "2.1.10" }
                    """.trimIndent(),
                    { path("gradle/libs.versions.toml") },
                ),
                buildGradle(
                    before = "",
                    after = """
                    dependencies {
                        testRuntimeOnly(libs.junit.platform.launcher)
                    }
                    """.trimIndent(),
                    { path("build.gradle") },
                ),
                buildGradleKts(
                    before = "",
                    after = """
                    dependencies {
                        testRuntimeOnly(libs.junit.platform.launcher)
                    }
                    """.trimIndent(),
                    { path("build.gradle.kts") },
                ),
            )
        }
    }

    @Nested
    inner class NoVersionCatalogCases {
        @Test
        fun `should add dependency if not yet exists`() {
            rewriteRun(
                buildGradle(
                    before = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                    }
                    """.trimIndent(),
                    after = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                    }
                    """.trimIndent(),
                ) { path("submodule1/build.gradle") },
                buildGradleKts(
                    before = """
                    dependencies {
                        implementation(libs.test)
                    }
                    """.trimIndent(),
                    after = """
                    dependencies {
                        implementation(libs.test)
                        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                    }
                    """.trimIndent(),
                ) { path("submodule2/build.gradle.kts") },
            )
        }

        @Test
        fun `should not add dependency if exists for another configuration`() {
            rewriteRun(
                buildGradle(
                    beforeAndAfter = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                        implementation("org.junit.platform:junit-platform-launcher")
                    }
                    """.trimIndent(),
                ) { path("submodule1/build.gradle") },
                buildGradleKts(
                    beforeAndAfter = """
                    dependencies {
                        implementation(libs.test)
                        implementation("org.junit.platform:junit-platform-launcher")
                    }
                    """.trimIndent(),
                ) { path("submodule2/build.gradle.kts") },
            )
        }

        @Test
        fun `should be noop when dependency exists`() {
            rewriteRun(
                buildGradle(
                    beforeAndAfter = """
                    dependencies {
                        implementation("com.test:test:1.2.3")
                        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                    }
                    """.trimIndent(),
                ) { path("submodule1/build.gradle") },
                buildGradleKts(
                    beforeAndAfter = """
                    dependencies {
                        implementation(libs.test)
                        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                    }
                    """.trimIndent(),
                ) { path("submodule2/build.gradle.kts") },
            )
        }

        @Test
        fun `should not add dependencies to other dependencies blocks`() {
            rewriteRun(
                buildGradle(
                    before = """
                    buildscript {
                        dependencies {
                            classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.10")
                        }
                    }
                    dependencies {
                        implementation("com.test:test:1.2.3")
                    }
                    subprojects {
                      dependencies {
                      }
                    }
                    allprojects {
                      dependencies {
                        implementation("com.test:test2:1.2.3")
                      }
                    }
                    """.trimIndent(),
                    after = """
                    buildscript {
                        dependencies {
                            classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.10")
                        }
                    }
                    dependencies {
                        implementation("com.test:test:1.2.3")
                        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                    }
                    subprojects {
                      dependencies {
                      }
                    }
                    allprojects {
                      dependencies {
                        implementation("com.test:test2:1.2.3")
                      }
                    }
                    """.trimIndent(),
                ) { path("submodule1/build.gradle") },
                buildGradleKts(
                    before = """
                    buildscript {
                        dependencies {
                            classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.10")
                        }
                    }
                    dependencies {
                        implementation("com.test:test:1.2.3")
                    }
                    subprojects {
                      dependencies {
                      }
                    }
                    allprojects {
                      dependencies {
                        implementation("com.test:test2:1.2.3")
                      }
                    }
                    """.trimIndent(),
                    after = """
                    buildscript {
                        dependencies {
                            classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.1.10")
                        }
                    }
                    dependencies {
                        implementation("com.test:test:1.2.3")
                        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                    }
                    subprojects {
                      dependencies {
                      }
                    }
                    allprojects {
                      dependencies {
                        implementation("com.test:test2:1.2.3")
                      }
                    }
                    """.trimIndent(),
                ) { path("submodule2/build.gradle.kts") },
            )
        }
    }
}
