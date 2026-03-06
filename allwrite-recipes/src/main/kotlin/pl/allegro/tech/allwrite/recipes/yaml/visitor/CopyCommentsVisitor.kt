package pl.allegro.tech.allwrite.recipes.yaml.visitor

import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.yaml.YamlIsoVisitor
import org.openrewrite.yaml.tree.Yaml
import pl.allegro.tech.allwrite.recipes.yaml.PrefixParts
import pl.allegro.tech.allwrite.recipes.yaml.YamlPath
import pl.allegro.tech.allwrite.recipes.yaml.YamlPath.Companion.toYamlPath
import pl.allegro.tech.allwrite.recipes.yaml.asPrefixParts
import pl.allegro.tech.allwrite.recipes.yaml.prefixParts
import java.util.*


/**
 * Traverses the original document and captures comments for the entries. Then, when visiting
 * the actual document applies comments to the same entries based on [YamlPath]
 *
 * [keyMapper] is a function used to transform the key. Transformation is applied to both original
 * and visited documents and helps to bring keys to the unified format. This may be useful to provide
 * case or style insensitive matching or when keys have changed. Default implementation is noop
 */
internal class CopyCommentsVisitor(
    original: Yaml.Documents,
    private val keyMapper: (YamlPath) -> YamlPath = { it }
) : YamlIsoVisitor<ExecutionContext>() {

    private val originalCommentsByDocument = original.documents.associate { doc ->
        doc.id to CaptureOriginalCommentsVisitor(keyMapper).also { it.visit(doc, InMemoryExecutionContext()) }
    }

    // keeps track of the previously visited mapping entry to apply `remainder` part of the prefix to it
    private var prev: YamlPath? = null

    override fun visitDocument(input: Yaml.Document, p: ExecutionContext): Yaml.Document {
        var document = super.visitDocument(input, p)

        // add a comment to the last entry in the document
        val originalComment = originalCommentsByDocument[document.id]?.postfixComments?.get(prev)
        val documentEnd = if (originalComment != null) {
            document.end.withPrefix(originalComment)
        } else {
            document.end.withPrefix("")
        }

        prev = null // document has changed, clear up link to the previous entry
        return document.withEnd(documentEnd)
    }

    override fun visitMappingEntry(entry: Yaml.Mapping.Entry, p: ExecutionContext): Yaml.Mapping.Entry {
        val e = visitInternal(entry, p, Yaml.Mapping.Entry::withPrefix)

        // we should visit children nodes after parent nodes to apply comments appropriately
        // this ensures that `prev` follows the natural top-to-down (pre-order) traversing
        return super.visitMappingEntry(e, p)
    }

    override fun visitSequenceEntry(entry: Yaml.Sequence.Entry, p: ExecutionContext): Yaml.Sequence.Entry {
        var e = entry

        // we only have to copy comments for scalar elements, as non-scalar will be handled as Mapping.Entry
        if (entry.block is Yaml.Scalar) {
            e = visitInternal(entry, p, Yaml.Sequence.Entry::withPrefix)
        }

        // we should visit children nodes after parent nodes to apply comments appropriately
        // this ensures that `prev` follows the natural top-to-down (pre-order) traversing
        return super.visitSequenceEntry(e, p)
    }

    private fun <Y: Yaml> visitInternal(yaml: Y, ctx: ExecutionContext, commentSetter: (Y, String) -> Y): Y {
        var y = yaml
        val key = keyMapper(cursor.toYamlPath())
        val doc = cursor.firstEnclosing(Yaml.Document::class.java)
        if (doc != null) {
            val newPrefix = copyComments(key, y.prefix, doc.id)
            if (newPrefix != y.prefix) {
                y = commentSetter(y, newPrefix)
                y = AutoFormatVisitor().visit(y, ctx, cursor.parent) as Y
            }
        }

        prev = key
        return y
    }

    private fun copyComments(key: YamlPath, prefix: String, docId: UUID): String {
        val prev = this.prev
        val originalComments = originalCommentsByDocument[docId] ?: return prefix
        val remainder = prev?.let { originalComments.postfixComments[prev] }
        val main = originalComments.prefixComments[key] ?: ""

        // build a new prefix:
        // if there was a comment for `prev` key in the original document, use it as remainder; empty string otherwise
        // keep `main` from the original document, matching by `key`
        // take `indent` from the current document to respect formatting
        val (_, _, currentIndent) = prefix.asPrefixParts()
        return PrefixParts(remainder, main, currentIndent).asString()
    }

    /**
     * Traverse the tree and create a mapping between [YamlPath] and corresponding comments.
     * For each [YamlPath] comments are separated into [prefixComments] and [postfixComments].
     */
    private class CaptureOriginalCommentsVisitor(
        private val keyMapper: (YamlPath) -> YamlPath
    ): YamlIsoVisitor<ExecutionContext>() {
        val prefixComments: MutableMap<YamlPath, String> = HashMap()
        val postfixComments: MutableMap<YamlPath, String> = HashMap()

        // keeps track of the previously visited mapping entry
        // if prefix has a `remainder` part, it should be routed to the `prev`
        var prev: YamlPath? = null

        override fun visitDocument(input: Yaml.Document, p: ExecutionContext): Yaml.Document {
            val document = super.visitDocument(input, p)
            if (prev != null) {
                postfixComments[prev!!] = document.end.prefix
            }
            return document
        }

        override fun visitMappingEntry(entry: Yaml.Mapping.Entry, p: ExecutionContext): Yaml.Mapping.Entry {
            capture(entry)
            return super.visitMappingEntry(entry, p)
        }

        override fun visitSequenceEntry(entry: Yaml.Sequence.Entry, p: ExecutionContext): Yaml.Sequence.Entry {
            if (entry.block is Yaml.Scalar) {
                capture(entry)
            }
            return super.visitSequenceEntry(entry, p)
        }

        private fun <Y: Yaml> capture(yaml: Y) {
            var y = yaml
            val key = keyMapper(cursor.toYamlPath())
            val (previousSuffix, currentPrefix, currentIndent) = y.prefixParts()
            prefixComments[key] = PrefixParts(null, currentPrefix, currentIndent).asString()
            if (prev != null && previousSuffix != null) {
                postfixComments[prev!!] = previousSuffix
            }
            prev = key
        }
    }
}

