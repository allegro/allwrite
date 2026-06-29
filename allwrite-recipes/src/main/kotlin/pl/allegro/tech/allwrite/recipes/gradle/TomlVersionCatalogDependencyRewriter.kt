package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.internal.StringUtils
import org.openrewrite.toml.TomlIsoVisitor
import org.openrewrite.toml.tree.Space
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.recipes.toml.Builders.kv
import pl.allegro.tech.allwrite.recipes.toml.name
import pl.allegro.tech.allwrite.recipes.toml.stringKey

internal class TomlVersionCatalogDependencyRewriter(
    private val oldGroupId: String,
    private val oldArtifactId: String,
    private val newGroupId: String?,
    private val newArtifactId: String?,
    private val newVersion: String?,
) : TomlIsoVisitor<ExecutionContext>() {
    private val sectionHandlers: List<VersionCatalogSectionHandler> = listOf(
        VersionsSectionHandler(),
        PluginsSectionHandler(),
        LibrariesSectionHandler(),
    )
    private val versionRefRenames = mutableMapOf<String, String>()
    private val versionRefUpdates = mutableMapOf<String, String>()
    private val versionEntriesToAdd = mutableMapOf<String, String>()

    override fun visitDocument(document: Toml.Document, p: ExecutionContext): Toml.Document {
        versionRefRenames.clear()
        versionRefUpdates.clear()
        versionEntriesToAdd.clear()
        planVersionRefChanges(document)
        return super.visitDocument(document, p)
    }

    override fun visitKeyValue(keyValue: Toml.KeyValue, p: ExecutionContext): Toml.KeyValue {
        val table = cursor.firstEnclosing(Toml.Table::class.java) ?: return keyValue
        val handler = sectionHandlers.firstOrNull { it.supports(table.name()) } ?: return keyValue
        return handler.handle(table, keyValue)
    }

    override fun visitTable(table: Toml.Table, p: ExecutionContext): Toml.Table {
        val visited = super.visitTable(table, p)
        if (visited.name() != VERSION_CATALOG_TABLE_VERSIONS) return visited
        if (versionEntriesToAdd.isEmpty()) return visited

        val existingKeys = visited.values
            .asSequence()
            .filterIsInstance<Toml.KeyValue>()
            .mapNotNull { it.stringKey() }
            .toSet()
        val missingEntries = versionEntriesToAdd
            .filterKeys { it !in existingKeys }
            .map { (name, value) -> kv(name, value).withPrefix(Space.format("\n")) }

        return if (missingEntries.isEmpty()) visited else visited.withValues(visited.values + missingEntries)
    }

    private fun planVersionRefChanges(document: Toml.Document) {
        planLibraryVersionRefChanges(document)
        planPluginVersionRefChanges(document)
    }

    private fun planLibraryVersionRefChanges(document: Toml.Document) {
        val libraryTable = document.tables().firstOrNull { it.name() == VERSION_CATALOG_TABLE_LIBS } ?: return
        libraryTable.values
            .asSequence()
            .filterIsInstance<Toml.KeyValue>()
            .forEach { keyValue ->
                val library = keyValue.valueToLibrary() ?: return@forEach
                if (!isTargetLibrary(library)) return@forEach
                if (libraryExistsInTable(libraryTable, library, keyValue)) return@forEach
                if (newVersion == null) return@forEach

                val entryName = keyValue.stringKey() ?: return@forEach
                val version = library.version as? VersionRef ?: return@forEach
                planVersionRefChange(document, keyValue, version.ref, newArtifactId ?: entryName)
            }
    }

    private fun planPluginVersionRefChanges(document: Toml.Document) {
        val pluginTable = document.tables().firstOrNull { it.name() == VERSION_CATALOG_TABLE_PLUGINS } ?: return
        pluginTable.values
            .asSequence()
            .filterIsInstance<Toml.KeyValue>()
            .forEach { keyValue ->
                val plugin = keyValue.valueToPlugin() ?: return@forEach
                if (newVersion == null) return@forEach
                val version = plugin.version as? VersionRef ?: return@forEach
                if (version.ref != oldArtifactId) return@forEach

                val entryName = keyValue.stringKey() ?: return@forEach
                planVersionRefChange(document, keyValue, version.ref, newArtifactId ?: entryName)
            }
    }

    private fun planVersionRefChange(document: Toml.Document, keyValue: Toml.KeyValue, currentRef: String, targetRef: String) {
        val version = newVersion ?: return
        when {
            isVersionRefShared(document, currentRef, keyValue) -> versionEntriesToAdd[targetRef] = version
            currentRef == targetRef -> versionRefUpdates[targetRef] = version
            else -> versionRefRenames[currentRef] = targetRef
        }
    }

    private fun isTargetLibrary(library: Library): Boolean =
        StringUtils.matchesGlob(library.group, oldGroupId) && StringUtils.matchesGlob(library.name, oldArtifactId)

    private fun libraryExistsInTable(table: Toml.Table, library: Library, currentEntry: Toml.KeyValue): Boolean =
        table.values
            .asSequence()
            .filterIsInstance<Toml.KeyValue>()
            .filter { it != currentEntry }
            .mapNotNull { it.valueToLibrary() }
            .any { it.group == (newGroupId ?: library.group) && it.name == (newArtifactId ?: library.name) }

    private fun isVersionRefShared(document: Toml.Document, versionRef: String, currentEntry: Toml.KeyValue): Boolean =
        document.values
            .asSequence()
            .filterIsInstance<Toml.Table>()
            .flatMap { it.values.asSequence().filterIsInstance<Toml.KeyValue>() }
            .filter { it != currentEntry }
            .mapNotNull { keyValue -> keyValue.valueToLibrary() ?: keyValue.valueToPlugin()?.let { Library(it.id, it.id, it.version) } }
            .any { library -> (library.version as? VersionRef)?.ref == versionRef }

    private interface VersionCatalogSectionHandler {
        fun supports(tableName: String?): Boolean
        fun handle(table: Toml.Table, keyValue: Toml.KeyValue): Toml.KeyValue
    }

    private inner class LibrariesSectionHandler : VersionCatalogSectionHandler {
        override fun supports(tableName: String?): Boolean = tableName == VERSION_CATALOG_TABLE_LIBS

        override fun handle(table: Toml.Table, keyValue: Toml.KeyValue): Toml.KeyValue {
            val library = keyValue.valueToLibrary() ?: return keyValue
            if (!isTargetLibrary(library)) return keyValue
            if (libraryExistsInTable(table, library, keyValue)) return keyValue

            val entryName = keyValue.stringKey() ?: return keyValue
            val targetLibrary = Library(
                group = newGroupId ?: library.group,
                name = newArtifactId ?: library.name,
                version = when {
                    library.version == null -> null
                    newVersion == null -> null
                    library.version is VersionRef -> VersionRef(newArtifactId ?: entryName)
                    else -> PlainVersion(newVersion)
                },
            )
            return targetLibrary.toTomlEntry(targetEntryName(entryName)).withPrefix(keyValue.prefix)
        }
    }

    private inner class VersionsSectionHandler : VersionCatalogSectionHandler {
        override fun supports(tableName: String?): Boolean = tableName == VERSION_CATALOG_TABLE_VERSIONS

        override fun handle(table: Toml.Table, keyValue: Toml.KeyValue): Toml.KeyValue {
            if (newVersion == null) return keyValue
            val entryName = keyValue.stringKey() ?: return keyValue
            versionRefRenames[entryName]?.let { targetEntryName ->
                return kv(targetEntryName, newVersion).withPrefix(keyValue.prefix)
            }
            versionRefUpdates[entryName]?.let {
                return kv(entryName, it).withPrefix(keyValue.prefix)
            }
            return keyValue
        }
    }

    private inner class PluginsSectionHandler : VersionCatalogSectionHandler {
        override fun supports(tableName: String?): Boolean = tableName == VERSION_CATALOG_TABLE_PLUGINS

        override fun handle(table: Toml.Table, keyValue: Toml.KeyValue): Toml.KeyValue {
            if (newVersion == null) return keyValue
            val plugin = keyValue.valueToPlugin() ?: return keyValue
            val renamedVersionRef =
                (plugin.version as? VersionRef)
                    ?.let { versionRefRenames[it.ref] }
                    ?: return keyValue
            val entryName = keyValue.stringKey() ?: return keyValue
            return Plugin(id = plugin.id, version = VersionRef(renamedVersionRef))
                .toTomlEntry(entryName)
                .withPrefix(keyValue.prefix)
        }
    }

    private fun Toml.Document.tables(): List<Toml.Table> = values.filterIsInstance<Toml.Table>()

    private fun targetEntryName(entryName: String): String = if (entryName == oldArtifactId && newArtifactId != null) newArtifactId else entryName
}
