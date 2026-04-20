package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.toml.TomlIsoVisitor
import org.openrewrite.toml.tree.Space
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.recipes.toml.Builders
import pl.allegro.tech.allwrite.recipes.toml.Builders.id
import pl.allegro.tech.allwrite.recipes.toml.name
import pl.allegro.tech.allwrite.recipes.toml.stringKey

internal class AddTomlVersionCatalogDependency(
    val lib: Library,
    val versionCatalogName: String = "${lib.group}.${lib.name}",
    val onVersionConflict: OnVersionConflict = OnVersionConflict.IGNORE,
) : TomlIsoVisitor<ExecutionContext>() {

    override fun visitDocument(document: Toml.Document, p: ExecutionContext): Toml.Document = super.visitDocument(withLibraries(document), p)

    private fun withLibraries(document: Toml.Document): Toml.Document {
        val librariesExist = document.values.any { it is Toml.Table && it.name() == VERSION_CATALOG_TABLE_LIBS }
        if (librariesExist) return document

        val newValues = document.values + Builders.emptyTable().withName(id(VERSION_CATALOG_TABLE_LIBS))
        return document.withValues(newValues)
    }

    override fun visitTable(table: Toml.Table, p: ExecutionContext): Toml.Table {
        if (table.name?.name != VERSION_CATALOG_TABLE_LIBS) return table
        val table = super.visitTable(table, p)

        if (cursor.shouldCreateNewEntry()) {
            val newLib = lib.toTomlEntry(versionCatalogName.toVersionCatalogName())
                .withPrefix(Space.format("\n"))
            return table.withValues(table.values + newLib)
        }

        return table
    }

    override fun visitKeyValue(keyValue: Toml.KeyValue, p: ExecutionContext): Toml.KeyValue {
        val library = keyValue.valueToLibrary() ?: return keyValue
        if (library.group == lib.group && library.name == lib.name) {
            cursor.markEntryFound()
            if (library.version != lib.version && onVersionConflict == OnVersionConflict.OVERRIDE) {
                val stringKey = keyValue.stringKey() ?: return keyValue
                return lib.toTomlEntry(stringKey).withPrefix(Space.format("\n"))
            }
        }
        return keyValue
    }

    private fun Cursor.markEntryFound() = putMessageOnFirstEnclosing(Toml.Table::class.java, CURSOR_MESSAGE_KEY, false)
    private fun Cursor.shouldCreateNewEntry() = getMessage(CURSOR_MESSAGE_KEY, true)

    private companion object {
        val CURSOR_MESSAGE_KEY: String = "${this::class.java.name}_message"
    }
}
