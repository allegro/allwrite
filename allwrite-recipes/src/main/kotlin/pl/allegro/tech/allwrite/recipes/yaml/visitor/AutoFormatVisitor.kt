package pl.allegro.tech.allwrite.recipes.yaml.visitor

import org.openrewrite.ExecutionContext
import org.openrewrite.Tree
import org.openrewrite.style.GeneralFormatStyle
import org.openrewrite.yaml.YamlIsoVisitor
import org.openrewrite.yaml.format.IndentsVisitor
import org.openrewrite.yaml.format.MinimumViableSpacingVisitor
import org.openrewrite.yaml.format.NormalizeFormatVisitor
import org.openrewrite.yaml.format.NormalizeLineBreaksVisitor
import org.openrewrite.yaml.style.Autodetect
import org.openrewrite.yaml.style.IndentsStyle
import org.openrewrite.yaml.style.YamlDefaultStyles
import org.openrewrite.yaml.tree.Yaml
import org.openrewrite.yaml.tree.Yaml.Documents
import pl.allegro.tech.allwrite.recipes.yaml.YamlPath.Companion.toYamlPath
import pl.allegro.tech.allwrite.recipes.yaml.prefixParts

internal class AutoFormatVisitor(
    private val stopAfter: Tree? = null,
    private val indentsStyle: IndentsStyle? = null,
    private val formatStyle: GeneralFormatStyle? = null,
    private val prefix: String? = null,
) : YamlIsoVisitor<ExecutionContext>() {

    override fun preVisit(tree: Yaml, p: ExecutionContext): Yaml? {
        stopAfterPreVisit()
        if (prefix != null && !cursor.toYamlPath().path.startsWith(prefix)) {
            return tree
        }

        val docs = tree as? Documents ?: cursor.firstEnclosingOrThrow(Documents::class.java)
        val cursor = cursor.parentOrThrow

        var y = NormalizeFormatVisitor<Any>(stopAfter).visit(tree, p, cursor.fork())

        y = CustomMinimumViableSpacingVisitor(stopAfter).visit(y, p, cursor.fork())

        val indentStyle = this.indentsStyle ?: docs.getStyle(IndentsStyle::class.java) ?: Autodetect.tabsAndIndents(docs, YamlDefaultStyles.indents())
        y = CustomIndentsVisitor(indentStyle, stopAfter).visit(y, p, cursor.fork())

        val formatStyle = this.formatStyle ?: docs.getStyle(GeneralFormatStyle::class.java) ?: Autodetect.generalFormat(docs)
        y = NormalizeLineBreaksVisitor<Any>(formatStyle, stopAfter).visit(y, p, cursor.fork())

        return y
    }

    override fun postVisit(tree: Yaml, p: ExecutionContext): Yaml? {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            cursor.putMessageOnFirstEnclosing(Documents::class.java, "stop", true)
        }
        return super.postVisit(tree, p)
    }

    override fun visit(tree: Tree?, p: ExecutionContext): Yaml? {
        if (cursor.getNearestMessage<Any>("stop") != null) {
            return tree as Yaml?
        }
        return super.visit(tree, p)
    }

    internal class CustomMinimumViableSpacingVisitor(
        stopAfter: Tree?,
    ) : MinimumViableSpacingVisitor<ExecutionContext>(stopAfter) {

        override fun visitSequenceEntry(input: Yaml.Sequence.Entry, p: ExecutionContext): Yaml.Sequence.Entry {
            var formatted = super.visitSequenceEntry(input, p)
            val parent = cursor.parent?.getValue<Any>() as? Yaml.Sequence
            if (parent?.openingBracketPrefix != null) {
                // it is a flow sequence, rewrite-yaml adds a line break before each element, but we
                // want to preserve an original indent
                formatted = formatted.withPrefix(input.prefix)
            }

            return formatted
        }

        override fun visitMappingEntry(input: Yaml.Mapping.Entry, p: ExecutionContext): Yaml.Mapping.Entry {
            val entry = super.visitMappingEntry(input, p)
            if (!entry.prefix.contains("\n")) {
                val enclosing = cursor.getParentOrThrow(2).getValue<Yaml>()
                if (enclosing is Yaml.Sequence.Entry) {
                    val mapping = cursor.parentOrThrow.getValue<Yaml.Mapping>()
                    if (mapping.entries.isEmpty() || mapping.entries[0] === input) {
                        return entry
                    }
                }
            }

            // make sure there is a line break before the entry
            val newPrefix = entry.prefixParts().asString(lineBreaksBefore = 1)
            return entry.withPrefix(newPrefix)
        }
    }

    internal class CustomIndentsVisitor(
        indentsStyle: IndentsStyle,
        stopAfter: Tree?,
    ) : IndentsVisitor<ExecutionContext>(indentsStyle, stopAfter) {

        override fun visitMappingEntry(input: Yaml.Mapping.Entry, p: ExecutionContext): Yaml.Mapping.Entry {
            val formatted = super.visitMappingEntry(input, p)
            val parentSequence = cursor.getParent(2)?.getValue<Yaml>() as? Yaml.Sequence.Entry
            val isInsideBlockSequence = parentSequence != null && parentSequence.isDash
            return if (isInsideBlockSequence && formatted.prefix.contains("\n")) {
                // this is a bug in rewrite-yaml which calculates indent incorrectly for entries inside block sequences
                val prefix = formatted.prefix
                val indent = BLOCK_SEQ_ENTRY_INDENT_OFFSET + parentSequence!!.prefix.substringAfterLast("\n").length
                val newPrefix = prefix.replaceAfterLast("\n", " ".repeat(indent))
                formatted.withPrefix(newPrefix)
            } else {
                formatted
            }
        }

        companion object {
            private const val BLOCK_SEQ_ENTRY_INDENT_OFFSET = "- ".length
        }
    }
}
