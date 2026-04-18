package pl.allegro.tech.allwrite.runtime.fake

import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor

open class FakeRecipe(
    private val id: String = "pl.allegro.tech.allwrite.recipes.fake",
    private val displayName: String = "Fake recipe",
    private val description: String = "Fake recipe description.",
    private val tags: Set<String> = emptySet(),
) : Recipe() {

    val visitedSourceFiles = mutableListOf<SourceFile>()

    override fun getName(): String = id

    override fun getDisplayName(): String = displayName

    override fun getDescription(): String = description

    override fun getTags(): Set<String> = tags

    override fun toString(): String = id

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        object : TreeVisitor<Tree, ExecutionContext>() {

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                if (tree is SourceFile) {
                    println("Visiting file: ${tree.sourcePath}")
                    visitedSourceFiles.add(tree)
                }
                return tree
            }
        }
}
