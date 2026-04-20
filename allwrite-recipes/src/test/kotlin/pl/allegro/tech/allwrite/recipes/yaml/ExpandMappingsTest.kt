package pl.allegro.tech.allwrite.recipes.yaml

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.TypeValidation
import pl.allegro.tech.allwrite.recipes.yaml

class ExpandMappingsTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        val recipe = ExpandMappings()
        spec.recipe(recipe).typeValidationOptions(
            TypeValidation.builder()
                .cursorAcyclic(false)
                .build(),
        )
    }

    @Test
    fun `should expand collapsed properties`() {
        rewriteRun(
            yaml(
                before = """
                    myapp.metrics.graphite.enabled: true
                    logging.level.root: DEBUG
                """.trimIndent(),
                after = """
                    
                    
                    myapp:
                      metrics:
                        graphite:
                          enabled: true
                    logging:
                      level:
                        root: DEBUG
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should expand collapsed properties and merge paths`() {
        rewriteRun(
            yaml(
                before = """
                    myapp.test: 1010
                    myapp.metrics.graphite.enabled: true
                    myapp.metrics.graphite.host: localhost
                """.trimIndent(),
                after = """
                    
                    
                    myapp:
                      test: 1010
                      metrics:
                        graphite:
                          enabled: true
                          host: localhost
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should expand and merge multiple independent mappings`() {
        rewriteRun(
            yaml(
                before = """
                    logging.level.root: DEBUG
                    myapp.metrics.graphite.enabled: true
                    logging.level.com.example: WARN
                    example-one.value: 1
                    myapp.metrics.graphite.host: localhost
                    example-two.value: 2
                """.trimIndent(),
                after = """
                    
                    
                    logging:
                      level:
                        root: DEBUG
                        com:
                          example: WARN
                    myapp:
                      metrics:
                        graphite:
                          enabled: true
                          host: localhost
                    example-one:
                      value: 1
                    example-two:
                      value: 2
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should be noop for already expanded properties`() {
        rewriteRun(
            yaml(
                beforeAndAfter = """
                    myapp:
                      metrics:
                        graphite:
                          enabled: true
                          host: localhost
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should preserve comments during merge`() {
        rewriteRun(
            yaml(
                before = """
                    # top level comment in the top
                    myapp:
                      # comment
                      metrics:
                        graphite:
                          enabled: true
                    # collapsed comment
                    myapp.metrics.graphite.host: localhost
                    # top level comment in the middle
                    myapp:
                      # another comment
                      metrics:
                        graphite:
                          port: 1111
                """.trimIndent(),
                after = """
                    # top level comment in the top
                    
                    # top level comment in the middle
                    myapp:
                      # comment
                      # another comment
                      metrics:
                        graphite:
                          enabled: true
                          # collapsed comment
                          host: localhost
                          port: 1111
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    @Disabled
    fun `should keep properties as 2 different when scalar is mixed with non-scalar`() {
        rewriteRun(
            yaml(
                before = """
                    logging.level.com.example: DEBUG
                    logging.level.com.example.impl: WARN
                """.trimIndent(),
                after = """
                    logging:
                      level:
                        com:
                          example: DEBUG
                          example.impl: WARN
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should expand keys for block sequences`() {
        rewriteRun(
            yaml(
                before = """
                        myapp.sequence:
                          - 1
                          - 2
                """.trimIndent(),
                after = """
                        
                        
                        myapp:
                          sequence:
                            - 1
                            - 2
                """.trimIndent(),
                spec = {
                    path("src/main/resources/application.yml")
                },
            ),
        )
    }

    @Test
    fun `should not reformat flow sequences`() {
        rewriteRun(
            yaml(
                before = """
                        myapp.sequence: [1, 2]
                        another: [4,
                          5]
                """.trimIndent(),
                after = """
                        
                        
                        myapp:
                          sequence: [1, 2]
                        another: [4,
                          5]
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should support line breaks before block sequence entries`() {
        rewriteRun(
            yaml(
                before = """
                        myapp.sequence:
                          -
                            field1: 1
                            field2: 2
                """.trimIndent(),
                after = """
                        
                        
                        myapp:
                          sequence:
                            -
                              field1: 1
                              field2: 2
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Nested
    inner class FilterTests : RewriteTest {

        override fun defaults(spec: RecipeSpec) {
            val recipe = ExpandMappings()
            recipe.prefix = "myapp"
            recipe.excludes = listOf("myapp.ignored", "logging")
            spec.recipe(recipe).typeValidationOptions(
                TypeValidation.builder()
                    .cursorAcyclic(false)
                    .build(),
            )
        }

        @Test
        fun `should expand collapsed properties matching filter`() {
            rewriteRun(
                yaml(
                    before = """
                        myapp.metrics.graphite.enabled: true
                        myapp.ignored.value.example: 42
                    """.trimIndent(),
                    after = """
                        
                        
                        myapp:
                          metrics:
                            graphite:
                              enabled: true
                          ignored:
                            value.example: 42
                    """.trimIndent(),
                    spec = { path("src/main/resources/application.yml") },
                ),
            )
        }

        @Test
        fun `should expand partially expanded properties matching filter`() {
            rewriteRun(
                yaml(
                    before = """
                        myapp:
                          metrics:
                            graphite.enabled: true
                            graphite.host: localhost
                            prometheus:
                              enabled: true
                          ignored:
                            value.example: 42
                    """.trimIndent(),
                    after = """
                        myapp:
                          metrics:
                            graphite:
                              enabled: true
                              host: localhost
                            prometheus:
                              enabled: true
                          ignored:
                            value.example: 42
                    """.trimIndent(),
                    spec = { path("src/main/resources/application.yml") },
                ),
            )
        }

        @Test
        fun `should support composite prefix for collapsed source`() {
            rewriteRun(
                { spec ->
                    val recipe = ExpandMappings("myapp.language-bundle")
                    spec.recipe(recipe)
                },
                yaml(
                    before = """
                        myapp.metrics.graphite.enabled: true
                        myapp.language-bundle.enabled: true
                        myapp.language-bundle.default-locale: en_US
                    """.trimIndent(),
                    after = """
                        myapp.metrics.graphite.enabled: true
                        myapp.language-bundle:
                          enabled: true
                          default-locale: en_US
                    """.trimIndent(),
                    spec = { path("src/main/resources/application.yml") },
                ),
            )
        }

        @Test
        fun `should support composite prefix for partially expanded source`() {
            rewriteRun(
                { spec ->
                    val recipe = ExpandMappings("myapp.language-bundle")
                    spec.recipe(recipe)
                },
                yaml(
                    before = """
                        myapp.metrics.graphite.enabled: true
                        myapp:
                          language-bundle:
                            example.one: 111
                            example.two: 222
                            another.prop: 42
                    """.trimIndent(),
                    after = """
                        myapp.metrics.graphite.enabled: true
                        myapp:
                          language-bundle:
                            example:
                              one: 111
                              two: 222
                            another:
                              prop: 42
                    """.trimIndent(),
                    spec = { path("src/main/resources/application.yml") },
                ),
            )
        }

        @Test
        fun `should be noop when exclude is broader than prefix`() {
            rewriteRun(
                { spec ->
                    val recipe = ExpandMappings("myapp.languageBundle", listOf("myapp"))
                    spec.recipe(recipe)
                },
                yaml(
                    beforeAndAfter = """
                        myapp.language-bundle.enabled: true
                        myapp.language-bundle.default-locale: en_US
                    """.trimIndent(),
                    spec = { path("src/main/resources/application.yml") },
                ),
            )
        }

        @Test
        fun `should be noop when there are no entries matching filter`() {
            rewriteRun(
                { spec ->
                    val recipe = ExpandMappings("myapp.languageBundle", listOf("myapp"))
                    spec.recipe(recipe)
                },
                yaml(
                    beforeAndAfter = """
                        myapp.i18n.enabled: true
                        myapp.language-bundle.default-locale: en_US
                    """.trimIndent(),
                    spec = { path("src/main/resources/application.yml") },
                ),
            )
        }

        @Test
        fun `should not reformat sequences when expanding`() {
            rewriteRun(
                { spec ->
                    val recipe = ExpandMappings("myapp", listOf("myapp.excluded"))
                    spec.recipe(recipe)
                },
                yaml(
                    before = """
                        myapp.seqence:
                          - first: 1
                            second: 2
                          - first: 3
                        myapp.excluded.sequence:
                          - first: 1
                          - first: 2
                            second: 3
                        ignored:
                          - first: 1
                            second: 2
                          - first: 3
                            second: 4
                    """.trimIndent(),
                    after = """
                        
                        
                        myapp:
                          seqence:
                            - first: 1
                              second: 2
                            - first: 3
                          excluded:
                            sequence:
                              - first: 1
                              - first: 2
                                second: 3
                        ignored:
                          - first: 1
                            second: 2
                          - first: 3
                            second: 4
                    """.trimIndent(),
                    spec = { path("src/main/resources/application.yml") },
                ),
            )
        }
    }
}
