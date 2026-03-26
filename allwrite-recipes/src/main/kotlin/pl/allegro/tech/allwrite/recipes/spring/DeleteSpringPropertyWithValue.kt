package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.Preconditions
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.properties.DeleteProperty
import org.openrewrite.properties.tree.Properties
import org.openrewrite.yaml.search.FindProperty
import org.openrewrite.yaml.tree.Yaml.Documents
import pl.allegro.tech.allwrite.spi.AllwriteRecipe
import pl.allegro.tech.allwrite.spi.RecipeVisibility.INTERNAL
import pl.allegro.tech.allwrite.recipes.util.Find
import pl.allegro.tech.allwrite.recipes.yaml.DeleteProperty as DeleteYamlProperty

public class DeleteSpringPropertyWithValue(
    @Option(
        displayName = "Property key",
        description = "The property key to delete.",
        example = "management.endpoint.configprops"
    )
    public var propertyKey: String,
    @Option(
        displayName = "Property value",
        description = "The exact value the property should have to be deleted",
        example = "true"
    )
    public val propertyValue: String
) : AllwriteRecipe(
    displayName = "Delete Spring property having specific value",
    visibility = INTERNAL
) {
    override fun getVisitor(): TreeVisitor<*, ExecutionContext> = Visitor(propertyKey, propertyValue)

    internal class Visitor(
        private val propertyKey: String,
        private val propertyValue: String?,
    ) : TreeVisitor<Tree, ExecutionContext>() {
        override fun isAcceptable(sourceFile: SourceFile, ctx: ExecutionContext): Boolean {
            return sourceFile is Documents || sourceFile is Properties.File
        }

        override fun visit(tree: Tree?, ctx: ExecutionContext): Tree? {
            var t = tree
            if (t is Documents) {
                t = Preconditions.check(
                    FindProperty(propertyKey, true, propertyValue),
                    DeleteYamlProperty(propertyKey, false, true, null).visitor
                ).visitNonNull(t, ctx)
            } else if (t is Properties.File) {
                t = Preconditions.check(
                    Find(
                        pattern = "${propertyKey}\\s*=\\s*${propertyValue}",
                        regex = true
                    ),
                    DeleteProperty(propertyKey, true).visitor
                ).visitNonNull(t, ctx)
            }
            return t
        }
    }
}
