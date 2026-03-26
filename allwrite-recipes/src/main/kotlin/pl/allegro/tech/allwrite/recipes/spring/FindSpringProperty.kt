package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.FindSourceFiles
import org.openrewrite.Option
import org.openrewrite.Preconditions
import org.openrewrite.TreeVisitor
import org.openrewrite.yaml.search.FindProperty
import pl.allegro.tech.allwrite.spi.AllwriteRecipe
import pl.allegro.tech.allwrite.spi.RecipeVisibility.INTERNAL
import pl.allegro.tech.allwrite.recipes.properties.FindProperties

public class FindSpringProperty(
    @Option(
        displayName = "Property key",
        description = "The property key to look for. Always compared using relaxed binding, supports glob",
        example = "server.port",
    )
    public val propertyKey: String,

    @Option(
        displayName = "Expected property value",
        description = "The property value to look for. If `null`, then matches any value",
        example = "8080"
    )
    public val expectedValue: String?,

    @Option(
        displayName = "Glob pattern for filename suffix",
        description = "Glob pattern for file name suffix, can be used to filter by profiles",
        example = "-integration",
        required = false,
    )
    public var fileNameSuffix: String?,
) : AllwriteRecipe(
    displayName = "Find spring property",
    description = "Find spring property with the given key and value, with respect to profile.",
    visibility = INTERNAL
) {
    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        val fileNameSuffix = fileNameSuffix ?: ""
        val fileNameCondition = Preconditions.or(
                FindSourceFiles("**/application${fileNameSuffix}.properties").visitor,
                FindSourceFiles("**/application${fileNameSuffix}.yml").visitor,
                FindSourceFiles("**/application${fileNameSuffix}.yaml").visitor
        )
        val contentCondition = Preconditions.or(
            FindProperties(propertyKey, expectedValue).visitor,
            FindProperty(propertyKey, false, expectedValue).visitor
        )
        return Preconditions.and(
            fileNameCondition,
            contentCondition
        )
    }
}
