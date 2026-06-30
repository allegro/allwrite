package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility
import kotlin.io.path.Path

public class ChangeGradleDependency(
    @Option(description = "The old group ID to replace.", example = "org.openrewrite.recipe")
    private val oldGroupId: String = "",
    @Option(description = "The old artifact ID to replace.", example = "rewrite-testing-frameworks")
    private val oldArtifactId: String = "",
    @Option(description = "The new group ID to use. Defaults to the existing group ID.", required = false, example = "corp.internal.openrewrite.recipe")
    private val newGroupId: String = "",
    @Option(description = "The new artifact ID to use. Defaults to the existing artifact ID.", required = false, example = "rewrite-testing-frameworks")
    private val newArtifactId: String = "",
    @Option(description = "The new version to set. When omitted, the version is removed from the dependency.", required = false, example = "3.1.4")
    private val newVersion: String = "",
) : AllwriteRecipe(
    displayName = "Change Gradle dependency with TOML support",
    description = "Changes Gradle dependencies and also updates matching entries in gradle/libs.versions.toml.",
    visibility = RecipeVisibility.INTERNAL,
) {
    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        if (oldGroupId.isBlank() || oldArtifactId.isBlank()) {
            return object : TreeVisitor<Tree, ExecutionContext>() {
                override fun visit(tree: Tree?, ctx: ExecutionContext): Tree? = tree
            }
        }

        val regexpDependencyChanger = RegexpDependencyChanger(
            oldGroupId = oldGroupId,
            oldArtifactId = oldArtifactId,
            newGroupId = newGroupId.takeIf { it.isNotBlank() } ?: oldGroupId,
            newArtifactId = newArtifactId.takeIf { it.isNotBlank() } ?: oldArtifactId,
            newVersion = newVersion.takeIf { it.isNotBlank() },
        )
        val gradleDependencyRewriter = GradleDependencyRewriter(
            oldGroupId = oldGroupId,
            oldArtifactId = oldArtifactId,
            newGroupId = newGroupId.takeIf { it.isNotBlank() } ?: oldGroupId,
            newArtifactId = newArtifactId.takeIf { it.isNotBlank() } ?: oldArtifactId,
            newVersion = newVersion.takeIf { it.isNotBlank() },
            regexpDependencyChanger = regexpDependencyChanger,
        )
        val tomlDependencyRewriter = TomlVersionCatalogDependencyRewriter(
            oldGroupId = oldGroupId,
            oldArtifactId = oldArtifactId,
            newGroupId = newGroupId.takeIf { it.isNotBlank() },
            newArtifactId = newArtifactId.takeIf { it.isNotBlank() },
            newVersion = newVersion.takeIf { it.isNotBlank() },
        )
        return object : TreeVisitor<Tree, ExecutionContext>() {
            private val TOML_VERSION_CATALOG_PATH = Path("gradle/libs.versions.toml")

            override fun isAcceptable(sourceFile: SourceFile, ctx: ExecutionContext): Boolean =
                sourceFile.isBuildGradleFile() || sourceFile is Toml.Document && sourceFile.sourcePath == TOML_VERSION_CATALOG_PATH

            override fun visit(tree: Tree?, ctx: ExecutionContext): Tree? {
                if (tree !is SourceFile) return tree
                if (tree.isBuildGradleFile()) {
                    return gradleDependencyRewriter.update(tree)
                }
                if (tree is Toml.Document && tree.sourcePath == TOML_VERSION_CATALOG_PATH) {
                    return tomlDependencyRewriter.visitNonNull(tree, ctx)
                }
                return tree
            }
        }
    }
}
