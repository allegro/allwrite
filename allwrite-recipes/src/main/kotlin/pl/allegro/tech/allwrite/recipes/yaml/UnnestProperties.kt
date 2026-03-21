package pl.allegro.tech.allwrite.recipes.yaml

import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.TreeVisitor
import org.openrewrite.yaml.YamlIsoVisitor
import org.openrewrite.yaml.tree.Yaml
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility
import pl.allegro.tech.allwrite.recipes.yaml.YamlPath.Companion.toYamlPath
import pl.allegro.tech.allwrite.recipes.yaml.visitor.AutoFormatVisitor

public class UnnestProperties(
    @Option(
        displayName = "path",
        description = "Path to unnest",
        example = "spring.groovy.template.configuration"
    )
    public val targetPath: String = "",
) : AllwriteRecipe(
    displayName = "Unnest properties",
    description = "Remove one level of nesting from mapping.",
    visibility = RecipeVisibility.INTERNAL,
) {
    private val targetMappingPath = targetPath.substringBeforeLast('.')
    private val targetMappingEntryKey = targetPath.substringAfterLast('.')

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        return object : YamlIsoVisitor<ExecutionContext>() {
            override fun visitMapping(mapping: Yaml.Mapping, ctx: ExecutionContext): Yaml.Mapping {
                val path = cursor.toYamlPath()
                return if (path.path == targetMappingPath) {
                    mapTargetMapping(mapping, ctx)
                } else {
                    super.visitMapping(mapping, ctx)
                }
            }

            private fun mapTargetMapping(mapping: Yaml.Mapping, ctx: ExecutionContext): Yaml.Mapping {
                val targetEntry = mapping.entries.firstOrNull { it.key.value == targetMappingEntryKey }
                val targetEntryBlock = targetEntry?.value as? Yaml.Mapping ?: return mapping

                val newEntries = mapping.entries.toMutableList()
                newEntries.remove(targetEntry)
                newEntries.addAll(targetEntryBlock.entries)

                val newMapping = mapping.withEntries(newEntries)
                return AutoFormatVisitor().visit(newMapping, ctx, cursor.parent) as Yaml.Mapping
            }
        }
    }
}
