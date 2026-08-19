package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.gradle.GradleParser
import org.openrewrite.groovy.GroovyIsoVisitor
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Statement
import org.openrewrite.kotlin.KotlinIsoVisitor
import org.openrewrite.kotlin.tree.K
import org.openrewrite.toml.TomlIsoVisitor
import org.openrewrite.toml.tree.Space
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.AllwriteScanningRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.INTERNAL
import pl.allegro.tech.allwrite.recipes.toml.Builders
import pl.allegro.tech.allwrite.recipes.toml.keyValues
import pl.allegro.tech.allwrite.recipes.toml.name
import pl.allegro.tech.allwrite.recipes.toml.stringKey
import pl.allegro.tech.allwrite.recipes.toml.table
import pl.allegro.tech.allwrite.recipes.util.DelegatingJVisitor
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import kotlin.jvm.optionals.getOrNull

private const val PLUGINS_BLOCK: String = "plugins"
private const val ALIAS_METHOD: String = "alias"
private val PLUGIN_ALIAS_MESSAGE: String = "${AddTomlVersionCatalogPlugin::class.java.name}_target"
private val GRADLE_PARSER: GradleParser = GradleParser.builder().build()

internal class AddTomlVersionCatalogPlugin(
    private val pluginName: String,
    private val pluginId: String,
    private val pluginVersion: String,
) : AllwriteScanningRecipe<AddTomlVersionCatalogPlugin.Context>(
    displayName = "Add a plugin to a version catalog",
    description = "Adds or updates a plugin in gradle/libs.versions.toml and applies its catalog alias to Gradle build files.",
    visibility = INTERNAL,
) {

    internal data class Context(
        var hasVersionCatalog: Boolean = false,
    )

    override fun getInitialValue(ctx: ExecutionContext): Context = Context()

    override fun getScanner(acc: Context): TreeVisitor<*, ExecutionContext> =
        object : TreeVisitor<Tree, ExecutionContext>() {
            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                val document = tree as? Toml.Document ?: return tree
                if (document.isTomlVersionCatalogFile()) {
                    acc.hasVersionCatalog = true
                }
                return tree
            }
        }

    override fun getVisitor(context: Context): TreeVisitor<*, ExecutionContext> {
        if (!context.hasVersionCatalog) return TreeVisitor.noop<Tree, ExecutionContext>()

        val versionCatalogVisitor = Visitor()
        val buildFileVisitor = AddVersionCatalogPluginReference(pluginName)
        return object : TreeVisitor<Tree, ExecutionContext>() {
            override fun isAcceptable(sourceFile: SourceFile, ctx: ExecutionContext): Boolean =
                sourceFile.isTomlVersionCatalogFile() || sourceFile.isBuildGradleFile()

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                if (tree is Toml.Document && tree.isTomlVersionCatalogFile()) {
                    return versionCatalogVisitor.visitNonNull(tree, p)
                }
                if (tree is SourceFile && tree.isBuildGradleFile()) {
                    return buildFileVisitor.visit(tree, p)
                }
                return tree
            }
        }
    }

    private inner class Visitor : TomlIsoVisitor<ExecutionContext>() {
        override fun visitDocument(document: Toml.Document, p: ExecutionContext): Toml.Document {
            val documentWithPlugins =
                if (document.table(VERSION_CATALOG_TABLE_PLUGINS) == null) {
                    val prefix = if (document.values.isEmpty()) Space.EMPTY else Space.format("\n\n")
                    document.withValues(document.values + Builders.emptyTable().withPrefix(prefix).withName(Builders.id(VERSION_CATALOG_TABLE_PLUGINS)))
                } else {
                    document
                }

            return super.visitDocument(documentWithPlugins, p)
        }

        override fun visitKeyValue(keyValue: Toml.KeyValue, p: ExecutionContext): Toml.KeyValue =
            when (cursor.firstEnclosing(Toml.Table::class.java)?.name()) {
                VERSION_CATALOG_TABLE_PLUGINS -> visitPlugin(keyValue, p)
                else -> super.visitKeyValue(keyValue, p)
            }

        private fun visitPlugin(keyValue: Toml.KeyValue, p: ExecutionContext): Toml.KeyValue {
            val plugin = keyValue.valueToPlugin() ?: return super.visitKeyValue(keyValue, p)
            if (plugin.id != pluginId) return super.visitKeyValue(keyValue, p)

            val entryName = keyValue.stringKey() ?: return super.visitKeyValue(keyValue, p)
            return requestedPlugin().toTomlEntry(entryName).withPrefix(keyValue.prefix)
        }

        override fun visitTable(table: Toml.Table, p: ExecutionContext): Toml.Table {
            val visited = super.visitTable(table, p)
            if (visited.name() != VERSION_CATALOG_TABLE_PLUGINS) return visited

            val hasPlugin = visited.keyValues().any { it.valueToPlugin()?.id == pluginId }
            if (hasPlugin || visited.keyValues().any { it.stringKey() == pluginName }) return visited

            val newPlugin = requestedPlugin().toTomlEntry(pluginName).withPrefix(Space.format("\n"))
            return visited.withValues(visited.values + newPlugin)
        }
    }

    private fun requestedPlugin(): Plugin = Plugin(pluginId, PlainVersion(pluginVersion))
}

private class AddVersionCatalogPluginReference(
    pluginName: String,
) : DelegatingJVisitor(
    javaVisitor = JavaIsoVisitor(),
    kotlinVisitor = KotlinAddVersionCatalogPluginReference(pluginName),
    groovyVisitor = GroovyAddVersionCatalogPluginReference(pluginName),
)

private class KotlinAddVersionCatalogPluginReference(
    pluginName: String,
) : KotlinIsoVisitor<ExecutionContext>() {
    private val pluginReference = pluginName.toVersionCatalogReference()

    override fun visitCompilationUnit(cu: K.CompilationUnit, p: ExecutionContext): K.CompilationUnit {
        if (!cu.sourcePath.toString().endsWith(".gradle.kts")) return cu
        return super.visitCompilationUnit(withPlugins(cu, p), p)
    }

    private fun withPlugins(cu: K.CompilationUnit, ctx: ExecutionContext): K.CompilationUnit {
        val block = cu.statements.firstOrNull() as? J.Block ?: return cu
        val target = block.statements.filterIsInstance<J.MethodInvocation>().firstOrNull { it.simpleName == PLUGINS_BLOCK }
        if (target != null) {
            cursor.putMessage(PLUGIN_ALIAS_MESSAGE, target)
            return cu
        }

        val plugins = parsePlugins(ctx) ?: return cu
        val statements =
            if (block.statements.isEmpty()) {
                listOf(plugins)
            } else {
                val first = block.statements.first()
                listOf(plugins, first.withPrefix(first.prefix.withWhitespace("\n\n" + first.prefix.whitespace))) + block.statements.drop(1)
            }
        cursor.putMessage(PLUGIN_ALIAS_MESSAGE, plugins)
        return cu.withStatements(mutableListOf<Statement>(block.withStatements(statements)))
    }

    private fun parsePlugins(ctx: ExecutionContext): J.MethodInvocation? =
        GRADLE_PARSER.parseInputs(
            listOf(
                org.openrewrite.Parser.Input(Paths.get("build.gradle.kts")) {
                    ByteArrayInputStream(
                        "plugins {\n    alias(libs.plugins.$pluginReference)\n}\n".toByteArray(StandardCharsets.UTF_8),
                    )
                },
            ),
            null,
            ctx,
        )
            .findFirst()
            .getOrNull()
            ?.let { it as? K.CompilationUnit }
            ?.statements
            ?.firstOrNull()
            ?.let { it as? J.Block }
            ?.statements
            ?.firstOrNull()
            ?.let { it as? J.MethodInvocation }

    private fun parsePluginAlias(ctx: ExecutionContext): Statement? {
        val plugins = parsePlugins(ctx) ?: return null
        val lambda = plugins.arguments.filterIsInstance<J.Lambda>().firstOrNull() ?: return null
        val body = lambda.body as? J.Block ?: return null
        return body.statements.firstOrNull()
    }

    override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation =
        if (cursor.getNearestMessage<Any>(PLUGIN_ALIAS_MESSAGE) == method) {
            PluginBlockVisitor(pluginReference, ::parsePluginAlias).visit(method, p, cursor.parent!!) as J.MethodInvocation
        } else {
            super.visitMethodInvocation(method, p)
        }
}

private class GroovyAddVersionCatalogPluginReference(
    pluginName: String,
) : GroovyIsoVisitor<ExecutionContext>() {
    private val pluginReference = pluginName.toVersionCatalogReference()

    override fun visitCompilationUnit(cu: G.CompilationUnit, p: ExecutionContext): G.CompilationUnit {
        if (!cu.sourcePath.toString().endsWith(".gradle")) return cu
        return super.visitCompilationUnit(withPlugins(cu), p)
    }

    private fun withPlugins(cu: G.CompilationUnit): G.CompilationUnit {
        val target = cu.statements.filterIsInstance<J.MethodInvocation>().firstOrNull { it.simpleName == PLUGINS_BLOCK }
        if (target != null) {
            cursor.putMessage(PLUGIN_ALIAS_MESSAGE, target)
            return cu
        }

        val plugins = parsePlugins() ?: return cu
        val statements =
            if (cu.statements.isEmpty()) {
                listOf(plugins)
            } else {
                val first = cu.statements.first()
                listOf(plugins, first.withPrefix(first.prefix.withWhitespace("\n\n" + first.prefix.whitespace))) + cu.statements.drop(1)
            }
        cursor.putMessage(PLUGIN_ALIAS_MESSAGE, plugins)
        return cu.withStatements(statements)
    }

    private fun parsePlugins(): J.MethodInvocation? =
        GRADLE_PARSER
            .parse("plugins {\n    alias(libs.plugins.$pluginReference)\n}\n")
            .findFirst()
            .getOrNull()
            ?.let { it as? G.CompilationUnit }
            ?.statements
            ?.firstOrNull()
            ?.let { it as? J.MethodInvocation }

    private fun parsePluginAlias(): Statement? {
        val plugins = parsePlugins() ?: return null
        val lambda = plugins.arguments.filterIsInstance<J.Lambda>().firstOrNull() ?: return null
        val body = lambda.body as? J.Block ?: return null
        return body.statements.firstOrNull()
    }

    override fun visitStatement(statement: Statement, p: ExecutionContext): Statement =
        if (cursor.getNearestMessage<Any>(PLUGIN_ALIAS_MESSAGE) == statement) {
            PluginBlockVisitor(pluginReference, { _ -> parsePluginAlias() }).visit(statement, p, cursor.parent!!) as Statement
        } else {
            super.visitStatement(statement, p)
        }
}

private class PluginBlockVisitor(
    private val pluginReference: String,
    private val parsePluginAlias: (ExecutionContext) -> Statement?,
) : JavaIsoVisitor<ExecutionContext>() {
    override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
        val result = super.visitBlock(block, p)
        val methodInvocation = cursor.firstEnclosing(J.MethodInvocation::class.java)
        val lambda = cursor.firstEnclosing(J.Lambda::class.java)
        if (methodInvocation?.simpleName != PLUGINS_BLOCK || lambda?.body != block || methodInvocation.arguments.none { it == lambda }) return result
        if (result.statements.any { statement -> PluginAliasDetector(pluginReference).containsAlias(statement, p) }) return result

        val pluginAlias = parsePluginAlias(p) ?: return result
        val formattedPluginAlias =
            if (result.statements.isEmpty()) {
                pluginAlias
            } else {
                val indent = result.statements.last().prefix.whitespace.substringAfterLast('\n')
                pluginAlias.withPrefix(pluginAlias.prefix.withWhitespace("\n$indent"))
            }
        return result.withStatements(result.statements.toMutableList() + formattedPluginAlias)
    }
}

private class PluginAliasDetector(
    private val pluginReference: String,
) : JavaIsoVisitor<ExecutionContext>() {
    private var found: Boolean = false

    fun containsAlias(statement: Statement, p: ExecutionContext): Boolean {
        visit(statement, p)
        return found
    }

    override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
        if (method.simpleName == ALIAS_METHOD &&
            method.arguments.singleOrNull()?.toString() == "libs.plugins.$pluginReference"
        ) {
            found = true
        }
        return super.visitMethodInvocation(method, p)
    }
}
