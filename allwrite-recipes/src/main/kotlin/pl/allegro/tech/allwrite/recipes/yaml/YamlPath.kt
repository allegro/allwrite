package pl.allegro.tech.allwrite.recipes.yaml

import org.openrewrite.Cursor
import org.openrewrite.yaml.tree.Yaml

/**
 * Simple representation of the current path in the tree. Only supports mapping entries and sequence entries.
 *
 * OpenRewrite also supplies [org.openrewrite.yaml.JsonPathMatcher], which uses a similar approach and
 * should be used in most of the cases, but not always sufficient.
 */
@JvmInline internal value class YamlPath(val path: String) {

    fun isRoot() = path.trim().isEmpty()

    fun and(nextSegment: String) = if (isRoot()) YamlPath(nextSegment) else YamlPath("$path.$nextSegment")

    fun map(mapper: (String) -> String) = YamlPath(mapper(path))

    companion object {
        fun List<String>.toYamlPath() = YamlPath(this.joinToString(separator =  "."))

        /**
         * Iterates over parents through [Cursor] and builds [YamlPath]
         */
        fun Cursor.toYamlPath() = this.pathAsCursors.asSequence().toList()
            .mapNotNull {
                when (val value = it.getValue<Any>()) {
                    is Yaml.Mapping.Entry -> {
                        value.key.value
                    }

                    is Yaml.Sequence.Entry -> {
                        val index = (it.parent?.getValue<Any>() as? Yaml.Sequence)?.entries?.indexOfFirst { e -> e.id == value.id }?.toString() ?: "?"
                        "[$index]"
                    }

                    else -> null
                }
            }
            .reversed()
            .toYamlPath()
    }
}
