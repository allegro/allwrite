package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.internal.StringUtils
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Statement
import org.openrewrite.toml.TomlIsoVisitor
import org.openrewrite.toml.tree.Space
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.AllwriteScanningRecipe
import pl.allegro.tech.allwrite.recipes.toml.name
import pl.allegro.tech.allwrite.recipes.toml.stringKey
import pl.allegro.tech.allwrite.recipes.util.DelegatingJVisitor
import java.nio.file.Path

internal class RemoveLibraryVersionRefs(
    private val pluginName: String,
    private val applyToModulesWithPluginId: String? = null,
) : AllwriteScanningRecipe<RemoveLibraryVersionRefs.Context>(
    displayName = "Remove library version references",
    description = "Removes version.ref from library aliases equal to pluginName or matching pluginName-* in gradle/libs.versions.toml.",
) {
    internal data class Context(
        var matchingAliases: Set<String> = emptySet(),
        var versionedAliases: Set<String> = emptySet(),
        var matchingLibraries: Map<String, Library> = emptyMap(),
        var matchingAliasIsUsedInBundle: Boolean = false,
        val buildFiles: MutableMap<Path, SourceFile> = HashMap(),
        val gatedBuildFiles: MutableSet<Path> = HashSet(),
    )

    override fun getInitialValue(ctx: ExecutionContext): Context = Context()

    override fun getScanner(acc: Context): TreeVisitor<*, ExecutionContext> =
        object : TreeVisitor<Tree, ExecutionContext>() {
            private val appliedPluginDetector = applyToModulesWithPluginId?.let(::AppliedPluginDetector)

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                val sourceFile = tree as? SourceFile ?: return tree
                if (sourceFile is Toml.Document && sourceFile.isTomlVersionCatalogFile()) {
                    val matchingLibraries = TomlVersionCatalog(sourceFile).libraries
                        .filter { it.keyValue.stringKey()?.matchesPluginLibraryName() == true }
                    acc.matchingAliases = matchingLibraries.mapNotNull { it.keyValue.stringKey() }.toSet()
                    acc.versionedAliases = matchingLibraries
                        .filter { it.library.version is VersionRef }
                        .mapNotNull { it.keyValue.stringKey() }
                        .toSet()
                    acc.matchingLibraries = matchingLibraries
                        .mapNotNull { entry -> entry.keyValue.stringKey()?.let { it to entry.library } }
                        .toMap()
                    acc.matchingAliasIsUsedInBundle = sourceFile.containsBundleAlias(acc.matchingAliases, p)
                }
                if (applyToModulesWithPluginId != null && sourceFile.isBuildGradleFile()) {
                    acc.buildFiles[sourceFile.sourcePath] = sourceFile
                    if (appliedPluginDetector?.isAppliedIn(sourceFile, p) == true) {
                        acc.gatedBuildFiles.add(sourceFile.sourcePath)
                    }
                }
                return tree
            }
        }

    override fun getVisitor(context: Context): TreeVisitor<*, ExecutionContext> {
        if (context.versionedAliases.isEmpty()) {
            return TreeVisitor.noop<Tree, ExecutionContext>()
        }

        val catalogVisitor = VersionRefRemovalVisitor(context.versionedAliases)
        if (applyToModulesWithPluginId == null) {
            return catalogTreeVisitor(catalogVisitor)
        }
        if (context.matchingAliasIsUsedInBundle) {
            return TreeVisitor.noop<Tree, ExecutionContext>()
        }

        return object : TreeVisitor<Tree, ExecutionContext>() {
            private var resolved = false
            private var shouldEditCatalog = false

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                if (!resolved) {
                    val referenceMatcher = LibraryRefsGradleDependencyReferenceMatcher(
                        aliases = context.matchingAliases,
                        libraries = context.matchingLibraries,
                    )
                    val referencingBuildFiles = context.buildFiles
                        .filterValues { referenceMatcher.isReferencedIn(it, p) }
                        .keys
                    shouldEditCatalog =
                        context.gatedBuildFiles.isNotEmpty() &&
                        referencingBuildFiles.all { it in context.gatedBuildFiles }
                    resolved = true
                }

                return if (shouldEditCatalog && tree is Toml.Document && tree.isTomlVersionCatalogFile()) {
                    catalogVisitor.visitNonNull(tree, p)
                } else {
                    tree
                }
            }
        }
    }

    private fun String.matchesPluginLibraryName(): Boolean = this == pluginName || StringUtils.matchesGlob(this, "$pluginName-*")
}

private fun catalogTreeVisitor(visitor: TreeVisitor<*, ExecutionContext>): TreeVisitor<Tree, ExecutionContext> =
    object : TreeVisitor<Tree, ExecutionContext>() {
        override fun visit(tree: Tree?, p: ExecutionContext): Tree? =
            if (tree is Toml.Document && tree.isTomlVersionCatalogFile()) {
                visitor.visitNonNull(tree, p)
            } else {
                tree
            }
    }

private class VersionRefRemovalVisitor(
    private val targetAliases: Set<String>,
) : TomlIsoVisitor<ExecutionContext>() {
    override fun visitKeyValue(keyValue: Toml.KeyValue, p: ExecutionContext): Toml.KeyValue {
        if (!isMatchingVersionedLibrary(keyValue)) {
            return super.visitKeyValue(keyValue, p)
        }

        return keyValue.withoutVersionReference()
    }

    private fun isMatchingVersionedLibrary(keyValue: Toml.KeyValue): Boolean {
        val isLibraryEntry = cursor.firstEnclosing(Toml.Table::class.java)?.name() == VERSION_CATALOG_TABLE_LIBS
        val libraryAlias = keyValue.stringKey()
        val library = keyValue.valueToLibrary()
        return isLibraryEntry &&
            libraryAlias != null &&
            libraryAlias in targetAliases &&
            library?.version is VersionRef
    }

    private fun Toml.KeyValue.withoutVersionReference(): Toml.KeyValue {
        val libraryDefinition = value as? Toml.Table ?: return this
        val entriesWithoutVersionReference = libraryDefinition.padding.values
            .filterNot { entry -> (entry.element as? Toml.KeyValue)?.stringKey() == VERSION_CATALOG_PARAM_VERSION_REF }
        val formattedEntries = entriesWithoutVersionReference.mapIndexed { index, entry ->
            if (index == entriesWithoutVersionReference.lastIndex) entry.withAfter(Space.SINGLE_SPACE) else entry
        }
        return withValue(libraryDefinition.padding.withValues(formattedEntries))
    }
}

private class LibraryRefsGradleDependencyReferenceMatcher(
    private val aliases: Set<String>,
    libraries: Map<String, Library>,
) {
    private val coordinates = libraries.values.map { it.group to it.name }.toSet()

    fun isReferencedIn(sourceFile: SourceFile, ctx: ExecutionContext): Boolean {
        val detection = LibraryRefsDependencyReferenceDetection()
        DelegatingJVisitor(
            javaVisitor = LibraryRefsGradleDependencyReferenceVisitor(
                aliases = aliases,
                targetCoordinates = coordinates,
                detection = detection,
            ),
        ).visit(sourceFile, ctx)
        return detection.found
    }
}

private class LibraryRefsDependencyReferenceDetection(
    var found: Boolean = false,
)

private class LibraryRefsGradleDependencyReferenceVisitor(
    private val aliases: Set<String>,
    private val targetCoordinates: Set<Pair<String, String>>,
    private val detection: LibraryRefsDependencyReferenceDetection,
) : JavaIsoVisitor<ExecutionContext>() {
    override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
        if (method.isDirectDependenciesAdd() &&
            method.arguments.drop(1).any { it.matchesGradleDependencyReference(aliases, targetCoordinates) }
        ) {
            detection.found = true
        }
        return super.visitMethodInvocation(method, p)
    }

    override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
        val isDependenciesBlock = cursor.isDependenciesBlock(block)
        val visited = super.visitBlock(block, p)
        if (!isDependenciesBlock) return visited

        visited.statements.forEach { statement ->
            if (statement.matchesTarget()) {
                detection.found = true
            }
        }
        return visited
    }

    private fun Statement.matchesTarget(): Boolean {
        val invocation = when (this) {
            is J.MethodInvocation -> this
            is J.Return -> expression as? J.MethodInvocation
            else -> null
        } ?: return false
        val arguments = if (invocation.simpleName == "add") invocation.arguments.drop(1) else invocation.arguments.take(1)
        return arguments.any { it.matchesGradleDependencyReference(aliases, targetCoordinates) }
    }

    private fun J.MethodInvocation.isDirectDependenciesAdd(): Boolean = simpleName == "add" && select?.toString() == "dependencies"
}
