package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.internal.StringUtils
import org.openrewrite.text.PlainTextParser
import org.openrewrite.toml.TomlIsoVisitor
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility
import pl.allegro.tech.allwrite.recipes.toml.Builders.kv
import pl.allegro.tech.allwrite.recipes.toml.name
import pl.allegro.tech.allwrite.recipes.toml.stringKey
import pl.allegro.tech.allwrite.recipes.toml.stringValue
import kotlin.io.path.Path

public class ChangeGradleDependency(
    @Option(description = "The old group ID to replace.", example = "org.openrewrite.recipe")
    public val oldGroupId: String,
    @Option(description = "The old artifact ID to replace.", example = "rewrite-testing-frameworks")
    public val oldArtifactId: String,
    @Option(description = "The new group ID to use. Defaults to the existing group ID.", required = false, example = "corp.internal.openrewrite.recipe")
    public val newGroupId: String? = null,
    @Option(description = "The new artifact ID to use. Defaults to the existing artifact ID.", required = false, example = "rewrite-testing-frameworks")
    public val newArtifactId: String? = null,
    @Option(description = "An exact version number or node-style semver selector used to select the version number.", required = false, example = "29.X")
    public val newVersion: String? = null
) : AllwriteRecipe(
    displayName = "Change Gradle dependency with TOML support",
    description = "Changes Gradle dependencies and also updates matching entries in gradle/libs.versions.toml.",
    visibility = RecipeVisibility.INTERNAL,
) {
    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        val regexpDependencyChanger = RegexpDependencyChanger(
            oldGroupId = oldGroupId,
            oldArtifactId = oldArtifactId,
            newGroupId = newGroupId ?: oldGroupId,
            newArtifactId = newArtifactId ?: oldArtifactId,
            newVersion = newVersion,
        )
        return object : TreeVisitor<Tree, ExecutionContext>() {
            override fun isAcceptable(sourceFile: SourceFile, ctx: ExecutionContext): Boolean =
                sourceFile.isBuildGradleFile() ||
                    sourceFile is Toml.Document && sourceFile.sourcePath == TOML_VERSION_CATALOG_PATH

            override fun visit(tree: Tree?, ctx: ExecutionContext): Tree? {
                if (tree !is SourceFile) return tree
                var sourceFile = tree

                if (sourceFile.isBuildGradleFile()) {
                    val plainText = PlainTextParser.convert(sourceFile)
                    val updatedText = regexpDependencyChanger.update(plainText.text)
                    sourceFile = if (updatedText != plainText.text) plainText.withText(updatedText) else sourceFile
                }

                if (sourceFile is Toml.Document && sourceFile.sourcePath == TOML_VERSION_CATALOG_PATH) {
                    val tomlVisitor = ChangeTomlVersionCatalogDependency(
                        oldGroupId = oldGroupId,
                        oldArtifactId = oldArtifactId,
                        newGroupId = newGroupId,
                        newArtifactId = newArtifactId,
                        newVersion = newVersion,
                    )

                    sourceFile = tomlVisitor.visitNonNull(sourceFile, ctx)
                }

                return sourceFile
            }
        }
    }
}

private val TOML_VERSION_CATALOG_PATH = Path("gradle/libs.versions.toml")

private class ChangeTomlVersionCatalogDependency(
    val oldGroupId: String,
    val oldArtifactId: String,
    val newGroupId: String?,
    val newArtifactId: String?,
    val newVersion: String?,
) : TomlIsoVisitor<ExecutionContext>() {

    override fun visitKeyValue(keyValue: Toml.KeyValue, p: ExecutionContext): Toml.KeyValue {
        val table = cursor.firstEnclosing(Toml.Table::class.java) ?: return keyValue
        val tableName = table.name()
        if (tableName == VERSION_CATALOG_TABLE_VERSIONS) {
            return updateVersionsEntry(keyValue)
        }
        if (tableName == VERSION_CATALOG_TABLE_PLUGINS) {
            return updatePluginEntry(keyValue)
        }
        if (tableName != VERSION_CATALOG_TABLE_LIBS) return keyValue

        val library = keyValue.valueToLibrary() ?: return keyValue
        val entryName = keyValue.stringKey() ?: return keyValue
        val isMatchedDependency = StringUtils.matchesGlob(library.group, oldGroupId) && StringUtils.matchesGlob(library.name, oldArtifactId)
        val renamedVersionRef = renameVersionRef(library.version)

        if (!isMatchedDependency) {
            return if (renamedVersionRef != library.version) {
                library.copy(version = renamedVersionRef).toTomlEntry(entryName).withPrefix(keyValue.prefix)
            } else {
                keyValue
            }
        }

        val targetLibrary = Library(
            group = newGroupId ?: library.group,
            name = newArtifactId ?: library.name,
            version = when {
                library.version is VersionRef -> renamedVersionRef
                newVersion != null -> PlainVersion(newVersion)
                else -> renamedVersionRef
            },
        )
        val targetEntryName = when {
            entryName == oldArtifactId && newArtifactId != null -> newArtifactId
            else -> entryName
        }
        return targetLibrary.toTomlEntry(targetEntryName).withPrefix(keyValue.prefix)
    }

    private fun updateVersionsEntry(keyValue: Toml.KeyValue): Toml.KeyValue {
        val entryName = keyValue.stringKey() ?: return keyValue
        val value = keyValue.stringValue() ?: return keyValue
        if (entryName != oldArtifactId) return keyValue
        val targetEntryName = newArtifactId ?: entryName
        val targetValue = newVersion ?: value
        return kv(targetEntryName, targetValue).withPrefix(keyValue.prefix)
    }

    private fun updatePluginEntry(keyValue: Toml.KeyValue): Toml.KeyValue {
        val plugin = keyValue.valueToPlugin() ?: return keyValue
        val entryName = keyValue.stringKey() ?: return keyValue
        val renamedVersionRef = renameVersionRef(plugin.version)
        if (renamedVersionRef == plugin.version) return keyValue
        return Plugin(id = plugin.id, version = renamedVersionRef).toTomlEntry(entryName).withPrefix(keyValue.prefix)
    }

    private fun renameVersionRef(version: Version?): Version? {
        if (version !is VersionRef) return version
        if (version.ref != oldArtifactId || newArtifactId == null) return version
        return VersionRef(newArtifactId)
    }
}
