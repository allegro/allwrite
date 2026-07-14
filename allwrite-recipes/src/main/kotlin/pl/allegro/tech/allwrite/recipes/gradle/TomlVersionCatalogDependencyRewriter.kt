package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.toml.TomlIsoVisitor
import org.openrewrite.toml.tree.Space
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.recipes.toml.Builders.kv
import pl.allegro.tech.allwrite.recipes.toml.Builders.literal
import pl.allegro.tech.allwrite.recipes.toml.name
import pl.allegro.tech.allwrite.recipes.toml.stringKey
import pl.allegro.tech.allwrite.recipes.toml.stringValue

internal class TomlVersionCatalogDependencyRewriter(
    oldGroupId: String,
    oldArtifactId: String,
    newGroupId: String?,
    newArtifactId: String?,
    newVersion: String?,
) : TomlIsoVisitor<ExecutionContext>() {
    private val target = TomlDependencyRewriteTarget(oldGroupId, oldArtifactId, newGroupId, newArtifactId, newVersion)
    private val planner = TomlVersionCatalogRewritePlanner(target)
    private lateinit var plan: TomlVersionCatalogRewritePlan

    override fun visitDocument(document: Toml.Document, p: ExecutionContext): Toml.Document {
        plan = planner.plan(document)
        return removeUnusedVersionEntries(super.visitDocument(document, p))
    }

    override fun visitKeyValue(keyValue: Toml.KeyValue, p: ExecutionContext): Toml.KeyValue {
        val table = cursor.firstEnclosing(Toml.Table::class.java) ?: return keyValue
        return when (table.name()) {
            VERSION_CATALOG_TABLE_LIBS -> rewriteLibraryEntry(table, keyValue)
            VERSION_CATALOG_TABLE_VERSIONS -> rewriteVersionEntry(keyValue)
            VERSION_CATALOG_TABLE_PLUGINS -> rewritePluginEntry(keyValue)
            else -> keyValue
        }
    }

    override fun visitTable(table: Toml.Table, p: ExecutionContext): Toml.Table {
        val visited = super.visitTable(table, p)
        if (visited.name() != VERSION_CATALOG_TABLE_VERSIONS) return visited
        if (plan.versionEntriesToAdd.isEmpty()) return visited

        val existingKeys = visited.values
            .asSequence()
            .filterIsInstance<Toml.KeyValue>()
            .mapNotNull { it.stringKey() }
            .toSet()
        val updatedValues = visited.values.map { value ->
            val keyValue = value as? Toml.KeyValue ?: return@map value
            val key = keyValue.stringKey() ?: return@map value
            plan.versionEntriesToAdd[key]?.let { version ->
                keyValue.withValue(literal(version).withPrefix(Space.SINGLE_SPACE))
            } ?: value
        }
        val missingEntries = plan.versionEntriesToAdd
            .filterKeys { it !in existingKeys }
            .map { (name, value) -> kv(name, value).withPrefix(Space.format("\n")) }
        return visited.withValues(updatedValues + missingEntries)
    }

    private fun removeUnusedVersionEntries(document: Toml.Document): Toml.Document {
        val usedVersionRefs = TomlVersionCatalog(document).usedVersionRefs()
        val values = document.values.map { value ->
            val table = value as? Toml.Table
            if (table?.name() != VERSION_CATALOG_TABLE_VERSIONS) return@map value
            val entries = table.values.filter { versionEntry ->
                val key = (versionEntry as? Toml.KeyValue)?.stringKey()
                key in usedVersionRefs
            }
            if (entries.size == table.values.size) value else table.withValues(entries)
        }
        return if (values.indices.all { values[it] === document.values[it] }) document else document.withValues(values)
    }

    private fun rewriteLibraryEntry(table: Toml.Table, keyValue: Toml.KeyValue): Toml.KeyValue {
        val library = keyValue.valueToLibrary() ?: return keyValue
        if (!target.matches(library) || libraryExistsInTable(table, library, keyValue)) return keyValue

        val entryName = keyValue.stringKey() ?: return keyValue
        val targetEntryName = target.targetEntryName(entryName)
        val currentVersionRef = (library.version as? VersionRef)?.ref
        val targetLibrary = target.targetLibrary(
            library,
            entryName,
            currentVersionRef?.takeIf { it in plan.versionRefUpdates },
        )
        if (entryName == targetEntryName && library == targetLibrary) return keyValue
        return targetLibrary.toTomlEntry(targetEntryName).withPrefix(keyValue.prefix)
    }

    private fun libraryExistsInTable(table: Toml.Table, library: Library, currentEntry: Toml.KeyValue): Boolean =
        table.values
            .asSequence()
            .filterIsInstance<Toml.KeyValue>()
            .filter { it != currentEntry }
            .mapNotNull { it.valueToLibrary() }
            .any { existingLibrary ->
                existingLibrary.group == (target.newGroupId ?: library.group) &&
                    existingLibrary.name == (target.newArtifactId ?: library.name)
            }

    private fun rewriteVersionEntry(keyValue: Toml.KeyValue): Toml.KeyValue {
        val version = target.newVersion ?: return keyValue
        val entryName = keyValue.stringKey() ?: return keyValue
        plan.versionRefRenames[entryName]?.let { targetEntryName ->
            if (keyValue.stringValue() == version) return keyValue
            return kv(targetEntryName, version).withPrefix(keyValue.prefix)
        }
        plan.versionRefUpdates[entryName]?.let {
            if (keyValue.stringValue() == it) return keyValue
            return kv(entryName, it).withPrefix(keyValue.prefix)
        }
        return keyValue
    }

    private fun rewritePluginEntry(keyValue: Toml.KeyValue): Toml.KeyValue {
        if (target.newVersion == null) return keyValue
        val plugin = keyValue.valueToPlugin() ?: return keyValue
        val versionRef = plugin.version as? VersionRef ?: return keyValue
        if (versionRef.ref != target.oldArtifactId) return keyValue
        val targetRef = plan.versionRefOverrides[versionRef.ref] ?: plan.versionRefRenames[versionRef.ref] ?: return keyValue
        val entryName = keyValue.stringKey() ?: return keyValue
        return Plugin(id = plugin.id, version = VersionRef(targetRef))
            .toTomlEntry(entryName)
            .withPrefix(keyValue.prefix)
    }
}
