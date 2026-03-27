package pl.allegro.tech.allwrite.runtime.fake

import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor

open class FakeThrowingRecipe(
    private val id: String = "pl.allegro.tech.allwrite.recipes.throwing",
    private val displayName: String = "Exception-throwing recipe",
    private val description: String = "Exception-throwing recipe description.",
    private val tags: Set<String> = emptySet()
) : Recipe() {

    override fun getName(): String = id

    override fun getDisplayName(): String = displayName

    override fun getDescription(): String = description

    override fun getTags(): Set<String> = tags

    override fun toString(): String = id

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        object : TreeVisitor<Tree, ExecutionContext>() {

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                throw IllegalStateException("This recipe always throws an exception")
            }
        }
}
