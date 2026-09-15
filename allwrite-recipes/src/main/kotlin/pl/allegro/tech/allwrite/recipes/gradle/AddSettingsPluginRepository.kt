package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import pl.allegro.tech.allwrite.AllwriteScanningRecipe
import org.openrewrite.gradle.plugins.AddSettingsPluginRepository as OpenRewriteAddSettingsPluginRepository

internal class AddSettingsPluginRepository(
    private val type: String,
    private val url: String? = null,
    private val applyToModulesWithPluginId: String? = null,
) : AllwriteScanningRecipe<AddSettingsPluginRepository.Context>(
    displayName = "Add a Gradle settings repository",
    description = "Adds a Gradle settings repository, optionally only when a configured plugin is applied.",
) {
    internal data class Context(
        var pluginApplied: Boolean = false,
    )

    override fun getInitialValue(ctx: ExecutionContext): Context = Context()

    override fun getScanner(acc: Context): TreeVisitor<*, ExecutionContext> =
        object : TreeVisitor<Tree, ExecutionContext>() {
            private val appliedPluginDetector = applyToModulesWithPluginId?.let(::AppliedPluginDetector)

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                val sourceFile = tree as? SourceFile ?: return tree
                if (appliedPluginDetector?.isAppliedIn(sourceFile, p) == true) {
                    acc.pluginApplied = true
                }
                return tree
            }
        }

    override fun getVisitor(context: Context): TreeVisitor<*, ExecutionContext> {
        if (applyToModulesWithPluginId != null && !context.pluginApplied) {
            return TreeVisitor.noop<Tree, ExecutionContext>()
        }
        return OpenRewriteAddSettingsPluginRepository(type, url).visitor
    }
}
