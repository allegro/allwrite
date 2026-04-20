package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.recipes.toml.Builders.kv
import pl.allegro.tech.allwrite.recipes.toml.asString
import pl.allegro.tech.allwrite.recipes.toml.findLiteralValue

public sealed interface VersionCatalogType
public object LibsToml : VersionCatalogType
public object Gradle : VersionCatalogType

public data class VersionCatalog(
    val versions: MutableMap<String, String> = HashMap(),
    val libraries: MutableMap<String, Library> = HashMap(),
    val plugins: MutableMap<String, Plugin> = HashMap(),
    val bundles: MutableMap<String, List<String>> = HashMap(),
)
public data class Library(
    val group: String,
    val name: String,
    val version: Version?,
)
public data class Plugin(
    val id: String,
    val version: Version?,
)

public sealed interface Version
public data class VersionRef(
    val ref: String,
) : Version
public data class PlainVersion(
    val version: String,
) : Version

internal const val VERSION_CATALOG_TABLE_VERSIONS: String = "versions"
internal const val VERSION_CATALOG_TABLE_LIBS: String = "libraries"
internal const val VERSION_CATALOG_TABLE_PLUGINS: String = "plugins"
internal const val VERSION_CATALOG_TABLE_BUNDLES: String = "bundles"

internal const val VERSION_CATALOG_PARAM_GROUP = "group"
internal const val VERSION_CATALOG_PARAM_NAME = "name"
internal const val VERSION_CATALOG_PARAM_VERSION = "version"
internal const val VERSION_CATALOG_PARAM_VERSION_REF = "version.ref"
internal const val VERSION_CATALOG_PARAM_ID = "id"
internal const val VERSION_CATALOG_PARAM_MODULE = "module"

internal fun Library.toTomlEntry(entryName: String) =
    buildMap {
        put(VERSION_CATALOG_PARAM_GROUP, group)
        put(VERSION_CATALOG_PARAM_NAME, name)
        when (version) {
            is PlainVersion -> put(VERSION_CATALOG_PARAM_VERSION, version.version)
            is VersionRef -> put(VERSION_CATALOG_PARAM_VERSION_REF, version.ref)
            null -> {}
        }
    }.let { kv(entryName, it) }

internal fun Toml.KeyValue.valueToLibrary(): Library? {
    val value = value as? Toml.Table

    // Handle module format: "org.junit.platform:junit-platform-launcher"
    val module = value?.findLiteralValue(VERSION_CATALOG_PARAM_MODULE)?.asString()
    if (module != null) {
        val parts = module.split(":")
        if (parts.size == 2) {
            val group = parts[0]
            val name = parts[1]
            val version = value.findLiteralValue(VERSION_CATALOG_PARAM_VERSION)?.asString()
            val versionRef = value.findLiteralValue(VERSION_CATALOG_PARAM_VERSION_REF)?.asString()
            val effectiveVersion = version?.let { PlainVersion(it) } ?: versionRef?.let { VersionRef(it) }
            return Library(group, name, effectiveVersion)
        }
    }

    // Handle group/name format
    val group = value?.findLiteralValue(VERSION_CATALOG_PARAM_GROUP)?.asString() ?: return null
    val name = value.findLiteralValue(VERSION_CATALOG_PARAM_NAME)?.asString() ?: return null
    val version = value.findLiteralValue(VERSION_CATALOG_PARAM_VERSION)?.asString()
    val versionRef = value.findLiteralValue(VERSION_CATALOG_PARAM_VERSION_REF)?.asString()

    val effectiveVersion = version?.let { PlainVersion(it) } ?: versionRef?.let { VersionRef(it) }
    return Library(group, name, effectiveVersion)
}

internal fun Toml.KeyValue.valueToPlugin(): Plugin? {
    val value = value as? Toml.Table
    val id = value?.findLiteralValue(VERSION_CATALOG_PARAM_ID)?.asString() ?: return null
    val version = value.findLiteralValue(VERSION_CATALOG_PARAM_VERSION)?.asString()
    val versionRef = value.findLiteralValue(VERSION_CATALOG_PARAM_VERSION_REF)?.asString()

    val effectiveVersion = version?.let { PlainVersion(it) } ?: versionRef?.let { VersionRef(it) }
    return Plugin(id, effectiveVersion)
}

internal fun String.toVersionCatalogName() = this.replace(".", "-")
internal fun String.toVersionCatalogReference() = this.replace("-", ".")
