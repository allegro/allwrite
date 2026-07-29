package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.Preconditions
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.java.search.UsesType
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.AllwriteScanningRecipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.INTERNAL
import pl.allegro.tech.allwrite.recipes.gradle.AddGradleDependency
import pl.allegro.tech.allwrite.recipes.gradle.LibsToml
import pl.allegro.tech.allwrite.recipes.gradle.ParseTomlVersionCatalog
import pl.allegro.tech.allwrite.recipes.gradle.VersionCatalog
import pl.allegro.tech.allwrite.recipes.gradle.VersionCatalogType
import pl.allegro.tech.allwrite.recipes.gradle.isBuildGradleFile
import java.nio.file.Path
import java.nio.file.Paths

internal class AddRestTestClientDependency :
    AllwriteScanningRecipe<AddRestTestClientDependency.Context>(
        displayName = "Add REST test client dependency",
        description = "Adds the Spring Boot REST test client dependency when TestRestTemplate is used.",
        visibility = INTERNAL,
    ),
    ClasspathAwareRecipe {

    data class Context(
        val versionCatalog: VersionCatalog = VersionCatalog(),
        var versionCatalogType: VersionCatalogType? = null,
        var moduleRoots: MutableSet<Path> = HashSet(),
        var restTestClientDetected: Boolean = false,
    )

    override fun getInitialValue(ctx: ExecutionContext): Context = Context()

    override fun requireOnClasspath(): List<String> = listOf("spring-boot-test-3")

    override fun getScanner(acc: Context): TreeVisitor<*, ExecutionContext> =
        object : TreeVisitor<Tree, ExecutionContext>() {
            private val findRestTestClient = Preconditions.check(
                UsesType<ExecutionContext>(TEST_REST_TEMPLATE_TYPE, false),
                object : TreeVisitor<Tree, ExecutionContext>() {
                    override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                        acc.restTestClientDetected = true
                        return tree
                    }
                },
            )

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                val source = tree as? SourceFile ?: return tree
                findRestTestClient.visit(source, p, cursor)
                if (source is Toml.Document && source.sourcePath == TOML_VERSION_CATALOG_PATH) {
                    acc.versionCatalogType = LibsToml
                    ParseTomlVersionCatalog(acc.versionCatalog).visit(source, p, cursor)
                }
                if (source.isBuildGradleFile()) {
                    acc.moduleRoots.add(source.sourcePath.parent ?: Paths.get(""))
                }
                return tree
            }
        }

    override fun getVisitor(context: Context): TreeVisitor<*, ExecutionContext> {
        if (!context.restTestClientDetected) return TreeVisitor.noop<Tree, ExecutionContext>()
        val delegate = AddGradleDependency(
            configuration = "testImplementation",
            groupId = "org.springframework.boot",
            artifactId = REST_TEST_CLIENT,
            versionCatalogName = REST_TEST_CLIENT,
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

    private companion object {
        private val TOML_VERSION_CATALOG_PATH = Path.of("gradle/libs.versions.toml")
        private const val REST_TEST_CLIENT = "spring-boot-resttestclient"
        private const val TEST_REST_TEMPLATE_TYPE = "org.springframework.boot.test.web.client.TestRestTemplate"
    }
}
