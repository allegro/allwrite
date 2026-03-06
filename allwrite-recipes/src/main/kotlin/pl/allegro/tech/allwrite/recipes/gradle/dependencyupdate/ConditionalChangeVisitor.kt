package pl.allegro.tech.allwrite.recipes.gradle.dependencyupdate

import org.openrewrite.ExecutionContext
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.marker.SearchResult

internal class ConditionalChangeVisitor : TreeVisitor<Tree, ExecutionContext>() {

    companion object {
        private const val MAIN_RECIPE_MADE_CHANGE_KEY = "mainRecipeMadeChanges"

        fun mainRecipeMadeChange(p: ExecutionContext) {
            p.putMessage(MAIN_RECIPE_MADE_CHANGE_KEY, true)
        }
    }

    override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
        if (p.getMessage<Boolean>(MAIN_RECIPE_MADE_CHANGE_KEY) == true) {
            return SearchResult.found(tree)
        }
        return tree
    }
}
