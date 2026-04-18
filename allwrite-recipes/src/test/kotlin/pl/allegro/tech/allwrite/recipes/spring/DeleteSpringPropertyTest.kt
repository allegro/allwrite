package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.openrewrite.java.Assertions.srcMainResources
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.properties
import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.PartialPropertyNameCaseConventionSource
import pl.allegro.tech.allwrite.recipes.yaml
import java.util.function.Consumer

class DeleteSpringPropertyTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        val recipe = DeleteSpringProperty(REMOVED_PROPERTY)
        spec.recipes(recipe).validateRecipeSerialization(false)
    }

    @Nested
    inner class YamlTests {
        @ParameterizedTest(name = "should delete property key in expanded yaml in {0}")
        @PartialPropertyNameCaseConventionSource
        fun `should delete property key in expanded yaml`(inputDescription: String, propertyName: String) {
            rewriteRun(
                srcMainResources(
                    yaml(
                        before = """
                            myapp:
                              isolated-environment:
                                nested-object:
                                  scalar: 123
                                  list:
                                  - a
                                  - b
                              test:
                                123
                        """.trimIndent(),
                        after = """
                            myapp:
                              test:
                                123
                        """.trimIndent(),
                        spec = { path("application.yml") },
                    ),
                ),
            )
        }

        @ParameterizedTest(name = "should delete property key in collapsed yaml in {0}")
        @PartialPropertyNameCaseConventionSource
        fun `should delete property key in collapsed yaml`(inputDescription: String, propertyName: String) {
            rewriteRun(
                srcMainResources(
                    yaml(
                        before = """
                            myapp.isolated-environment: vte666
                        """.trimIndent(),
                        after = "",
                        spec = { path("application.yml") },
                    ),
                ),
            )
        }

        @Test
        fun `should be noop when target property does not exist`() {
            rewriteRun(
                srcMainResources(
                    yaml(
                        beforeAndAfter = "myapp.i18n.language-bundle.default-locale: cs-CZ",
                        spec = { path("src/main/resources/application.yml") },
                    ),
                ),
            )
        }

        @Test
        fun `should be noop when file name is unsupported`() {
            rewriteRun(
                srcMainResources(
                    yaml(
                        beforeAndAfter = "myapp.isolated-environment: vte666",
                        spec = { path("src/main/resources/tycho.yml") },
                    ),
                ),
            )
        }
    }

    @Nested
    inner class PropertiesTest {
        @ParameterizedTest(name = "should delete property key in properties file in {0}")
        @PartialPropertyNameCaseConventionSource
        fun `should delete property key in properties file`(inputDescription: String, propertyName: String) {
            rewriteRun(
                srcMainResources(
                    properties(
                        before = """
                            myapp.isolated-environment: vte666
                            myapp.isolated-environment.nested-object.scalar = 123
                            myapp.isolated-environment.nested-object.list = a,b
                            myapp.test = 123
                        """.trimIndent(),
                        after = """
                            myapp.isolated-environment.nested-object.scalar = 123
                            myapp.isolated-environment.nested-object.list = a,b
                            myapp.test = 123
                        """.trimIndent(),
                        spec = { path("application.properties") },
                    ),
                ),
            )
        }

        @ParameterizedTest(name = "should delete property key with asterisk in properties file in {0}")
        @PartialPropertyNameCaseConventionSource
        fun `should delete property key with asterisk in properties file`(inputDescription: String, propertyName: String) {
            rewriteRun(
                Consumer<RecipeSpec> { spec -> spec.recipe(DeleteSpringProperty(REMOVED_PROPERTY_WITH_ASTERISK)) },
                srcMainResources(
                    properties(
                        before = """
                            myapp.isolated-environment: vte666
                            myapp.isolated-environment.nested-object.scalar = 123
                            myapp.isolated-environment.nested-object.list = a,b
                            myapp.test = 123
                        """.trimIndent(),
                        after = """
                            myapp.isolated-environment: vte666
                            myapp.test = 123
                        """.trimIndent(),
                        spec = { path("application.properties") },
                    ),
                ),
            )
        }

        @Test
        fun `should be noop when target property does not exist`() {
            rewriteRun(
                srcMainResources(
                    properties(
                        beforeAndAfter = "myapp.i18n.language-bundle.default-locale: cs-CZ",
                        spec = { path("src/main/resources/application.properties") },
                    ),
                ),
            )
        }

        @Test
        fun `should be noop when file name is unsupported`() {
            rewriteRun(
                srcMainResources(
                    properties(
                        beforeAndAfter = "myapp.isolated-environment: vte666",
                        spec = { path("src/main/resources/tycho.properties") },
                    ),
                ),
            )
        }
    }

    companion object {
        const val REMOVED_PROPERTY = "myapp.isolated-environment"
        const val REMOVED_PROPERTY_WITH_ASTERISK = "myapp.isolated-environment.*"
    }
}
