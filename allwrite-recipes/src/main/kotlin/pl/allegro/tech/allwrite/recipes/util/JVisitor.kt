package pl.allegro.tech.allwrite.recipes.util

import org.openrewrite.ExecutionContext
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.groovy.GroovyVisitor
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.kotlin.KotlinVisitor
import org.openrewrite.kotlin.tree.K

internal abstract class JVisitor : TreeVisitor<Tree, ExecutionContext>() {

    override fun visit(tree: Tree?, p: ExecutionContext): Tree? =
        when (tree) {
            is K -> visitKotlin(tree, p)
            is G -> visitGroovy(tree, p)
            is J -> visitJava(tree, p)
            else -> tree
        }

    abstract fun visitJava(tree: J, p: ExecutionContext): Tree?

    open fun visitKotlin(tree: K, p: ExecutionContext): Tree? = visitJava(tree, p)
    open fun visitGroovy(tree: G, p: ExecutionContext): Tree? = visitJava(tree, p)
}

internal inline fun <T : Tree, C : ExecutionContext, reified V : TreeVisitor<T, C>> TreeVisitor<T, C>.adaptTo(): V = adapt(V::class.java)

internal open class DelegatingJVisitor(
    private val javaVisitor: JavaVisitor<ExecutionContext>,
    private val kotlinVisitor: KotlinVisitor<ExecutionContext> = javaVisitor.adaptTo<J, ExecutionContext, KotlinVisitor<ExecutionContext>>(),
    private val groovyVisitor: GroovyVisitor<ExecutionContext> = javaVisitor.adaptTo<J, ExecutionContext, GroovyVisitor<ExecutionContext>>(),
) : JVisitor() {

    override fun visitJava(tree: J, p: ExecutionContext): Tree? = javaVisitor.visit(tree, p)

    override fun visitKotlin(tree: K, p: ExecutionContext): Tree? = kotlinVisitor.visit(tree, p)

    override fun visitGroovy(tree: G, p: ExecutionContext): Tree? = groovyVisitor.visit(tree, p)
}
