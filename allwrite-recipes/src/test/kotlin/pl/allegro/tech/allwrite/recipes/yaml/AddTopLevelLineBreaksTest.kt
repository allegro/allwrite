package pl.allegro.tech.allwrite.recipes.yaml

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.yaml

class AddTopLevelLineBreaksTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        val recipe = AddTopLevelLineBreaks()
        spec.recipe(recipe)
    }

    @Test
    fun `should add line breaks to top level properties`() {
        rewriteRun(
            { spec ->
                spec.cycles(1).expectedCyclesThatMakeChanges(1)
            },
            yaml(
                before = """
                    prop1: 123
                    prop2: 456
                    prop3: 789
                """.trimIndent(),
                after = """
                    prop1: 123
                    
                    prop2: 456
                    
                    prop3: 789
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should remove odd line breaks from top level properties`() {
        rewriteRun(
            { spec ->
                spec.cycles(1).expectedCyclesThatMakeChanges(1)
            },
            yaml(
                before = """
                    prop1: 123
                    
                    
                    
                    prop2: 456
                    prop3: 789
                """.trimIndent(),
                after = """
                    prop1: 123
                    
                    prop2: 456
                    
                    prop3: 789
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should not change line breaks for nested properties`() {
        rewriteRun(
            { spec ->
                spec.cycles(1).expectedCyclesThatMakeChanges(1)
            },
            yaml(
                before = """
                    prop1:
                    
                    
                      nested:
                    
                    
                    
                         value: 123
                    prop2: 456
                    prop3:
                      nested:
                    
                    
                         value: 456
                """.trimIndent(),
                after = """
                    prop1:
                    
                    
                      nested:
                    
                    
                    
                         value: 123
                    
                    prop2: 456
                    
                    prop3:
                      nested:
                    
                    
                         value: 456
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should keep newline after document separator`() {
        rewriteRun(
            { spec ->
                spec.cycles(1).expectedCyclesThatMakeChanges(1)
            },
            yaml(
                before = """
                    ---
                    prop1: 123
                """.trimIndent(),
                after = """
                    ---
                    prop1: 123
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }

    @Test
    fun `should keep newline after comment`() {
        rewriteRun(
            { spec ->
                spec.cycles(1).expectedCyclesThatMakeChanges(1)
            },
            yaml(
                before = """
                    # comment
                    prop1: 123
                """.trimIndent(),
                after = """
                    # comment
                    prop1: 123
                """.trimIndent(),
                spec = { path("src/main/resources/application.yml") },
            ),
        )
    }
}
