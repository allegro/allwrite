package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.Preconditions
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.gradle.search.FindDependency
import org.openrewrite.java.search.UsesType
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.AllwriteScanningRecipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.recipes.gradle.AddGradleDependency
import pl.allegro.tech.allwrite.recipes.gradle.LibsToml
import pl.allegro.tech.allwrite.recipes.gradle.ParseTomlVersionCatalog
import pl.allegro.tech.allwrite.recipes.gradle.VersionCatalog
import pl.allegro.tech.allwrite.recipes.gradle.VersionCatalogType
import pl.allegro.tech.allwrite.recipes.gradle.isBuildGradleFile
import pl.allegro.tech.allwrite.recipes.gradle.isTomlVersionCatalogFile
import java.nio.file.Path
import java.nio.file.Paths

public open class PreconditionsAwareAddDependency(
    displayName: String = "Add dependency when types are used",
    description: String = "Adds a Gradle dependency when one of the configured types is used.",
    @Option(description = "Classpath entries required to parse the configured types.", required = false)
    public val requiredClasspath: List<String> = emptyList(),
    @Option(description = "Fully qualified types whose usage triggers dependency insertion.", example = "com.example.MyType")
    public val requiredTypes: List<String> = emptyList(),
    @Option(
        description = "Gradle dependencies whose presence triggers dependency insertion, in groupId:artifactId format.",
        example = "io.rest-assured:rest-assured",
    )
    public val requiredDependencies: List<String> = emptyList(),
    @Option(description = "Gradle configuration to add the dependency to.", example = "testImplementation")
    public val configuration: String = "",
    @Option(description = "Group ID of the dependency to add.", example = "org.springframework.boot")
    public val groupId: String = "",
    @Option(description = "Artifact ID of the dependency to add.", example = "spring-boot-webtestclient")
    public val artifactId: String = "",
    @Option(description = "Dependency name in the Gradle version catalog.", required = false, example = "spring-boot-webtestclient")
    public val versionCatalogName: String = artifactId,
) : AllwriteScanningRecipe<PreconditionsAwareAddDependency.Context>(
    displayName = displayName,
    description = description,
),
    ClasspathAwareRecipe {

    public data class Context(
        val versionCatalog: VersionCatalog = VersionCatalog(),
        var versionCatalogType: VersionCatalogType? = null,
        var moduleRoots: MutableSet<Path> = HashSet(),
        var dependencyDetected: Boolean = false,
    )

    override fun getInitialValue(ctx: ExecutionContext): Context = Context()

    override fun requireOnClasspath(): List<String> = requiredClasspath

    override fun getScanner(acc: Context): TreeVisitor<*, ExecutionContext> {
        val dependencyCoordinates = dependencyCoordinates()
        require(requiredTypes.isNotEmpty() || dependencyCoordinates.isNotEmpty()) {
            "At least one type or dependency must be configured."
        }
        require(configuration.isNotBlank() && groupId.isNotBlank() && artifactId.isNotBlank()) {
            "Dependency configuration, group ID, and artifact ID must be configured."
        }
        return object : TreeVisitor<Tree, ExecutionContext>() {
            private val markDependencyDetected = object : TreeVisitor<Tree, ExecutionContext>() {
                override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                    acc.dependencyDetected = true
                    return tree
                }
            }
            private val findUsedType = requiredTypes
                .takeIf { it.isNotEmpty() }
                ?.let { types ->
                    Preconditions.check(
                        Preconditions.or(*types.map { UsesType<ExecutionContext>(it, false) }.toTypedArray()),
                        markDependencyDetected,
                    )
                }
            private val findDeclaredDependency = dependencyCoordinates
                .takeIf { it.isNotEmpty() }
                ?.let { dependencies ->
                    Preconditions.check(
                        Preconditions.or(
                            *dependencies.map {
                                FindDependency(it.groupId, it.artifactId, null, null, null).visitor
                            }.toTypedArray(),
                        ),
                        markDependencyDetected,
                    )
                }

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                val source = tree as? SourceFile ?: return tree
                findUsedType?.visit(source, p, cursor)
                findDeclaredDependency?.visit(source, p, cursor)
                if (source is Toml.Document && source.isTomlVersionCatalogFile()) {
                    acc.versionCatalogType = LibsToml
                    ParseTomlVersionCatalog(acc.versionCatalog).visit(source, p, cursor)
                    acc.dependencyDetected = acc.dependencyDetected ||
                        acc.versionCatalog.libraries.values.any { library ->
                            dependencyCoordinates.any { dependency ->
                                library.group == dependency.groupId && library.name == dependency.artifactId
                            }
                        }
                }
                if (source.isBuildGradleFile()) {
                    acc.moduleRoots.add(source.sourcePath.parent ?: Paths.get(""))
                }
                return tree
            }
        }
    }

    private fun dependencyCoordinates(): List<DependencyCoordinates> =
        requiredDependencies.map { dependency ->
            val (groupId, artifactId) = dependency.split(":").also { parts ->
                require(parts.size == 2 && parts.all { it.isNotBlank() }) {
                    "Detected dependencies must use the groupId:artifactId format."
                }
            }
            DependencyCoordinates(groupId, artifactId)
        }

    private data class DependencyCoordinates(
        val groupId: String,
        val artifactId: String,
    )

    override fun getVisitor(context: Context): TreeVisitor<*, ExecutionContext> {
        if (!context.dependencyDetected) return TreeVisitor.noop<Tree, ExecutionContext>()
        val delegate = AddGradleDependency(
            configuration = configuration,
            groupId = groupId,
            artifactId = artifactId,
            versionCatalogName = versionCatalogName,
        )
        val delegateContext = AddGradleDependency.GradleContext(
            versionCatalog = context.versionCatalog,
            versionCatalogType = context.versionCatalogType,
            moduleRoots = context.moduleRoots,
        )
        return object : TreeVisitor<Tree, ExecutionContext>() {
            private val delegateVisitor = delegate.getVisitor(delegateContext)

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? = delegateVisitor.visit(tree, p)
        }
    }
}
