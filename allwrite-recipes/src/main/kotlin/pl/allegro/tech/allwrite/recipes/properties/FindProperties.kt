package pl.allegro.tech.allwrite.recipes.properties

import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.internal.NameCaseConvention
import org.openrewrite.marker.SearchResult
import org.openrewrite.properties.PropertiesVisitor
import org.openrewrite.properties.tree.Properties
import pl.allegro.tech.allwrite.spi.AllwriteRecipe
import pl.allegro.tech.allwrite.spi.RecipeVisibility.INTERNAL

public class FindProperties(
    @Option(
        displayName = "Property key",
        description = "The property key to look for. Always compared using relaxed binding, supports glob",
        example = "server.port"
    )
    public val propertyKey: String,

    @Option(
        displayName = "Expected property value",
        description = "The property value to look for. If null, matches any value",
        example = "8080"
    )
    public val expectedValue: String?,
): AllwriteRecipe(
    displayName = "Find property",
    description = "Finds occurrences of a property with given key and value.",
    visibility = INTERNAL
) {
    override fun getVisitor(): TreeVisitor<*, ExecutionContext> = Visitor()

    internal inner class Visitor : PropertiesVisitor<ExecutionContext>() {
        override fun visitEntry(entry: Properties.Entry, ctx: ExecutionContext): Properties {
            var entry = entry
            val key = entry.key
            val value = entry.value.text.trim()
            val valueMatches = expectedValue == null || value == expectedValue
            if (NameCaseConvention.matchesGlobRelaxedBinding(key, propertyKey) && valueMatches) {
                val v = entry.value.withMarkers(entry.value.markers.add(SearchResult(Tree.randomId(), null)))
                entry = entry.withValue(v)
            }
            return super.visitEntry(entry, ctx)
        }
    }
}
