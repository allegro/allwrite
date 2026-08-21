package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.Parser
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
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Paths
import kotlin.jvm.optionals.getOrNull

private const val PLUGINS_BLOCK: String = "plugins"
private const val ALIAS_METHOD: String = "alias"
private const val KOTLIN_GRADLE_PATH: String = "build.gradle.kts"
private val PLUGIN_ALIAS_MESSAGE: String = "${AddTomlVersionCatalogPlugin::class.java.name}_target"
private val GRADLE_PARSER: GradleParser = GradleParser.builder().build()

private fun pluginsBlockSource(pluginReference: String): String =
    """
    plugins {
        alias(libs.plugins.$pluginReference)
    }
    """.trimIndent()

private fun J.MethodInvocation.pluginAliasStatement(): Statement? {
    val lambda = arguments.singleOrNull() as? J.Lambda ?: return null
    val body = lambda.body as? J.Block ?: return null
    return body.statements.singleOrNull()
}

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
        var pluginAlias: String? = null,
    )

    override fun getInitialValue(ctx: ExecutionContext): Context = Context()

    override fun getScanner(acc: Context): TreeVisitor<*, ExecutionContext> =
        object : TreeVisitor<Tree, ExecutionContext>() {
            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                val document = tree as? Toml.Document ?: return tree
                if (document.isTomlVersionCatalogFile()) {
                    acc.pluginAlias = document.resolvePluginAlias()
                }
                return tree
            }
        }

    override fun getVisitor(context: Context): TreeVisitor<*, ExecutionContext> {
        val pluginAlias = context.pluginAlias ?: return TreeVisitor.noop<Tree, ExecutionContext>()

        val versionCatalogVisitor = VersionCatalogVisitor(pluginAlias)
        val buildFileVisitor = AddVersionCatalogPluginReference(pluginAlias)
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

    private inner class VersionCatalogVisitor(
        private val pluginAlias: String,
    ) : TomlIsoVisitor<ExecutionContext>() {
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
            if (plugin.id != pluginId || plugin.version == PlainVersion(pluginVersion)) return super.visitKeyValue(keyValue, p)

            val entryName = keyValue.stringKey() ?: return super.visitKeyValue(keyValue, p)
            if (entryName != pluginAlias) return super.visitKeyValue(keyValue, p)
            return requestedPlugin().toTomlEntry(entryName).withPrefix(keyValue.prefix)
        }

        override fun visitTable(table: Toml.Table, p: ExecutionContext): Toml.Table {
            val visited = super.visitTable(table, p)
            if (visited.name() != VERSION_CATALOG_TABLE_PLUGINS) return visited

            if (visited.keyValues().any { it.stringKey() == pluginAlias }) return visited

            val newPlugin = requestedPlugin().toTomlEntry(pluginAlias).withPrefix(Space.format("\n"))
            return visited.withValues(visited.values + newPlugin)
        }
    }

    private fun Toml.Document.resolvePluginAlias(): String {
        val pluginEntries = table(VERSION_CATALOG_TABLE_PLUGINS)?.keyValues().orEmpty()
        val requestedAliasEntry = pluginEntries.firstOrNull { it.stringKey() == pluginName }
        val requestedAliasPlugin = requestedAliasEntry?.valueToPlugin()
        require(requestedAliasEntry == null || requestedAliasPlugin?.id == pluginId) {
            "Plugin alias '$pluginName' already refers to another plugin."
        }

        val matchingPluginEntries = TomlVersionCatalog(this).plugins.filter { it.plugin.id == pluginId }
        require(matchingPluginEntries.size <= 1) {
            "Plugin ID '$pluginId' is declared by multiple version catalog aliases."
        }

        return requestedAliasEntry?.stringKey() ?: matchingPluginEntries.singleOrNull()?.keyValue?.stringKey() ?: pluginName
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
        return super.visitCompilationUnit(addPluginsBlockIfMissing(cu, p), p)
    }

    private fun addPluginsBlockIfMissing(cu: K.CompilationUnit, ctx: ExecutionContext): K.CompilationUnit {
        val compilationUnitBody = cu.statements.firstOrNull() as? J.Block ?: return cu
        val existingPluginsBlock = compilationUnitBody.statements
            .filterIsInstance<J.MethodInvocation>()
            .firstOrNull { it.simpleName == PLUGINS_BLOCK }
        if (existingPluginsBlock != null) {
            cursor.putMessage(PLUGIN_ALIAS_MESSAGE, existingPluginsBlock)
            return cu
        }

        val pluginsBlock = parsePluginsBlock(ctx) ?: return cu
        val statementsWithPluginsBlock =
            if (compilationUnitBody.statements.isEmpty()) {
                listOf(pluginsBlock)
            } else {
                val firstStatement = compilationUnitBody.statements.first()
                listOf(pluginsBlock, firstStatement.withPrefix(firstStatement.prefix.withWhitespace("\n\n" + firstStatement.prefix.whitespace))) +
                    compilationUnitBody.statements.drop(1)
            }
        cursor.putMessage(PLUGIN_ALIAS_MESSAGE, pluginsBlock)
        return cu.withStatements(mutableListOf<Statement>(compilationUnitBody.withStatements(statementsWithPluginsBlock)))
    }

    private fun parsePluginsBlock(ctx: ExecutionContext): J.MethodInvocation? {
        val input = Parser.Input(Paths.get(KOTLIN_GRADLE_PATH)) {
            ByteArrayInputStream(pluginsBlockSource(pluginReference).toByteArray(UTF_8))
        }
        val parsed = GRADLE_PARSER.parseInputs(
            listOf(input),
            null,
            ctx,
        ).findFirst().getOrNull() ?: return null
        val compilationUnit = parsed as? K.CompilationUnit ?: return null
        val block = compilationUnit.statements.firstOrNull() as? J.Block ?: return null
        return block.statements.firstOrNull() as? J.MethodInvocation
    }

    private fun parsePluginAlias(ctx: ExecutionContext): Statement? {
        val pluginsBlock = parsePluginsBlock(ctx) ?: return null
        return pluginsBlock.pluginAliasStatement()
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
        return super.visitCompilationUnit(addPluginsBlockIfMissing(cu), p)
    }

    private fun addPluginsBlockIfMissing(cu: G.CompilationUnit): G.CompilationUnit {
        val existingPluginsBlock = cu.statements
            .filterIsInstance<J.MethodInvocation>()
            .firstOrNull { it.simpleName == PLUGINS_BLOCK }
        if (existingPluginsBlock != null) {
            cursor.putMessage(PLUGIN_ALIAS_MESSAGE, existingPluginsBlock)
            return cu
        }

        val pluginsBlock = parsePluginsBlock() ?: return cu
        val statementsWithPluginsBlock =
            if (cu.statements.isEmpty()) {
                listOf(pluginsBlock)
            } else {
                val firstStatement = cu.statements.first()
                listOf(pluginsBlock, firstStatement.withPrefix(firstStatement.prefix.withWhitespace("\n\n" + firstStatement.prefix.whitespace))) +
                    cu.statements.drop(1)
            }
        cursor.putMessage(PLUGIN_ALIAS_MESSAGE, pluginsBlock)
        return cu.withStatements(statementsWithPluginsBlock)
    }

    private fun parsePluginsBlock(): J.MethodInvocation? {
        val parsed = GRADLE_PARSER.parse(pluginsBlockSource(pluginReference)).findFirst().getOrNull() ?: return null
        val compilationUnit = parsed as? G.CompilationUnit ?: return null
        return compilationUnit.statements.firstOrNull() as? J.MethodInvocation
    }

    private fun parsePluginAlias(): Statement? {
        val pluginsBlock = parsePluginsBlock() ?: return null
        return pluginsBlock.pluginAliasStatement()
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
        val visitedBlock = super.visitBlock(block, p)
        if (!isPluginsBlock(block)) return visitedBlock
        if (visitedBlock.statements.any { PluginAliasDetector(pluginReference).containsAlias(it, p) }) return visitedBlock

        val pluginAlias = parsePluginAlias(p) ?: return visitedBlock
        val formattedPluginAlias =
            if (visitedBlock.statements.isEmpty()) {
                pluginAlias
            } else {
                val indent = visitedBlock.statements.last().prefix.whitespace.substringAfterLast('\n')
                pluginAlias.withPrefix(pluginAlias.prefix.withWhitespace("\n$indent"))
            }
        return visitedBlock.withStatements(visitedBlock.statements.toMutableList() + formattedPluginAlias)
    }

    private fun isPluginsBlock(block: J.Block): Boolean {
        val pluginsMethod = cursor.firstEnclosing(J.MethodInvocation::class.java)
        val pluginsLambda = cursor.firstEnclosing(J.Lambda::class.java)
        return pluginsMethod?.simpleName == PLUGINS_BLOCK &&
            pluginsLambda?.body == block &&
            pluginsMethod.arguments.any { it == pluginsLambda }
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
