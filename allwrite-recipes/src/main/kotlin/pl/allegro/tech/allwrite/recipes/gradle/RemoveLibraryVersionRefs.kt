package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.FindSourceFiles
import org.openrewrite.Preconditions
import org.openrewrite.internal.StringUtils
import org.openrewrite.toml.TomlIsoVisitor
import org.openrewrite.toml.tree.Space
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.INTERNAL
import pl.allegro.tech.allwrite.recipes.toml.name
import pl.allegro.tech.allwrite.recipes.toml.stringKey

internal class RemoveLibraryVersionRefs(
    private val pluginName: String,
) : AllwriteRecipe(
    displayName = "Remove library version references",
    description = "Removes version.ref from library aliases equal to pluginName or matching pluginName-* in gradle/libs.versions.toml.",
    visibility = INTERNAL,
) {
    override fun getVisitor() =
        Preconditions.check(
            FindSourceFiles("gradle/libs.versions.toml").visitor,
            Visitor(pluginName),
        )

    private class Visitor(
        private val pluginName: String,
    ) : TomlIsoVisitor<ExecutionContext>() {
        override fun visitKeyValue(keyValue: Toml.KeyValue, p: ExecutionContext): Toml.KeyValue {
            if (cursor.firstEnclosing(Toml.Table::class.java)?.name() != VERSION_CATALOG_TABLE_LIBS) {
                return super.visitKeyValue(keyValue, p)
            }

            val entryName = keyValue.stringKey()
            val library = keyValue.valueToLibrary()
            if (entryName == null || library?.version !is VersionRef || !entryName.matchesPluginLibraryName()) {
                return super.visitKeyValue(keyValue, p)
            }

            val value = keyValue.value as? Toml.Table ?: return keyValue
            val values = value.padding.values.filter { nestedValue ->
                (nestedValue.element as? Toml.KeyValue)?.stringKey() != VERSION_CATALOG_PARAM_VERSION_REF
            }
            val updatedValues = values.mapIndexed { index, nestedValue ->
                if (index == values.lastIndex) nestedValue.withAfter(Space.SINGLE_SPACE) else nestedValue
            }
            return keyValue.withValue(value.padding.withValues(updatedValues))
        }

        private fun String.matchesPluginLibraryName(): Boolean = this == pluginName || StringUtils.matchesGlob(this, "$pluginName-*")
    }
}
