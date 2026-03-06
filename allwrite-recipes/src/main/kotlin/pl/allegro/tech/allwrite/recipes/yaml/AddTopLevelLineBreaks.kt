package pl.allegro.tech.allwrite.recipes.yaml

import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.TreeVisitor
import org.openrewrite.yaml.YamlIsoVisitor
import org.openrewrite.yaml.tree.Yaml

public class AddTopLevelLineBreaks : Recipe() {

    override fun getDisplayName(): String = "Adds conventional line breaks to the top level entries"

    override fun getDescription(): String = "Adds  conventional line breaks so that top level entries had an empty lines between."

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> = Visitor

    internal object Visitor : YamlIsoVisitor<ExecutionContext>() {

        override fun visitDocument(document: Yaml.Document, p: ExecutionContext): Yaml.Document {
            val block = document.block as? Yaml.Mapping ?: return document
            val newEntries = block.entries.mapIndexed { index, entry -> visitTopLevelEntry(index, entry, document.isExplicit) }
            return document.withBlock(block.withEntries(newEntries))
        }

        private fun visitTopLevelEntry(index: Int, entry: Yaml.Mapping.Entry, hasExplicitDocumentSeparator: Boolean): Yaml.Mapping.Entry {
            // first entry in the top level mapping should have no line break or a single line break if it follows document separator
            // if it has any comments, they are treated as a prefix for Yaml.Document, not this entry.
            // For other entries, make sure there is a blank line in between
            val requiredLineBreak = when {
                index == 0 && hasExplicitDocumentSeparator -> 1
                index == 0 -> 0
                else -> 2
            }
            val newPrefix = entry.prefixParts().asString(lineBreaksBefore = requiredLineBreak)
            return entry.withPrefix(newPrefix)
        }
    }
}
