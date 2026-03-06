package pl.allegro.tech.allwrite.recipes.yaml

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.yaml

class UnnestPropertiesTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(UnnestProperties(
            targetPath = "spring.groovy.template.configuration"
        ))
    }

    @Test
    fun `should unnest one level of properties`() {
        rewriteRun(
            yaml(
                before = """
                    spring.groovy.template:
                      configuration:
                       inner:
                         value: 1
                       b: 2
                       enabled: true
                    """.trimIndent(),
                after = """
                    spring.groovy.template:
                      inner:
                        value: 1
                      b: 2
                      enabled: true
                    """.trimIndent(),
                spec = { path("src/main/resources/application.yml") }
            )
        )
    }

    @Test
    fun `should adjust formatting while unnesting`() {
        rewriteRun(
            yaml(
                before = """
                    spring.groovy.template:
                       configuration:
                         a: 1
                         # comment
                         b: 2
                       enabled: true
                    
                    
                    another:
                             indent: unchanged
                    """.trimIndent(),
                after = """
                    spring.groovy.template:
                       enabled: true
                       a: 1
                       # comment
                       b: 2
                    
                    
                    another:
                             indent: unchanged
                    """.trimIndent(),
                spec = { path("src/main/resources/application.yml") }
            )
        )
    }

    @Test
    @Disabled("TODO")
    fun `should unnest flat`() {
        rewriteRun(
            yaml(
                before = """
                    spring.groovy.template.configuration.a: 1
                    spring.groovy.template.configuration.b: 2
                    """.trimIndent(),
                after = """
                    spring.groovy.template:
                      a: 1
                      b: 2
                    """.trimIndent(),
                spec = { path("src/main/resources/application.yml") }
            )
        )
    }
}
