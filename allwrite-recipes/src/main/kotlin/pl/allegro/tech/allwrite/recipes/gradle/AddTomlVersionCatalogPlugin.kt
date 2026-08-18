package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.FindSourceFiles
import org.openrewrite.Preconditions
import org.openrewrite.TreeVisitor
import org.openrewrite.internal.StringUtils
import org.openrewrite.marker.SearchResult
import org.openrewrite.toml.TomlIsoVisitor
import org.openrewrite.toml.tree.Space
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.INTERNAL
import pl.allegro.tech.allwrite.recipes.toml.Builders
import pl.allegro.tech.allwrite.recipes.toml.keyValues
import pl.allegro.tech.allwrite.recipes.toml.name
import pl.allegro.tech.allwrite.recipes.toml.stringKey
import pl.allegro.tech.allwrite.recipes.toml.table

internal class AddTomlVersionCatalogPlugin(
    private val pluginName: String,
    private val pluginId: String,
    private val pluginVersion: String,
) : AllwriteRecipe(
    displayName = "Add a plugin to the version catalog",
    description = "Adds or updates a plugin in gradle/libs.versions.toml.",
    visibility = INTERNAL,
) {

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        Preconditions.check(
            Preconditions.and(
                FindSourceFiles("gradle/libs.versions.toml").visitor,
                PluginLibraryPrecondition(),
            ),
            Visitor(),
        )

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

        override fun visitKeyValue(keyValue: Toml.KeyValue, p: ExecutionContext): Toml.KeyValue {
            return when (cursor.firstEnclosing(Toml.Table::class.java)?.name()) {
                VERSION_CATALOG_TABLE_LIBS -> visitLibrary(keyValue, p)
                VERSION_CATALOG_TABLE_PLUGINS -> visitPlugin(keyValue, p)
                else -> super.visitKeyValue(keyValue, p)
            }
        }

        private fun visitLibrary(keyValue: Toml.KeyValue, p: ExecutionContext): Toml.KeyValue {
            val entryName = keyValue.stringKey()
            if (entryName == null || !entryName.matchesPluginLibraryName()) return super.visitKeyValue(keyValue, p)

            val library = keyValue.valueToLibrary()
            if (library?.version !is VersionRef) return super.visitKeyValue(keyValue, p)

            val value = keyValue.value as? Toml.Table ?: return keyValue
            val values = value.padding.values.filter { nestedValue ->
                (nestedValue.element as? Toml.KeyValue)?.stringKey() != VERSION_CATALOG_PARAM_VERSION_REF
            }
            val updatedValues = values.mapIndexed { index, nestedValue ->
                if (index == values.lastIndex) nestedValue.withAfter(Space.SINGLE_SPACE) else nestedValue
            }
            return keyValue.withValue(value.padding.withValues(updatedValues))
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

    private inner class PluginLibraryPrecondition : TomlIsoVisitor<ExecutionContext>() {
        override fun visitDocument(document: Toml.Document, p: ExecutionContext): Toml.Document =
            if (document.hasPluginLibraries()) SearchResult.found(document)!! else document
    }

    private fun Toml.Document.hasPluginLibraries(): Boolean =
        table(VERSION_CATALOG_TABLE_LIBS)
            ?.keyValues()
            ?.any { keyValue ->
                keyValue.stringKey()?.matchesPluginLibraryName() == true && keyValue.valueToLibrary() != null
            } == true

    private fun String.matchesPluginLibraryName(): Boolean =
        this == pluginName || StringUtils.matchesGlob(this, "$pluginName-*")

    private fun requestedPlugin(): Plugin = Plugin(pluginId, PlainVersion(pluginVersion))
}
