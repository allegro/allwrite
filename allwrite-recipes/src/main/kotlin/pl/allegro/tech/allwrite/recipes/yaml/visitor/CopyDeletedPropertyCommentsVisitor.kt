package pl.allegro.tech.allwrite.recipes.yaml.visitor

import org.openrewrite.ExecutionContext
import org.openrewrite.Tree
import org.openrewrite.marker.Markers
import org.openrewrite.yaml.YamlIsoVisitor
import org.openrewrite.yaml.tree.Yaml
import org.openrewrite.yaml.tree.Yaml.Document
import pl.allegro.tech.allwrite.recipes.yaml.prefixParts
import java.util.UUID

internal class CopyDeletedPropertyCommentsVisitor(
    private val snapshot: PreOrderTraversalSnapshot,
    private val removed: Yaml,
    private val removedParent: Yaml.Mapping,
) : YamlIsoVisitor<ExecutionContext>() {

    var prev: Yaml.Mapping.Entry? = null

    override fun visitDocument(document: Document, p: ExecutionContext): Document {
        var doc = super.visitDocument(document, p)

        // If we reached the end of the document and the removed entry was
        // after the last visited entry, we have to copy comments to the document's
        // end. Checking that removed entry was last is not enough, as we can remove
        // multiple nested mappings within a single DeleteProperty run
        if (removed.isImmediatelyAfter(prev)) {
            val end = doc.end ?: Document.End(UUID.randomUUID(), "", Markers.EMPTY, false)
            val newPrefix = removed.prefixParts()
                .merge(end.prefixParts())
                .asString(keepNewLinesInMain = true)
                .removeSuffix("\n") // remove \n in the end, as Document.End should not contain a trailing line break
            doc = doc.withEnd(end.withPrefix(newPrefix))
        }

        return doc
    }

    override fun visitMappingEntry(e: Yaml.Mapping.Entry, p: ExecutionContext): Yaml.Mapping.Entry {
        var entry = e

        // if removed entry was between current entry and previously visited
        //   and parent mapping has other entries than the one being removed
        // then we have to copy comment
        if (removed.isBetween(prev, entry) && removedParent.entries.size > 1) {
            val newPrefix = removed.prefixParts().merge(entry.prefixParts())
            entry = entry.withPrefix(newPrefix.asString(keepNewLinesInMain = true))
        }

        prev = entry
        return super.visitMappingEntry(entry, p)
    }

    private fun Yaml.serial() = snapshot.order[this]
    private fun Yaml.isFirst() = serial() == 0
    private fun Yaml.isImmediatelyAfter(another: Yaml?): Boolean {
        if (another == null) return isFirst()
        val anotherSerial = another.serial() ?: return false
        return anotherSerial + 1 == serial()
    }

    private fun Yaml.isBetween(prev: Yaml?, next: Yaml?): Boolean {
        val thisSerial = this.serial()
        val prevSerial = if (prev == null) -1 else prev.serial()
        val nextSerial = next?.serial()
        return if (thisSerial != null && prevSerial != null && nextSerial != null) {
            thisSerial in (prevSerial + 1)..<nextSerial
        } else {
            false
        }
    }
}

internal class PreOrderTraversalSnapshot(
    val order: Map<Yaml, Int>,
)

// Capture visited entries in pre-order traversal and produce a mapping
// from Yaml to its serial number during traversal
internal class PreOrderVisitor : YamlIsoVisitor<ExecutionContext>() {

    private var counter = 0
    private val order: MutableMap<Yaml, Int> = HashMap()

    fun traverse(tree: Tree?, p: ExecutionContext): PreOrderTraversalSnapshot {
        super.visit(tree, p)
        val result = HashMap(order)
        order.clear()
        return PreOrderTraversalSnapshot(result)
    }

    override fun visitMappingEntry(entry: Yaml.Mapping.Entry, p: ExecutionContext): Yaml.Mapping.Entry {
        order[entry] = counter++
        return super.visitMappingEntry(entry, p)
    }

    override fun visitSequenceEntry(entry: Yaml.Sequence.Entry, p: ExecutionContext): Yaml.Sequence.Entry {
        order[entry] = counter++
        return super.visitSequenceEntry(entry, p)
    }
}
