package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.SourceFile
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Statement
import org.openrewrite.kotlin.tree.K
import pl.allegro.tech.allwrite.recipes.util.DelegatingJVisitor

private const val ID_METHOD: String = "id"
private const val APPLY_METHOD: String = "apply"
private const val PLUGIN_ARGUMENT: String = "plugin"

internal class AppliedPluginDetector(
    private val pluginId: String,
) {
    fun isAppliedIn(sourceFile: SourceFile, ctx: ExecutionContext): Boolean = detectIn(sourceFile, ctx).applied

    internal fun detectIn(sourceFile: SourceFile, ctx: ExecutionContext): DetectionResult {
        if (!sourceFile.isBuildGradleFile()) return DetectionResult(applied = false, hasPluginsBlock = false)

        val detection = Detection(pluginId).also { detection ->
            DelegatingJVisitor(javaVisitor = AppliedPluginVisitor(detection)).visit(sourceFile, ctx)
        }
        return DetectionResult(applied = detection.matched, hasPluginsBlock = detection.pluginsBlockFound)
    }

    internal data class DetectionResult(
        val applied: Boolean,
        val hasPluginsBlock: Boolean,
    )

    private class Detection(
        val pluginId: String,
        var matched: Boolean = false,
        var pluginsBlockFound: Boolean = false,
    ) {
        fun inspect(cursor: Cursor, block: J.Block) {
            if (!cursor.isPluginsBlock(block)) return
            pluginsBlockFound = true
            if (block.statements.any { it.isPluginDeclaration(pluginId) }) matched = true
        }

        fun inspect(method: J.MethodInvocation) {
            if (method.appliesPlugin(pluginId)) {
                matched = true
            }
        }
    }

    private class AppliedPluginVisitor(
        private val detection: Detection,
    ) : JavaIsoVisitor<ExecutionContext>() {
        override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
            detection.inspect(cursor, block)
            return super.visitBlock(block, p)
        }

        override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
            detection.inspect(method)
            return super.visitMethodInvocation(method, p)
        }
    }
}

private fun Statement.isPluginDeclaration(pluginId: String): Boolean =
    when (this) {
        is K.ExpressionStatement -> expression.isPluginDeclaration(pluginId)
        else -> (this as? J)?.isPluginDeclaration(pluginId) == true
    }

private fun J.isPluginDeclaration(pluginId: String): Boolean =
    when (this) {
        is J.Identifier -> simpleName.trim('`') == pluginId
        is J.MethodInvocation ->
            if (simpleName == APPLY_METHOD && select != null) {
                false
            } else {
                (
                    simpleName == ID_METHOD &&
                        select == null &&
                        arguments.singleOrNull()?.let { (it as? J.Literal)?.value as? String } == pluginId
                    ) ||
                    select?.isPluginDeclaration(pluginId) == true
            }
        is J.Return -> expression?.isPluginDeclaration(pluginId) == true
        else -> false
    }

private fun J.MethodInvocation.appliesPlugin(pluginId: String): Boolean =
    select == null &&
        simpleName == APPLY_METHOD &&
        arguments.any { argument ->
            when (argument) {
                is J.Assignment ->
                    argument.variable is J.Identifier &&
                        (argument.variable as J.Identifier).simpleName == PLUGIN_ARGUMENT &&
                        (argument.assignment as? J.Literal)?.value as? String == pluginId
                is G.MapEntry -> (argument.key as? J.Literal)?.value == PLUGIN_ARGUMENT &&
                    (argument.value as? J.Literal)?.value as? String == pluginId
                else -> false
            }
        }
