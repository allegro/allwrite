package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.toml.TomlVisitor
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.recipes.toml.asString
import pl.allegro.tech.allwrite.recipes.toml.findLiteralValue
import pl.allegro.tech.allwrite.recipes.toml.name
import pl.allegro.tech.allwrite.recipes.toml.stringKey
import pl.allegro.tech.allwrite.recipes.toml.stringValue

// TODO: support bundles
internal class ParseTomlVersionCatalog(val versionCatalog: VersionCatalog = VersionCatalog()) : TomlVisitor<ExecutionContext>() {

    override fun visitKeyValue(keyValue: Toml.KeyValue, p: ExecutionContext): Toml {
        val key = keyValue.stringKey() ?: return keyValue
        val table = cursor.firstEnclosing(Toml.Table::class.java) ?: return keyValue

        when (table.name()) {
            VERSION_CATALOG_TABLE_VERSIONS -> keyValue.stringValue()?.let { version -> versionCatalog.versions[key] = version }
            VERSION_CATALOG_TABLE_LIBS -> keyValue.valueToLibrary()?.let { lib -> versionCatalog.libraries[key] = lib }
            VERSION_CATALOG_TABLE_PLUGINS -> keyValue.valueToPlugin()?.let { plugin -> versionCatalog.plugins[key] = plugin }
        }

        return super.visitKeyValue(keyValue, p)
    }
}
