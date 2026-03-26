package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.Preconditions
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.properties.DeleteProperty
import org.openrewrite.properties.tree.Properties
import org.openrewrite.yaml.tree.Yaml
import pl.allegro.tech.allwrite.spi.AllwriteRecipe
import pl.allegro.tech.allwrite.spi.RecipeVisibility.INTERNAL
import pl.allegro.tech.allwrite.recipes.yaml.DeleteProperty as DeleteYamlProperty

public class DeleteSpringProperty(public val propertyKey: String) : AllwriteRecipe(
    displayName = "Remove spring property from yaml and properties files",
    visibility = INTERNAL
) {
    override fun getVisitor(): TreeVisitor<*, ExecutionContext> = Preconditions.check(findSpringPropertyFiles(), Visitor(propertyKey))

    internal class Visitor(val propertyKey: String) : TreeVisitor<Tree, ExecutionContext>() {

        override fun visit(tree: Tree?, p: ExecutionContext): Tree? = when (tree) {
            is Yaml -> DeleteYamlProperty(propertyKey, false, true, null).visitor.visit(tree, p)
            is Properties -> DeleteProperty(propertyKey, true).visitor.visit(tree, p)
            else -> tree
        }
    }
}
