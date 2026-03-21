package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.internal.NameCaseConvention
import org.openrewrite.internal.NameCaseConvention.LOWER_CAMEL
import org.openrewrite.internal.NameCaseConvention.LOWER_HYPHEN
import org.openrewrite.properties.tree.Properties
import org.openrewrite.yaml.tree.Yaml
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.INTERNAL
import pl.allegro.tech.allwrite.recipes.util.FindAndReplace
import java.util.regex.Pattern
import org.openrewrite.java.spring.ChangeSpringPropertyKey as DefaultChangeSpringPropertyKey

/**
 * Extension of [org.openrewrite.java.spring.ChangeSpringPropertyKey] to also run find and replace
 * for files beyond yaml and properties
 */
public class ChangeSpringPropertyKey(
    @Option(displayName = "Old property key", description = "The property key to rename. Supports glob", example = "management.metrics.binders.*.enabled")
    public val oldKey: String,
    @Option(displayName = "New property key", description = "The new name for the property key.", example = "management.metrics.enable.process.files")
    public val newKey: String
) : AllwriteRecipe(
    displayName = "Change a key of spring application property",
    visibility = INTERNAL
) {
    override fun getVisitor(): TreeVisitor<*, ExecutionContext> = Visitor(oldKey, newKey)

    private class Visitor(
        private val find: String,
        private val replace: String,
    ) : TreeVisitor<Tree, ExecutionContext>() {

        override fun visit(tree: Tree?, context: ExecutionContext): Tree? {
            return when (tree) {
                // delegate yaml and properties to openrewrite-spring
                is Yaml.Documents,
                is Properties.File -> {
                    return DefaultChangeSpringPropertyKey(find, replace, null).visitor.visit(tree, context)
                }

                // try to replace exact match in other files (e.g. markdown
                else -> FindAndReplace(
                    find = "${find.format(LOWER_HYPHEN)}|${find.format(LOWER_CAMEL)}",
                    replace = replace,
                    regex = true,
                    caseSensitive = true,
                    multiline = false,
                    dotAll = false,
                    filePattern = null,
                    plaintextOnly = false
                ).visitor.visit(tree, context)
            }
        }

        private fun String.format(convention: NameCaseConvention) = Pattern.quote(convention.format(this))
    }
}
