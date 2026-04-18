package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.RewriteTest.toRecipe
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility
import pl.allegro.tech.allwrite.recipes.buildGradle
import pl.allegro.tech.allwrite.recipes.buildGradleKts
import java.util.function.Supplier

class AddVersionCatalogDependencyReferenceTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipes(
            toRecipe(
                Supplier {
                    AddVersionCatalogDependencyReference(
                        configuration = "testRuntimeOnly",
                        library = Library("com.example", "test", null),
                        versionCatalogAlias = "com-example-test",
                    )
                },
            ),
        )
    }

    @Test
    fun `should add dependency when it is not present`() {
        rewriteRun(
            buildGradle(
                before = """
                dependencies {
                }
                """.trimIndent(),
                after = """
                dependencies {
                    testRuntimeOnly(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build.gradle") },
            buildGradleKts(
                before = """
                dependencies {
                }
                """.trimIndent(),
                after = """
                dependencies {
                    testRuntimeOnly(libs.com.example.test)
                }
                """.trimIndent(),
                { path("build.gradle.kts") },
            ),
            buildGradle(
                before = """
                plugins {
                    id "groovy"
                    id "application"
                }
                dependencies {
                    implementation("org.google.gson:gson:2.8.6")
                    testImplementation(group = "com.google.code.gson", name = "gson-test", version = "2.8.6")
                }

                java {
                  withSourcesJar()
                }
                """.trimIndent(),
                after = """
                plugins {
                    id "groovy"
                    id "application"
                }
                dependencies {
                    implementation("org.google.gson:gson:2.8.6")
                    testImplementation(group = "com.google.code.gson", name = "gson-test", version = "2.8.6")
                    testRuntimeOnly(libs.com.example.test)
                }

                java {
                  withSourcesJar()
                }
                """.trimIndent(),
            ) { path("build.gradle") },
            buildGradleKts(
                before = """
                plugins {
                  `java-library`
                  `maven-publish`
                }
                dependencies {
                  implementation("org.google.gson:gson:2.8.6")
                  testImplementation(group = "com.google.code.gson", name = "gson-test", version = "2.8.6")
                }

                java {
                  withSourcesJar()
                }
                """.trimIndent(),
                after = """
                plugins {
                  `java-library`
                  `maven-publish`
                }
                dependencies {
                  implementation("org.google.gson:gson:2.8.6")
                  testImplementation(group = "com.google.code.gson", name = "gson-test", version = "2.8.6")
                  testRuntimeOnly(libs.com.example.test)
                }

                java {
                  withSourcesJar()
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should add dependency when dependency block is not present`() {
        rewriteRun(
            buildGradle(
                before = "",
                after = """
                dependencies {
                    testRuntimeOnly(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build.gradle") },
            buildGradleKts(
                before = "",
                after = """
                dependencies {
                    testRuntimeOnly(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build1.gradle.kts") },
            buildGradleKts(
                before = """
                plugins {
                  `java-library`
                  `maven-publish`
                }
                java {
                  withSourcesJar()
                }
                """.trimIndent(),
                after = """
                plugins {
                  `java-library`
                  `maven-publish`
                }
                java {
                  withSourcesJar()
                }
                dependencies {
                    testRuntimeOnly(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build2.gradle.kts") },
        )
    }

    @Test
    fun `should not add dependency to the blocks nested inside dependencies block`() {
        rewriteRun(
            buildGradle(
                beforeAndAfter = """
                dependencies {
                    testRuntimeOnly(libs.another) {
                        exclude('test') {
                           version = '1.0.0'
                        }
                    }
                    testRuntimeOnly(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build.gradle") },
            buildGradleKts(
                beforeAndAfter = """
                dependencies {
                    testRuntimeOnly(libs.another) {
                       exclude('test') {
                           version = "1.0.0"
                       }
                    }
                    testRuntimeOnly(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build1.gradle.kts") },
        )
    }

    @Test
    fun `should not add dependency when it exists for a different configuration`() {
        rewriteRun(
            buildGradle(
                beforeAndAfter = """
                dependencies {
                    implementation(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build.gradle") },
            buildGradleKts(
                beforeAndAfter = """
                dependencies {
                    compileOnly(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should not add dependency when it already exists as a ref to version catalog`() {
        rewriteRun(
            buildGradle(
                beforeAndAfter = """
                dependencies {
                  testRuntimeOnly(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build.gradle") },
            buildGradleKts(
                beforeAndAfter = """
                dependencies {
                  testRuntimeOnly(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should not add dependency when it already exists in string notation`() {
        rewriteRun(
            buildGradle(
                beforeAndAfter = """
                dependencies {
                  testRuntimeOnly "com.example:test:1.0.1"
                }
                """.trimIndent(),
            ) { path("build.gradle") },
            buildGradleKts(
                beforeAndAfter = """
                dependencies {
                  testRuntimeOnly("com.example:test:1.0.1")
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should not add dependency when it already exists in groovy map notation`() {
        rewriteRun(
            buildGradle(
                beforeAndAfter = """
                dependencies {
                  testRuntimeOnly(group: 'com.example', name: 'test', version: '1.0.0')
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should not add dependency when it already exists in kotlin named param notation`() {
        rewriteRun(
            buildGradleKts(
                beforeAndAfter = """
                dependencies {
                  testRuntimeOnly(group = "com.example", name = "test", version = "1.0.0")
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
        )
    }

    @Test
    fun `should not add dependency to buildscript block`() {
        rewriteRun(
            buildGradleKts(
                before = """
                buildscript {
                    dependencies {
                      classpath(libs.phoenix.provisioning)
                    }
                }
                """.trimIndent(),
                after = """
                buildscript {
                    dependencies {
                      classpath(libs.phoenix.provisioning)
                    }
                }
                dependencies {
                    testRuntimeOnly(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build.gradle.kts") },
            buildGradle(
                before = """
                buildscript {
                    dependencies {
                      classpath(libs.phoenix.provisioning)
                    }
                }
                """.trimIndent(),
                after = """
                buildscript {
                    dependencies {
                      classpath(libs.phoenix.provisioning)
                    }
                }
                dependencies {
                    testRuntimeOnly(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    @Test
    fun `should not change non-gradle files`() {
        rewriteRun(
            buildGradle(
                beforeAndAfter = """
                dependencies {
                }
                """.trimIndent(),
            ) { path("build.groovy") },
            buildGradleKts(
                beforeAndAfter = """
                dependencies {
                }
                """.trimIndent(),
            ) { path("build.kt") },
        )
    }

    @Test
    fun `should be applied multiple times correctly`() {
        rewriteRun(
            { spec -> spec.recipe(ListRecipe()) },
            buildGradle(
                before = """
                dependencies {
                }
                """.trimIndent(),
                after = """
                dependencies {
                    testRuntimeOnly(libs.com.example.test)
                    testRuntimeOnly(libs.com.example.two)
                }
                """.trimIndent(),
                { path("build.gradle") },
            ),
            buildGradleKts(
                before = """
                dependencies {
                }
                """.trimIndent(),
                after = """
                dependencies {
                    testRuntimeOnly(libs.com.example.test)
                    testRuntimeOnly(libs.com.example.two)
                }
                """.trimIndent(),
                { path("build.gradle.kts") },
            ),
        )
    }

    @Test
    fun `should copy indent from the previous entry`() {
        rewriteRun(
            buildGradle(
                before = """
                dependencies {
                 testRuntimeOnly("com.example:example")
                }
                """.trimIndent(),
                after = """
                dependencies {
                 testRuntimeOnly("com.example:example")
                 testRuntimeOnly(libs.com.example.test)
                }
                """.trimIndent(),
            ) { path("build.gradle") },
        )
    }

    private class ListRecipe : AllwriteRecipe(null, null, RecipeVisibility.INTERNAL, null, null) {

        override fun getRecipeList(): List<Recipe> =
            listOf(
                toRecipe(
                    Supplier {
                        AddVersionCatalogDependencyReference(
                            configuration = "testRuntimeOnly",
                            library = Library("com.example", "test", null),
                            versionCatalogAlias = "com-example-test",
                        )
                    },
                ),
                toRecipe(
                    Supplier {
                        AddVersionCatalogDependencyReference(
                            configuration = "testRuntimeOnly",
                            library = Library("com.example", "two", null),
                            versionCatalogAlias = "com-example-two",
                        )
                    },
                ),
            )
    }
}
