package pl.allegro.tech.allwrite.recipes.yaml

import org.openrewrite.ExecutionContext
import org.openrewrite.TreeVisitor
import org.openrewrite.marker.SearchResult
import org.openrewrite.yaml.JsonPathMatcher
import org.openrewrite.yaml.YamlVisitor
import org.openrewrite.yaml.tree.Yaml
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.INTERNAL

public class YamlEntryHasValue(
    key: String,
    public val expectedValue: String
) : AllwriteRecipe(
    displayName = "YAML entry has value",
    description = """
        |Find YAML entries specified by JSON path with the given value
        """.trimMargin(),
    visibility = INTERNAL
) {
    private val matcher = JsonPathMatcher(key)

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> = object : YamlVisitor<ExecutionContext>() {
        override fun visitMappingEntry(entry: Yaml.Mapping.Entry, p: ExecutionContext): Yaml {
            val visitedEntry = super.visitMappingEntry(entry, p) as Yaml.Mapping.Entry
            return when {
                matches(visitedEntry) -> SearchResult.found(visitedEntry)!!
                else -> visitedEntry
            }
        }

        private fun matches(entry: Yaml.Mapping.Entry) =
            matcher.matches(cursor) &&
                (entry.value as? Yaml.Scalar)?.value == expectedValue
    }
}
