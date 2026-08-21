package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.FindSourceFiles
import org.openrewrite.Preconditions
import org.openrewrite.TreeVisitor
import org.openrewrite.internal.StringUtils
import org.openrewrite.toml.TomlIsoVisitor
import org.openrewrite.toml.tree.Space
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.recipes.toml.name
import pl.allegro.tech.allwrite.recipes.toml.stringKey

internal class RemoveLibraryVersionRefs(
    private val pluginName: String,
) : AllwriteRecipe(
    displayName = "Remove library version references",
    description = "Removes version.ref from library aliases equal to pluginName or matching pluginName-* in gradle/libs.versions.toml.",
) {
    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        Preconditions.check(
            FindSourceFiles("gradle/libs.versions.toml").visitor,
            Visitor(pluginName),
        )

    private class Visitor(
        private val pluginName: String,
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
                library?.version is VersionRef &&
                libraryAlias.matchesPluginLibraryName()
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

        private fun String.matchesPluginLibraryName(): Boolean = this == pluginName || StringUtils.matchesGlob(this, "$pluginName-*")
    }
}
