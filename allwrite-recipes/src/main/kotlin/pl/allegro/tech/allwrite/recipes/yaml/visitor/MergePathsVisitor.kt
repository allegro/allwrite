package pl.allegro.tech.allwrite.recipes.yaml.visitor

import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.internal.ListUtils
import org.openrewrite.yaml.YamlIsoVisitor
import org.openrewrite.yaml.tree.Yaml
import pl.allegro.tech.allwrite.recipes.yaml.Builders.entry
import pl.allegro.tech.allwrite.recipes.yaml.Builders.mapping
import pl.allegro.tech.allwrite.recipes.yaml.Builders.scalar

/**
 * Join parts of YAML tree, having the same key. For example,
 * ```
 * foo:
 *   bar: 42
 * foo.baz: 1
 * foo:
 *   bax: 0
 * ```
 * Will be transformed into:
 * ```
 * foo:
 *   bar: 42
 *   baz: 1
 *   bax: 0
 * ```
 */
internal class MergePathsVisitor(
    val prefix: String,
) : YamlIsoVisitor<ExecutionContext>() {

    override fun visitMapping(input: Yaml.Mapping, ctx: ExecutionContext): Yaml.Mapping {
        var mapping = input
        val mappingsByKey: Map<String, List<Yaml.Mapping.Entry>> = mapping.entries.groupBy { it.key.value }

        mappingsByKey
            .filter { it.value.size > 1 }
            .forEach { (key, entries) ->
                val mergedSubMappings = entries.map { it.value }
                    .filterIsInstance<Yaml.Mapping>()
                    .flatMap { it.entries }
                val mergedComments = entries.map { it.prefix.trim() }
                    .filter { it.startsWith("#") }
                    .joinToString(separator = "\n", prefix = "\n", postfix = "\n")
                val newEntry = entry(scalar(key), mapping(mergedSubMappings), mergedComments)
                mapping = replaceEntry(mapping, newEntry)
            }

        mapping = maybeAutoFormat(
            input,
            mapping,
            ctx,
            (if (cursor.parent?.getValue<Any>() is Yaml.Document) cursor.parent else cursor)!!,
        )

        return super.visitMapping(mapping, ctx)
    }

    override fun <Y : Yaml> maybeAutoFormat(before: Y, after: Y, p: ExecutionContext, cursor: Cursor): Y {
        if (before !== after) {
            return AutoFormatVisitor().visit(after, p, cursor) as Y
        }
        return after
    }

    private fun replaceEntry(mapping: Yaml.Mapping, toBe: Yaml.Mapping.Entry): Yaml.Mapping {
        val insertionIndex = mapping.entries.indexOfFirst { entry -> entry.key.value == toBe.key.value }
        val cleanedUpEntries = mapping.entries.filterNot { entry -> entry.key.value == toBe.key.value }
        return mapping.withEntries(ListUtils.insertAll(cleanedUpEntries, insertionIndex, listOf(toBe)))
    }
}
