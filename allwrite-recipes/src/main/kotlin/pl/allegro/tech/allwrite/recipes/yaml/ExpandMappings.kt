package pl.allegro.tech.allwrite.recipes.yaml

import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.Recipe
import org.openrewrite.TreeVisitor
import org.openrewrite.yaml.YamlIsoVisitor
import org.openrewrite.yaml.tree.Yaml
import pl.allegro.tech.allwrite.recipes.yaml.Builders.entry
import pl.allegro.tech.allwrite.recipes.yaml.Builders.scalar
import pl.allegro.tech.allwrite.recipes.yaml.Mutators.withMapping
import pl.allegro.tech.allwrite.recipes.yaml.YamlPath.Companion.toYamlPath
import pl.allegro.tech.allwrite.recipes.yaml.visitor.AutoFormatVisitor
import pl.allegro.tech.allwrite.recipes.yaml.visitor.MergePathsVisitor

/**
 * Transforms properties from plain structure into hierarchical and merges paths, for example:
 * ```
 * myapp.metrics.graphite.enabled: true
 * myapp.metrics.graphite:
 *   host: localhost
 *   port: 2003
 * myapp:
 *   i18n:
 *    enabled: true
 * ```
 * Gets transformed into:
 * ```
 * myapp:
 *   metrics:
 *     graphite:
 *       enabled: true
 *       host: localhost
 *       port: 2003
 *   i18n:
 *     enabled: true
 * ```
 *
 * Supports [prefix] to only transform YAML entries having key matching the prefix.
 * Supports [excludes] to not transform entries matching the specified prefixes.
 * Both [prefix] and [excludes] should contain strings, representing a path in YAML document,
 * with all parts matching the keys exactly, i.e. `prefix=my` will not match `myapp.metrics.enabled`
 * property, but `prefix=myapp` and `prefix=myapp.metrics` will
 */
public class ExpandMappings(
    @Option(displayName = "prefix", description = "Prefix", example = "myapp", required = false)
    public var prefix: String = "",

    @Option(displayName = "excludes", description = "Exclude prefixes", example = "myapp", required = false)
    public var excludes: List<String> = emptyList()
) : Recipe() {

    override fun getDisplayName(): String = "Transform properties from plain structure into hierarchical"

    override fun getDescription(): String = """
        Transforms properties from plain structure into hierarchical and merges paths.
    """.trimIndent()

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> = Visitor(prefix, excludes)

    public open class Visitor(
        private val prefix: String,
        private val excludes: List<String>
    ) : YamlIsoVisitor<ExecutionContext>() {
        private val mergePathsVisitor = MergePathsVisitor(prefix)

        override fun visitDocument(document: Yaml.Document, p: ExecutionContext): Yaml.Document {
            val expanded = super.visitDocument(document, p)
            return maybeAutoFormat(document, expanded, p, cursor.fork())
        }

        override fun visitMapping(mapping: Yaml.Mapping, p: ExecutionContext): Yaml.Mapping {
            val expanded = super.visitMapping(mapping, p)
            return mergePathsVisitor.visit(expanded, p, cursor.parentOrThrow) as Yaml.Mapping
        }

        override fun visitMappingEntry(input: Yaml.Mapping.Entry, context: ExecutionContext): Yaml.Mapping.Entry {
            return when (action(input)) {
                Action.EXPAND -> expand(input, context)
                Action.PROCEED -> super.visitMappingEntry(input, context)
                Action.SKIP -> input
            }
        }

        private fun expand(input: Yaml.Mapping.Entry, context: ExecutionContext): Yaml.Mapping.Entry {
            var entry = input
            val key = entry.key
            val keyValue = key.value

            if (key is Yaml.Scalar && keyValue.contains(".")) {
                val (newKeyValue, remainingKey) = splitKey(keyValue)
                val (remainder, main, indent) = entry.prefixParts()
                val remainingEntry = entry(
                    key = scalar(remainingKey),
                    value = entry.value,
                    prefix = PrefixParts(null, main, "").asString()
                )

                entry = entry
                    .withKey(key.withValue(newKeyValue)).withMapping(entries = listOf(remainingEntry))
                    .withPrefix(PrefixParts(remainder, "", indent).asString())

                entry = autoFormat(entry, context, cursor.parentOrThrow)
                setCursor(Cursor(cursor.parent, entry))
            }
            return super.visitMappingEntry(entry, context)
        }

        // choose how to split the key
        // in case of complex prefixes and collapsed entries (e.g prefix myapp.metrics and property myapp.metrics.graphite.enabled),
        // we have to make sure prefix parts are not expanded. Otherwise, simply split in 2 parts by '.'
        private fun splitKey(keyValue: String): Pair<String, String> {
            return if (keyValue.startsWith("$prefix.")) {
                Pair(prefix, keyValue.removePrefix("$prefix."))
            } else {
                val (new, remainder) = keyValue.split(".", limit = 2)
                Pair(new, remainder)
            }
        }

        private fun action(entry: Yaml.Mapping.Entry): Action {
            val key = entry.key.value
            val parentKey = cursor.parentOrThrow.toYamlPath()
            val fullKeyPath = parentKey.and(key).path

            // if parent key exactly matches one of the exclusions, skip this entry
            if (excludes.any { it == parentKey.path }) return Action.SKIP

            // if current key exactly matches the prefix, proceed to children, as
            // we only need to expand subtrees of the tree with given prefix
            if (prefix == fullKeyPath) return Action.PROCEED

            // if current key starts with the desired prefix (and does not match it exactly),
            // we have to expand it
            if (fullKeyPath.startsWith(prefix)) return Action.EXPAND

            // if current key does not start with a desired prefix, but desired prefix is
            // longer - we have to visit children to check if they match the prefix
            if (prefix.startsWith(fullKeyPath)) return Action.PROCEED

            // nothing else matched - skip
            return Action.SKIP
        }

        override fun <Y: Yaml> autoFormat(y: Y, p: ExecutionContext, cursor: Cursor): Y {
            return AutoFormatVisitor(prefix = prefix).visit(y, p, cursor) as Y
        }

        override fun <Y : Yaml> maybeAutoFormat(before: Y, after: Y, p: ExecutionContext, cursor: Cursor): Y {
            if (before !== after) {
                return AutoFormatVisitor(prefix = prefix).visit(after, p, cursor) as Y
            }
            return after
        }

        private enum class Action {
            // expand current entry and proceed to child entries
            EXPAND,

            // do not expand current entry, but proceed to child entries
            PROCEED,

            // do not expand current entry, do not process child entries
            SKIP
        }
    }
}
