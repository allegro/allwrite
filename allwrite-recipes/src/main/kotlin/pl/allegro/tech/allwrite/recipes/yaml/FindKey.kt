package pl.allegro.tech.allwrite.recipes.yaml

import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.marker.SearchResult
import org.openrewrite.yaml.YamlIsoVisitor
import org.openrewrite.yaml.tree.Yaml
import pl.allegro.tech.allwrite.recipes.yaml.YamlPath.Companion.toYamlPath
import org.openrewrite.internal.NameCaseConvention.LOWER_HYPHEN

/**
 * Lighter and faster version of [org.openrewrite.yaml.search.FindKey], which does not support
 * JsonPath and instead checks for simple case-insensitive key match.
 */
public class FindKey(
    @Option(displayName = "key", description = "yaml key to find", example = "myapp", required = false)
    public val key: String? = null,
) : Recipe() {
    override fun getDisplayName(): String = "Find key"

    override fun getDescription(): String = "Should be used as precondition, which fires when YAML contains the specified key."

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        return if (key == null) TreeVisitor.noop<Tree, ExecutionContext>() else Visitor(key)
    }

    internal class Visitor(target: String) : YamlIsoVisitor<ExecutionContext>() {

        private val target = LOWER_HYPHEN.format(target)

        override fun visitMappingEntry(entry: Yaml.Mapping.Entry, p: ExecutionContext): Yaml.Mapping.Entry {
            val path = LOWER_HYPHEN.format(cursor.toYamlPath().path)
            if (path == target || path.startsWith("$target.")) return SearchResult.found(entry)!!
            if (target.startsWith(path)) {
                return super.visitMappingEntry(entry, p)
            }
            return entry
        }

        override fun visitMapping(mapping: Yaml.Mapping, p: ExecutionContext): Yaml.Mapping {
            val path = cursor.toYamlPath().path
            if (path == target || path.startsWith("$target.")) return SearchResult.found(mapping)!!
            if (target.startsWith(path)) {
                return super.visitMapping(mapping, p)
            }
            return mapping
        }

        override fun visitSequenceEntry(entry: Yaml.Sequence.Entry, p: ExecutionContext): Yaml.Sequence.Entry {
            val path = cursor.toYamlPath().path
            if (path == target || path.startsWith("$target.")) return SearchResult.found(entry)!!
            if (target.startsWith(path)) {
                return super.visitSequenceEntry(entry, p)
            }
            return entry
        }
    }
}
