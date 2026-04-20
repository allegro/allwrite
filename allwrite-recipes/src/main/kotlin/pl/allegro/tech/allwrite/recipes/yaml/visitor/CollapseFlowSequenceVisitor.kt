package pl.allegro.tech.allwrite.recipes.yaml.visitor

import org.openrewrite.ExecutionContext
import org.openrewrite.yaml.YamlIsoVisitor
import org.openrewrite.yaml.tree.Yaml
import pl.allegro.tech.allwrite.recipes.yaml.Mutators.mapEntries
import pl.allegro.tech.allwrite.recipes.yaml.Mutators.mapFirstEntry
import pl.allegro.tech.allwrite.recipes.yaml.YamlPath
import pl.allegro.tech.allwrite.recipes.yaml.YamlPath.Companion.toYamlPath
import pl.allegro.tech.allwrite.recipes.yaml.nodes

/**
 * Collapses [Yaml.Sequence] if it is collapsed in the original document
 *
 * [keyMapper] is a function used to transform the key. Transformation is applied to both original
 * and visited documents and helps to bring keys to the unified format. This may be useful to provide
 * case or style insensitive matching or when keys have changed. Default implementation is noop
 */
internal class CollapseFlowSequenceVisitor(
    original: Yaml.Documents,
    private val keyMapper: (YamlPath) -> YamlPath = { it },
) : YamlIsoVisitor<ExecutionContext>() {

    private val nodes = original.documents.associate { it.id to it.nodes<Yaml.Sequence>().mapKeys { entry -> keyMapper(entry.key) } }

    override fun visitSequence(input: Yaml.Sequence, p: ExecutionContext): Yaml.Sequence {
        // do not visit block sequences
        val sequence = super.visitSequence(input, p)
        if (sequence.openingBracketPrefix == null) {
            return sequence
        }

        // do not collapse non-scalar sequences
        val allScalar = sequence.entries.all { it.block is Yaml.Scalar }
        if (!allScalar) {
            return sequence
        }

        val originalSequence = findOriginalSequence()
        if (originalSequence == null || originalSequence.isExpanded()) return sequence

        return sequence
            .mapEntries { it.withPrefix("").withBlock(it.block.withPrefix(" ")) }
            .mapFirstEntry { mapFirstEntry(it, originalSequence) }
            .withOpeningBracketPrefix(originalSequence.openingBracketPrefix)
            .withClosingBracketPrefix(originalSequence.closingBracketPrefix)
    }

    private fun findOriginalSequence(): Yaml.Sequence? {
        val currentSequenceKey = cursor.toYamlPath()
        val parentDocument = cursor.firstEnclosing(Yaml.Document::class.java)
        val originalSequences = parentDocument?.let { nodes[it.id] }
        return originalSequences?.get(keyMapper(currentSequenceKey))
    }

    private fun mapFirstEntry(entry: Yaml.Sequence.Entry, originalSequence: Yaml.Sequence): Yaml.Sequence.Entry {
        val originalFirstEntry = originalSequence.entries.firstOrNull()
        val originalFirstEntryPrefix = originalFirstEntry?.prefix ?: ""
        val originalFirstEntryBlockPrefix = originalFirstEntry?.block?.prefix ?: " "
        return entry
            .withPrefix(originalFirstEntryPrefix)
            .withBlock(entry.block.withPrefix(originalFirstEntryBlockPrefix))
    }

    private fun Yaml.Sequence.isExpanded() =
        this.entries.any { it.prefix.startsWith("\n") } ||
            this.entries.map { it.block }.any { it.prefix.startsWith("\n") }
}
