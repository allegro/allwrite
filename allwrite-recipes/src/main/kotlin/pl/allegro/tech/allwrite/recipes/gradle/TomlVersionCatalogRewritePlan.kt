package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.internal.StringUtils
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.recipes.toml.name
import pl.allegro.tech.allwrite.recipes.toml.stringKey

internal data class TomlDependencyRewriteTarget(
    val oldGroupId: String,
    val oldArtifactId: String,
    val newGroupId: String?,
    val newArtifactId: String?,
    val newVersion: String?,
) {
    fun matches(library: Library): Boolean =
        StringUtils.matchesGlob(library.group, oldGroupId) && StringUtils.matchesGlob(library.name, oldArtifactId)

    fun targetLibrary(library: Library, entryName: String, versionRef: String? = null): Library =
        Library(
            group = newGroupId ?: library.group,
            name = newArtifactId ?: library.name,
            version = targetLibraryVersion(library, entryName, versionRef),
        )

    fun targetEntryName(entryName: String): String =
        if (entryName == oldArtifactId && newArtifactId != null) newArtifactId.toVersionCatalogName() else entryName

    fun targetVersionRef(entryName: String): String = (newArtifactId ?: entryName).toVersionCatalogName()

    private fun targetLibraryVersion(library: Library, entryName: String, versionRef: String?): Version? =
        when {
            library.version == null || newVersion == null -> null
            library.version is VersionRef -> VersionRef(versionRef ?: targetVersionRef(entryName))
            else -> PlainVersion(newVersion)
        }
}

internal data class TomlVersionCatalogRewritePlan(
    val versionRefRenames: Map<String, String>,
    val versionRefOverrides: Map<String, String>,
    val versionRefUpdates: Map<String, String>,
    val versionEntriesToAdd: Map<String, String>,
)

internal class TomlVersionCatalogRewritePlanner(
    private val target: TomlDependencyRewriteTarget,
) {
    fun plan(document: Toml.Document): TomlVersionCatalogRewritePlan {
        val versionRefRenames = mutableMapOf<String, String>()
        val versionRefOverrides = mutableMapOf<String, String>()
        val versionRefUpdates = mutableMapOf<String, String>()
        val versionEntriesToAdd = mutableMapOf<String, String>()

        document.libraryEntries()
            .filter { (keyValue, library) -> target.matches(library) && !libraryExists(document, keyValue, library) }
            .forEach { (keyValue, library) ->
                val entryName = keyValue.stringKey() ?: return@forEach
                val versionRef = (library.version as? VersionRef)?.ref ?: return@forEach
                val version = target.newVersion
                if (version == null) return@forEach
                val targetRef = target.targetVersionRef(entryName)
                when {
                    isVersionRefShared(document, versionRef, keyValue) -> {
                        versionEntriesToAdd[targetRef] = version
                        versionRefOverrides[versionRef] = targetRef
                    }
                    isVersionRefSharedByMatchingLibraries(document, versionRef, keyValue) -> versionRefUpdates[versionRef] = version
                    versionRef == targetRef || versionEntryExists(document, targetRef) -> versionRefUpdates[targetRef] = version
                    else -> versionRefRenames[versionRef] = targetRef
                }
            }

        if (target.newVersion != null) {
            document.pluginEntries()
                .filter { (_, plugin) -> (plugin.version as? VersionRef)?.ref == target.oldArtifactId }
                .forEach { (keyValue, plugin) ->
                    val entryName = keyValue.stringKey() ?: return@forEach
                    val versionRef = (plugin.version as? VersionRef)?.ref ?: return@forEach
                    val targetRef = target.targetVersionRef(entryName)
                    when {
                        isVersionRefShared(document, versionRef, keyValue) -> {
                            versionEntriesToAdd[targetRef] = target.newVersion
                            versionRefOverrides[versionRef] = targetRef
                        }
                        versionRef == targetRef || versionEntryExists(document, targetRef) -> versionRefUpdates[targetRef] = target.newVersion
                        else -> versionRefRenames[versionRef] = targetRef
                    }
                }
        }

        return TomlVersionCatalogRewritePlan(
            versionRefRenames,
            versionRefOverrides,
            versionRefUpdates,
            versionEntriesToAdd,
        )
    }

    private fun libraryExists(document: Toml.Document, currentEntry: Toml.KeyValue, library: Library): Boolean =
        document.libraryEntries()
            .filter { (keyValue, _) -> keyValue != currentEntry }
            .any { (_, existingLibrary) ->
                existingLibrary.group == (target.newGroupId ?: library.group) &&
                    existingLibrary.name == (target.newArtifactId ?: library.name)
            }

    private fun versionEntryExists(document: Toml.Document, versionName: String): Boolean =
        document.table(VERSION_CATALOG_TABLE_VERSIONS)
            ?.values
            ?.filterIsInstance<Toml.KeyValue>()
            ?.any { it.stringKey() == versionName } == true

    private fun isVersionRefShared(document: Toml.Document, versionRef: String, currentEntry: Toml.KeyValue): Boolean =
        document.libraryEntries()
            .any { (keyValue, library) ->
                keyValue != currentEntry &&
                    (library.version as? VersionRef)?.ref == versionRef &&
                    !target.matches(library)
            } ||
            document.pluginEntries()
                .any { (keyValue, plugin) ->
                    keyValue != currentEntry && (plugin.version as? VersionRef)?.ref == versionRef
                }

    private fun isVersionRefSharedByMatchingLibraries(
        document: Toml.Document,
        versionRef: String,
        currentEntry: Toml.KeyValue,
    ): Boolean =
        document.libraryEntries()
            .any { (keyValue, library) ->
                keyValue != currentEntry &&
                    target.matches(library) &&
                    (library.version as? VersionRef)?.ref == versionRef
            }

    private fun Toml.Document.libraryEntries(): List<Pair<Toml.KeyValue, Library>> =
        table(VERSION_CATALOG_TABLE_LIBS)
            ?.values
            ?.filterIsInstance<Toml.KeyValue>()
            ?.mapNotNull { keyValue -> keyValue.valueToLibrary()?.let { keyValue to it } }
            ?: emptyList()

    private fun Toml.Document.pluginEntries(): List<Pair<Toml.KeyValue, Plugin>> =
        table(VERSION_CATALOG_TABLE_PLUGINS)
            ?.values
            ?.filterIsInstance<Toml.KeyValue>()
            ?.mapNotNull { keyValue -> keyValue.valueToPlugin()?.let { keyValue to it } }
            ?: emptyList()

    private fun Toml.Document.table(name: String): Toml.Table? =
        values.filterIsInstance<Toml.Table>().firstOrNull { it.name() == name }
}
