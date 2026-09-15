package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Statement
import org.openrewrite.text.PlainTextParser
import org.openrewrite.toml.TomlIsoVisitor
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.AllwriteScanningRecipe
import pl.allegro.tech.allwrite.recipes.toml.asString
import pl.allegro.tech.allwrite.recipes.toml.name
import pl.allegro.tech.allwrite.recipes.toml.stringKey
import pl.allegro.tech.allwrite.recipes.util.DelegatingJVisitor
import java.nio.file.Path

internal class RemoveTomlVersionCatalogLibrary(
    private val groupId: String,
    private val artifactId: String,
    private val applyToModulesWithPluginId: String? = null,
) : AllwriteScanningRecipe<RemoveTomlVersionCatalogLibrary.Context>(
    displayName = "Remove a library from Gradle build files and its TOML version catalog entry",
    description = "Removes matching library usages from Gradle build files and, when present, the corresponding library alias and unused version from gradle/libs.versions.toml.",
) {

    internal data class Context(
        var targetAliases: Set<String> = emptySet(),
        var targetVersionKeys: Set<String> = emptySet(),
        var versionCatalogVersionKeys: Set<String> = emptySet(),
        var targetAliasIsUsedInBundle: Boolean = false,
        val buildFiles: MutableMap<Path, SourceFile> = HashMap(),
        val gatedBuildFiles: MutableSet<Path> = HashSet(),
        val gradleVersionAccessorReferences: MutableSet<String> = HashSet(),
    )

    override fun getInitialValue(ctx: ExecutionContext): Context = Context()

    override fun getScanner(acc: Context): TreeVisitor<*, ExecutionContext> =
        object : TreeVisitor<Tree, ExecutionContext>() {
            private val appliedPluginDetector = applyToModulesWithPluginId?.let(::AppliedPluginDetector)
            private val versionAccessor = Regex("""libs\.versions\.([A-Za-z0-9_.-]+)""")

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                val sourceFile = tree as? SourceFile ?: return tree
                if (sourceFile is Toml.Document && sourceFile.isTomlVersionCatalogFile()) {
                    val catalog = TomlVersionCatalog(sourceFile)
                    val matchingLibraries = catalog.libraries
                        .filter { it.library.group == groupId && it.library.name == artifactId }
                    val aliases = matchingLibraries
                        .mapNotNull { it.keyValue.stringKey() }
                        .toSet()
                    acc.targetAliases = aliases
                    acc.targetVersionKeys = matchingLibraries
                        .mapNotNull { (it.library.version as? VersionRef)?.ref }
                        .toSet()
                    acc.versionCatalogVersionKeys = catalog.versionKeys
                    acc.targetAliasIsUsedInBundle = BundleAliasDetector(aliases).containsAlias(sourceFile, p)
                }
                if (sourceFile.isBuildGradleFile()) {
                    acc.buildFiles[sourceFile.sourcePath] = sourceFile
                    versionAccessor.findAll(PlainTextParser.convert(sourceFile).text)
                        .map { it.groupValues[1] }
                        .forEach(acc.gradleVersionAccessorReferences::add)
                    if (appliedPluginDetector?.isAppliedIn(sourceFile, p) == true) {
                        acc.gatedBuildFiles.add(sourceFile.sourcePath)
                    }
                }
                return tree
            }
        }

    override fun getVisitor(context: Context): TreeVisitor<*, ExecutionContext> {
        if (context.targetAliasIsUsedInBundle) {
            return TreeVisitor.noop<Tree, ExecutionContext>()
        }

        val referenceMatcher = GradleDependencyReferenceMatcher(context.targetAliases, groupId, artifactId)
        val gradleUsedVersionKeys = context.gradleVersionAccessorReferences
            .mapNotNull { it.toVersionCatalogVersionKey(context.versionCatalogVersionKeys) }
            .toSet()
        return object : TreeVisitor<Tree, ExecutionContext>() {
            private var resolved = false
            private var buildFilesToEdit: Set<Path> = emptySet()
            private var removeCatalogEntry = false

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                if (!resolved) {
                    val referencingBuildFiles = context.buildFiles
                        .filterValues { referenceMatcher.isReferencedIn(it, p) }
                        .keys
                    buildFilesToEdit =
                        if (applyToModulesWithPluginId == null) {
                            referencingBuildFiles
                        } else {
                            referencingBuildFiles.intersect(context.gatedBuildFiles)
                        }
                    removeCatalogEntry =
                        if (context.targetAliases.isEmpty()) {
                            false
                        } else {
                            applyToModulesWithPluginId == null ||
                                referencingBuildFiles.all { it in context.gatedBuildFiles }
                        }
                    resolved = true
                }

                if (tree is SourceFile && tree.isBuildGradleFile() && tree.sourcePath in buildFilesToEdit) {
                    return referenceMatcher.removeFrom(tree, p)
                }
                if (removeCatalogEntry && tree is Toml.Document && tree.isTomlVersionCatalogFile()) {
                    val withoutLibrary = TomlVersionCatalogLibraryRemover(context.targetAliases).visitDocument(tree, p)
                    return withoutLibrary.removeUnusedVersionEntries(
                        gradleUsedVersionKeys = gradleUsedVersionKeys,
                        versionKeysToCleanUp = context.targetVersionKeys,
                    )
                }
                return tree
            }
        }
    }
}

private class BundleAliasDetector(
    private val aliases: Set<String>,
) : TomlIsoVisitor<ExecutionContext>() {
    private var found = false

    fun containsAlias(document: Toml.Document, ctx: ExecutionContext): Boolean {
        visit(document, ctx)
        return found
    }

    override fun visitLiteral(literal: Toml.Literal, p: ExecutionContext): Toml.Literal {
        if (cursor.firstEnclosing(Toml.Table::class.java)?.name() == VERSION_CATALOG_TABLE_BUNDLES &&
            literal.asString() in aliases
        ) {
            found = true
        }
        return super.visitLiteral(literal, p)
    }
}

private class TomlVersionCatalogLibraryRemover(
    private val aliases: Set<String>,
) : TomlIsoVisitor<ExecutionContext>() {
    override fun visitTable(table: Toml.Table, p: ExecutionContext): Toml.Table {
        val visited = super.visitTable(table, p)
        if (visited.name() != VERSION_CATALOG_TABLE_LIBS) return visited
        val values = visited.values.filterNot { value ->
            (value as? Toml.KeyValue)?.stringKey() in aliases
        }
        return if (values.size == visited.values.size) visited else visited.withValues(values)
    }
}

private class GradleDependencyReferenceMatcher(
    private val aliases: Set<String>,
    private val groupId: String,
    private val artifactId: String,
) {
    fun isReferencedIn(sourceFile: SourceFile, ctx: ExecutionContext): Boolean {
        val detection = DependencyReferenceDetection()
        dependencyVisitor(detection, remove = false).visit(sourceFile, ctx)
        return detection.found
    }

    fun removeFrom(sourceFile: SourceFile, ctx: ExecutionContext): SourceFile =
        dependencyVisitor(DependencyReferenceDetection(), remove = true).visit(sourceFile, ctx) as SourceFile

    private fun dependencyVisitor(detection: DependencyReferenceDetection, remove: Boolean): TreeVisitor<*, ExecutionContext> =
        DelegatingJVisitor(
            javaVisitor = GradleDependencyReferenceVisitor(
                aliases = aliases,
                groupId = groupId,
                artifactId = artifactId,
                detection = detection,
                remove = remove,
            ),
        )
}

private class DependencyReferenceDetection(
    var found: Boolean = false,
)

private class GradleDependencyReferenceVisitor(
    private val aliases: Set<String>,
    private val groupId: String,
    private val artifactId: String,
    private val detection: DependencyReferenceDetection,
    private val remove: Boolean,
) : JavaIsoVisitor<ExecutionContext>() {
    override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
        val isDependenciesBlock = cursor.isDependenciesBlock(block)
        val visited = super.visitBlock(block, p)
        if (!isDependenciesBlock) return visited

        val statements = visited.statements.filterNot { statement ->
            val matches = statement.matchesTarget()
            if (matches) detection.found = true
            remove && matches
        }
        return if (remove && statements.size != visited.statements.size) {
            visited.withStatements(statements.toMutableList())
        } else {
            visited
        }
    }

    private fun Statement.matchesTarget(): Boolean {
        val invocation = when (this) {
            is J.MethodInvocation -> this
            is J.Return -> expression as? J.MethodInvocation
            else -> null
        } ?: return false
        return invocation.arguments.firstOrNull()?.matchesTarget() == true
    }

    private fun J.matchesTarget(): Boolean =
        when (this) {
            is J.FieldAccess -> aliases.any { toString() == "libs.${it.toVersionCatalogReference()}" }
            is J.Literal -> (value as? String)?.let(::matchesCoordinates) == true
            is J.MethodInvocation ->
                if (simpleName == "platform" || simpleName == "enforcedPlatform") {
                    arguments.firstOrNull()?.matchesTarget() == true
                } else {
                    select?.matchesTarget() == true && arguments.isEmpty()
                }
            else -> false
        }

    private fun matchesCoordinates(coordinates: String): Boolean {
        val parts = coordinates.split(":")
        return parts.size >= 2 && parts[0] == groupId && parts[1] == artifactId
    }
}

private fun Cursor.isDependenciesBlock(block: J.Block): Boolean {
    val dependenciesMethod = firstEnclosing(J.MethodInvocation::class.java)
    val dependenciesLambda = firstEnclosing(J.Lambda::class.java)
    return dependenciesMethod?.simpleName == "dependencies" &&
        dependenciesLambda?.body == block &&
        dependenciesMethod.arguments.any { it == dependenciesLambda }
}

private fun String.toVersionCatalogVersionKey(versionKeys: Set<String>): String? =
    versionKeys
        .filter { versionKey ->
            val accessor = versionKey.toVersionCatalogReference()
            this == accessor || startsWith("$accessor.")
        }
        .maxByOrNull { it.length }
