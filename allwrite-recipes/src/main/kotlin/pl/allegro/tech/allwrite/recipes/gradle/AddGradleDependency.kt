package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.toml.tree.Toml
import org.slf4j.LoggerFactory
import pl.allegro.tech.allwrite.recipes.AllwriteScanningRecipe
import pl.allegro.tech.allwrite.recipes.RecipeVisibility
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path

private val TOML_VERSION_CATALOG_PATH = Path("gradle/libs.versions.toml")

public class AddGradleDependency(
    @Option(description = "Gradle configuration to add dependency to", example = "implementation")
    public val configuration: String,
    @Option(description = "Dependency's group id", example = "org.springframework.boot")
    public val groupId: String,
    @Option(description = "Dependency's artifact id", example = "spring-boot-starter-web")
    public val artifactId: String,
    @Option(description = "Dependency's version", required = false)
    public val version: String? = null,
    @Option(description = "Name of the dependency in the version catalog", required = false)
    public val versionCatalogName: String? = null
) : AllwriteScanningRecipe<AddGradleDependency.GradleContext>(
    displayName = "Adds dependency to gradle",
    description = "Adds a dependency to gradle with support of TOML version catalog and build.kts.",
    visibility = RecipeVisibility.INTERNAL
) {

    public data class GradleContext(
        val versionCatalog: VersionCatalog = VersionCatalog(),
        var versionCatalogType: VersionCatalogType? = null,
        var moduleRoots: MutableSet<Path> = HashSet(),
    )

    override fun getInitialValue(ctx: ExecutionContext): GradleContext = GradleContext()

    override fun getScanner(acc: GradleContext): TreeVisitor<*, ExecutionContext> = object : TreeVisitor<Tree, ExecutionContext>() {
        override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
            val cursor = cursor
            val tree = tree as? SourceFile ?: return tree

            if (tree is Toml.Document && tree.sourcePath == TOML_VERSION_CATALOG_PATH) {
                acc.versionCatalogType = LibsToml
                ParseTomlVersionCatalog(acc.versionCatalog).visit(tree, p, cursor)
            }

            if (tree.isBuildGradleFile()) {
                acc.moduleRoots.add(tree.sourcePath.parent ?: Paths.get(""))
            }

            return tree
        }
    }

    // TODO: support settings.gradle version catalog
    override fun getVisitor(context: GradleContext): TreeVisitor<*, ExecutionContext> = object : TreeVisitor<Tree, ExecutionContext>() {
        private val existingDependency = versionCatalogName?.let { name ->
            context.versionCatalog.libraries.entries.firstOrNull { it.value.name == name }
        }

        override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
            // add a dependency to the version catalog
            if (tree is Toml.Document && tree.sourcePath == TOML_VERSION_CATALOG_PATH && context.versionCatalogType == LibsToml && existingDependency == null) {
                return AddTomlVersionCatalogDependency(
                    lib = Library(groupId, artifactId, null),
                    versionCatalogName = versionCatalogName(),
                    onVersionConflict = OnVersionConflict.IGNORE
                )
                    .visit(tree, p)
            }

            // add a dependency to the build.gradle
            if (tree.isBuildGradleFile()) {
                when (context.versionCatalogType) {
                    LibsToml -> {
                        val versionCatalogName = existingDependency?.key ?: versionCatalogName()
                        val library = Library(groupId, artifactId, null)
                        return AddVersionCatalogDependencyReference(configuration, library, versionCatalogName).visit(tree, p)
                    }

                    Gradle -> {
                        logger.warn("Gradle version catalog discovered, but it is not supported")
                        val library = Library(groupId, artifactId, version?.let { PlainVersion(it) })
                        return AddVersionCatalogDependencyReference(configuration, library, null).visit(tree, p)
                    }

                    null -> {
                        val library = Library(groupId, artifactId, version?.let { PlainVersion(it) })
                        return AddVersionCatalogDependencyReference(configuration, library, null).visit(tree, p)
                    }
                }
            }

            return tree
        }

        fun versionCatalogName(): String = versionCatalogName ?: "${groupId}.${artifactId}".toVersionCatalogName()
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(AddGradleDependency::class.java)
    }
}
