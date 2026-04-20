package pl.allegro.tech.allwrite.recipes.yaml

import org.junit.jupiter.api.Test
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.yaml

class FindKeyTest : RewriteTest {

    @Test
    fun `should find simple property`() {
        rewriteRun(
            { spec ->
                spec.recipe(FindKey("target"))
            },
            yaml(
                before = """
                    prop1: 1
                    target: 2
                """.trimIndent(),
                after = """
                    prop1: 1
                    ~~>target: 2
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should find nested property`() {
        rewriteRun(
            { spec ->
                spec.recipe(FindKey("target.prop1"))
            },
            yaml(
                before = """
                    prop1: 1
                    target:
                      prop1: 1
                      prop2: 2
                """.trimIndent(),
                after = """
                    prop1: 1
                    target:
                      ~~>prop1: 1
                      prop2: 2
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should find property when collapsed`() {
        rewriteRun(
            { spec ->
                spec.recipe(FindKey("target"))
            },
            yaml(
                before = """
                    prop1: 1
                    target.prop1: 1
                """.trimIndent(),
                after = """
                    prop1: 1
                    ~~>target.prop1: 1
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should be noop when property does not exist`() {
        rewriteRun(
            { spec ->
                spec.recipe(FindKey("target"))
            },
            yaml(
                beforeAndAfter = """
                    prop1: 1
                    targetProp.prop1: 1
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }
}
