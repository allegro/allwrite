package pl.allegro.tech.allwrite.recipes.gradle

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.RewriteTest.toRecipe
import pl.allegro.tech.allwrite.recipes.toml
import java.util.function.Supplier

class AddTomlVersionCatalogDependencyTest : RewriteTest {

    private val library = Library(group = "com.example", name = "test", version = PlainVersion("0.0.11"))

    override fun defaults(spec: RecipeSpec) {
        spec.recipes(recipeUnderTest(library, "com.example.test"))
    }

    @Test
    fun `should add dependency when does not exist`() {
        rewriteRun(
            toml(
                before = """
                [libraries]
                smth-else = { group = "smth", name = "else" }
                """.trimIndent(),
                after = """
                [libraries]
                smth-else = { group = "smth", name = "else" }
                com-example-test = { group = "com.example", name = "test", version = "0.0.11" }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should not add dependency when it already exists and onVersionConflict=IGNORE`() {
        rewriteRun(
            toml(
                beforeAndAfter = """
                [libraries]
                smth-else = { group = "smth", name = "else" }
                com-example-test = { group = "com.example", name = "test" }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should change dependency when it already exists and onVersionConflict=OVERRIDE`() {
        rewriteRun(
            { spec -> spec.recipes(recipeUnderTest(library, "com.example.test", OnVersionConflict.OVERRIDE)) },
            toml(
                before = """
                [libraries]
                smth-else = { group = "smth", name = "else" }
                com-example-test = { group = "com.example", name = "test" }
                """.trimIndent(),
                after = """
                [libraries]
                smth-else = { group = "smth", name = "else" }
                com-example-test = { group = "com.example", name = "test", version = "0.0.11" }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should add dependency when there is no libraries table`() {
        rewriteRun(
            toml(
                before = "",
                after = """
                [libraries]
                com-example-test = { group = "com.example", name = "test", version = "0.0.11" }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should add dependency with version ref`() {
        rewriteRun(
            { spec -> spec.recipe(recipeUnderTest(Library("com.example", "test", VersionRef("x")), "com.example.test")).validateRecipeSerialization(false) },
            toml(
                before = "",
                after = """
                [libraries]
                com-example-test = { group = "com.example", name = "test", version.ref = "x" }
                """.trimIndent(),
            ),
        )
    }

    private fun recipeUnderTest(library: Library, versionCatalogName: String, onVersionConflict: OnVersionConflict = OnVersionConflict.IGNORE) =
        toRecipe(Supplier { AddTomlVersionCatalogDependency(library, versionCatalogName, onVersionConflict) })
}
