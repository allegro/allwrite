package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.openrewrite.java.Assertions.srcMainResources
import org.openrewrite.java.JavaParser
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.groovy
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.recipes.kotlin
import pl.allegro.tech.allwrite.recipes.properties
import pl.allegro.tech.allwrite.recipes.text
import pl.allegro.tech.allwrite.recipes.yaml

class ChangeSpringPropertyKeyTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
        spec.parser(KotlinParser.builder().classpath(JavaParser.runtimeClasspath()))
        val recipe = ChangeSpringPropertyKey(OLD_PROPERTY_CAMEL, NEW_PROPERTY)
        spec.recipe(recipe).validateRecipeSerialization(false)
    }

    @Nested
    inner class YamlTests {

        @ParameterizedTest(name = "should change property key in expanded yaml in {0}")
        @PartialPropertyNameCaseConventionSource
        fun `should change property key in expanded yaml`(inputDescription: String, propertyName: String) {
            rewriteRun(
                srcMainResources(
                    yaml(
                        before = """
                            i18n:
                               $propertyName:
                                 enabled: true
                        """.trimIndent(),
                        after = """
                            myapp.i18n.$LANGUAGE_BUNDLE_KEBAB.enabled: true
                        """.trimIndent(),
                        spec = { path("application.yml") },
                    ),
                ),
            )
        }

        @ParameterizedTest(name = "should change property key in collapsed yaml in {0}")
        @PartialPropertyNameCaseConventionSource
        fun `should change property key in collapsed yaml`(inputDescription: String, propertyName: String) {
            rewriteRun(
                srcMainResources(
                    yaml(
                        before = """
                            i18n.$propertyName.enabled: true
                        """.trimIndent(),
                        after = """
                            myapp.i18n.$LANGUAGE_BUNDLE_KEBAB.enabled: true
                        """.trimIndent(),
                        spec = { path("src/main/resources/application.yml") },
                    ),
                ),
            )
        }

        @ParameterizedTest(name = "should ignore already existing property with the same prefix in {0}") // merging is handled by a separate visitor
        @PartialPropertyNameCaseConventionSource
        fun `should ignore already existing property with the same prefix`(inputDescription: String, propertyName: String) {
            rewriteRun(
                srcMainResources(
                    yaml(
                        before = """
                            i18n.$propertyName.enabled: true
                            myapp.i18n.$propertyName.default-locale: cs-CZ
                        """.trimIndent(),
                        after = """
                            myapp.i18n.$LANGUAGE_BUNDLE_KEBAB.enabled: true
                            myapp.i18n.$propertyName.default-locale: cs-CZ
                        """.trimIndent(),
                        spec = { path("src/main/resources/application.yml") },
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
    }

    @Nested
    inner class PropertiesTest {
        @ParameterizedTest(name = "should change property key")
        @FullPropertyNameCaseConventionSource
        fun `should change property key`(inputDescription: String, propertyName: String) {
            rewriteRun(
                srcMainResources(
                    properties(
                        before = """
                            $propertyName = true
                        """.trimIndent(),
                        after = """
                            $NEW_PROPERTY = true
                        """.trimIndent(),
                        spec = { path("src/main/resources/application.properties") },
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
    }

    @Nested
    inner class OtherFilesTests {

        @ParameterizedTest(name = "should change java files with property in {0}")
        @FullPropertyNameCaseConventionSource
        fun `should change java files`(inputDescription: String, propertyName: String) {
            rewriteRun(
                java(
                    before = """
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.TestPropertySource;
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.ConditionalOnProperty;
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.Value;
                        
                        @TestPropertySource(properties = {"some.prop=true", "$propertyName=true"})
                        class SomeTestConfiguration {
                          @Value("${'$'}{$propertyName}")
                          private String value;
                        
                          @ConditionalOnProperty(name = "$propertyName", matchIfMissing = true)
                          public String testBean() { return ""; }
                        }
                    """.trimIndent(),
                    after = """
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.TestPropertySource;
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.ConditionalOnProperty;
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.Value;
                        
                        @TestPropertySource(properties = {"some.prop=true", "$NEW_PROPERTY=true"})
                        class SomeTestConfiguration {
                          @Value("${'$'}{$NEW_PROPERTY}")
                          private String value;
                        
                          @ConditionalOnProperty(name = "$NEW_PROPERTY", matchIfMissing = true)
                          public String testBean() { return ""; }
                        }
                    """.trimIndent(),
                    spec = { path("src/main/java/Example.java") },
                ),
            )
        }

        @ParameterizedTest(name = "should change groovy files with property in {0}")
        @FullPropertyNameCaseConventionSource
        fun `should change groovy files`(inputDescription: String, propertyName: String) {
            rewriteRun(
                groovy(
                    before = """
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.TestPropertySource
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.ConditionalOnProperty
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.Value
                        
                        @TestPropertySource(properties = ["some.prop=true", "$propertyName=true"])
                        class SomeTestConfiguration {
                          @Value('${'$'}{$propertyName}')
                          private String value
                        
                          @ConditionalOnProperty(name = '$propertyName', matchIfMissing = true)
                          public Map testBean() { return ['$propertyName': "$propertyName"] }
                        }
                    """.trimIndent(),
                    after = """
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.TestPropertySource
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.ConditionalOnProperty
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.Value
                        
                        @TestPropertySource(properties = ["some.prop=true", "$NEW_PROPERTY=true"])
                        class SomeTestConfiguration {
                          @Value('${'$'}{$NEW_PROPERTY}')
                          private String value
                        
                          @ConditionalOnProperty(name = '$NEW_PROPERTY', matchIfMissing = true)
                          public Map testBean() { return ['$NEW_PROPERTY': "$NEW_PROPERTY"] }
                        }
                    """.trimIndent(),
                    spec = { path("src/integration/groovy/Example.groovy") },
                ),
            )
        }

        @ParameterizedTest(name = "should change kotlin files with property in {0}")
        @FullPropertyNameCaseConventionSource
        fun `should change kotlin files`(inputDescription: String, propertyName: String) {
            rewriteRun(
                kotlin(
                    before = """
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.TestPropertySource
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.ConditionalOnProperty
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.Value
                        
                        @TestPropertySource(properties = ["some.prop=true", "$propertyName=true"])
                        class SomeTestConfiguration {
                          @Value("${'\$'}{$propertyName}")
                          private val value : String;
                        
                          @ConditionalOnProperty(name = "$propertyName", matchIfMissing = true)
                          public fun testBean(): Map<String, String> = mapOf("$propertyName", "$propertyName")
                        }
                    """.trimIndent(),
                    after = """
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.TestPropertySource
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.ConditionalOnProperty
                        import pl.allegro.tech.allwrite.recipes.spring.ChangeSpringPropertyKeyTest.Value
                        
                        @TestPropertySource(properties = ["some.prop=true", "$NEW_PROPERTY=true"])
                        class SomeTestConfiguration {
                          @Value("${'\$'}{$NEW_PROPERTY}")
                          private val value : String;
                        
                          @ConditionalOnProperty(name = "$NEW_PROPERTY", matchIfMissing = true)
                          public fun testBean(): Map<String, String> = mapOf("$NEW_PROPERTY", "$NEW_PROPERTY")
                        }
                    """.trimIndent(),
                    spec = { path("src/main/kotlin/Example.kt") },
                ),
            )
        }

        @ParameterizedTest(name = "should change text files with property in {0}")
        @FullPropertyNameCaseConventionSource
        fun `should change text files`(inputDescription: String, propertyName: String) {
            rewriteRun(
                text(
                    before = """
                        ## Description
                        We are using `$propertyName` to change myapp's behaviour.
                    """.trimIndent(),
                    after = """
                        ## Description
                        We are using `$NEW_PROPERTY` to change myapp's behaviour.
                    """.trimIndent(),
                    spec = { path("example.md") },
                ),
            )
        }

        @ParameterizedTest(name = "should change text files with property in {0}")
        @FullPropertyNameCaseConventionSource
        fun `should not interpret dot in property name as any symbol`(inputDescription: String, propertyName: String) {
            val nonMatchingPropertyName = propertyName.replace('.', 'a')
            rewriteRun(
                text(
                    beforeAndAfter = """
                        ## Description
                        We are using `$nonMatchingPropertyName` to change myapp's behaviour.
                    """.trimIndent(),
                    spec = { path("example.md") },
                ),
            )
        }
    }

    companion object {
        const val LANGUAGE_BUNDLE_CAMEL = "languageBundle"
        const val LANGUAGE_BUNDLE_KEBAB = "language-bundle"
        const val OLD_PROPERTY_CAMEL = "i18n.$LANGUAGE_BUNDLE_CAMEL.enabled"
        const val OLD_PROPERTY_KEBAB = "i18n.$LANGUAGE_BUNDLE_KEBAB.enabled"
        const val NEW_PROPERTY = "myapp.i18n.$LANGUAGE_BUNDLE_KEBAB.enabled"
    }

    @CsvSource(
        delimiterString = "->",
        textBlock = """
        kebab case -> $OLD_PROPERTY_KEBAB
        camel case -> $OLD_PROPERTY_CAMEL""",
    )
    annotation class FullPropertyNameCaseConventionSource

    @CsvSource(
        delimiterString = "->",
        textBlock = """
        kebab case -> $LANGUAGE_BUNDLE_KEBAB
        camel case -> $LANGUAGE_BUNDLE_CAMEL""",
    )
    annotation class PartialPropertyNameCaseConventionSource

    annotation class TestPropertySource(
        val properties: Array<String>,
    )
    annotation class Value(
        val value: String,
    )
    annotation class ConditionalOnProperty(
        val name: String,
        val matchIfMissing: Boolean,
    )
}
