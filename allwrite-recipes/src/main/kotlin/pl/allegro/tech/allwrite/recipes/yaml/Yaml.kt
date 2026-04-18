package pl.allegro.tech.allwrite.recipes.yaml

import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Tree
import org.openrewrite.internal.ListUtils
import org.openrewrite.marker.Markers
import org.openrewrite.yaml.tree.Yaml
import org.openrewrite.yaml.tree.YamlKey
import pl.allegro.tech.allwrite.recipes.yaml.Builders.mapping
import pl.allegro.tech.allwrite.recipes.yaml.visitor.EmittingVisitor

internal object Builders {
    fun mapping(entries: List<Yaml.Mapping.Entry>) =
        Yaml.Mapping(
            Tree.randomId(),
            Markers.EMPTY,
            null,
            entries,
            null,
            null,
            null,
        )

    fun entry(key: YamlKey, value: Yaml.Block, prefix: String = "") =
        Yaml.Mapping.Entry(
            Tree.randomId(),
            prefix,
            Markers.EMPTY,
            key,
            "",
            value,
        )

    fun scalar(value: String) =
        Yaml.Scalar(
            Tree.randomId(),
            "",
            Markers.EMPTY,
            Yaml.Scalar.Style.PLAIN,
            null,
            null,
            value,
        )
}

internal object Mutators {
    fun Yaml.Documents.mapDocuments(mapper: (Yaml.Document) -> Yaml.Document) = this.withDocuments(this.documents.map(mapper))
    inline fun <reified Y : Yaml.Block> Yaml.Document.mapBlock(mapper: (Y) -> Y) = if (this.block is Y) this.withBlock(mapper(this.block as Y)) else this
    fun Yaml.Mapping.mapEntries(mapper: (Yaml.Mapping.Entry) -> Yaml.Mapping.Entry) = this.withEntries(this.entries.map(mapper))
    fun Yaml.Sequence.mapEntries(mapper: (Yaml.Sequence.Entry) -> Yaml.Sequence.Entry) = this.withEntries(this.entries.map(mapper))
    fun Yaml.Sequence.mapFirstEntry(mapper: (Yaml.Sequence.Entry) -> Yaml.Sequence.Entry) = this.withEntries(ListUtils.mapFirst(this.entries, mapper)!!)
    fun Yaml.Mapping.Entry.withMapping(entries: List<Yaml.Mapping.Entry>) = this.withValue(mapping(entries))
}

internal inline fun <reified Y : Yaml> Yaml.Document.nodes(): Map<YamlPath, Y> {
    val emitter = EmittingVisitor<Y>()
    emitter.visit(this, InMemoryExecutionContext())
    return emitter.nodes
}

internal fun Yaml.Mapping.lastEntryPrefix() = entries.lastOrNull()?.prefix ?: ""
