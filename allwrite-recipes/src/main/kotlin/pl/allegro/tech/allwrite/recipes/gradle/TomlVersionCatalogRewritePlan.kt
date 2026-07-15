package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.internal.StringUtils
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.recipes.toml.stringKey

internal data class TomlDependencyRewriteTarget(
    val oldGroupId: String,
    val oldArtifactId: String,
    val newGroupId: String?,
    val newArtifactId: String?,
    val newVersion: String?,
) {
    fun matches(library: Library): Boolean = StringUtils.matchesGlob(library.group, oldGroupId) && StringUtils.matchesGlob(library.name, oldArtifactId)

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
        val catalog = TomlVersionCatalog(document)
        val versionRefRenames = mutableMapOf<String, String>()
        val versionRefOverrides = mutableMapOf<String, String>()
        val versionRefUpdates = mutableMapOf<String, String>()
        val versionEntriesToAdd = mutableMapOf<String, String>()

        catalog.libraries
            .filter { entry ->
                target.matches(entry.library) &&
                    !catalog.hasLibrary(
                        target.newGroupId ?: entry.library.group,
                        target.newArtifactId ?: entry.library.name,
                        entry.keyValue,
                    )
            }
            .forEach { entry ->
                val entryName = entry.keyValue.stringKey() ?: return@forEach
                val library = entry.library
                val versionRef = (library.version as? VersionRef)?.ref ?: return@forEach
                val version = target.newVersion ?: return@forEach
                val targetRef = target.targetVersionRef(entryName)
                when {
                    catalog.hasOtherConsumer(versionRef, entry.keyValue, target::matches) -> {
                        versionEntriesToAdd[targetRef] = version
                        versionRefOverrides[versionRef] = targetRef
                    }
                    catalog.hasOtherLibrary(versionRef, entry.keyValue, target::matches) -> versionRefUpdates[versionRef] = version
                    versionRef == targetRef || catalog.hasVersion(targetRef) -> versionRefUpdates[targetRef] = version
                    else -> versionRefRenames[versionRef] = targetRef
                }
            }

        if (target.newVersion != null) {
            catalog.plugins
                .filter { entry -> (entry.plugin.version as? VersionRef)?.ref == target.oldArtifactId }
                .forEach { entry ->
                    val entryName = entry.keyValue.stringKey() ?: return@forEach
                    val plugin = entry.plugin
                    val versionRef = (plugin.version as? VersionRef)?.ref ?: return@forEach
                    val targetRef = target.targetVersionRef(entryName)
                    when {
                        catalog.hasOtherConsumer(versionRef, entry.keyValue, target::matches) -> {
                            versionEntriesToAdd[targetRef] = target.newVersion
                            versionRefOverrides[versionRef] = targetRef
                        }
                        versionRef == targetRef || catalog.hasVersion(targetRef) -> versionRefUpdates[targetRef] = target.newVersion
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
}
