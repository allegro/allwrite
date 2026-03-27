package pl.allegro.tech.allwrite.runtime

import org.openrewrite.ExecutionContext
import org.openrewrite.Tree
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J

/**
 * Adapts tree produces by OpenRewrite's `JavaParser` to adapt to the changes available in the later versions of
 * java:
 * - unnamed variables in lambda arguments ([JEP 456](https://openjdk.org/jeps/456))
 */
internal class Java22TreeAdapterVisitor : JavaIsoVisitor<ExecutionContext>() {

    override fun visit(tree: Tree?, p: ExecutionContext): J? {
        val result = UnnamedLambdaArgumentsVisitor().visit(tree, p)
        return result
    }
}

private class UnnamedLambdaArgumentsVisitor : JavaIsoVisitor<ExecutionContext>() {
    override fun visitVariableDeclarations(multiVariable: J.VariableDeclarations, p: ExecutionContext): J.VariableDeclarations {
        val variable = super.visitVariableDeclarations(multiVariable, p)
        val parent = cursor.firstEnclosing(J.Lambda::class.java) ?: return variable
        if (multiVariable !in parent.parameters.parameters) return variable

        val unnamed = multiVariable.variables.find { it.simpleName.isEmpty() }
        if (unnamed == null) return variable

        val newVariables = multiVariable.variables.map { v ->
            if (v != unnamed) v else unnamed.let { it.withName(it.name.withSimpleName("_")) }
        }
        return multiVariable.withVariables(newVariables)
    }
}

