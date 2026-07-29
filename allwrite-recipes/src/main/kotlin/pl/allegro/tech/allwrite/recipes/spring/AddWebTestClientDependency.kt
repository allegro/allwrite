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
import kotlin.io.path.Path as KotlinPath

internal class AddWebTestClientDependency :
    AllwriteScanningRecipe<AddWebTestClientDependency.Context>(
        displayName = "Add WebTestClient dependency",
        description = "Adds the Spring Boot WebTestClient test dependency when it is used.",
        visibility = INTERNAL,
    ),
    ClasspathAwareRecipe {

    data class Context(
        val versionCatalog: VersionCatalog = VersionCatalog(),
        var versionCatalogType: VersionCatalogType? = null,
        var moduleRoots: MutableSet<Path> = HashSet(),
        var webTestClientDetected: Boolean = false,
    )

    override fun getInitialValue(ctx: ExecutionContext): Context = Context()

    override fun requireOnClasspath(): List<String> = listOf("spring-test-6")

    override fun getScanner(acc: Context): TreeVisitor<*, ExecutionContext> =
        object : TreeVisitor<Tree, ExecutionContext>() {
            private val findWebTestClient = Preconditions.check(
                Preconditions.or(
                    UsesType<ExecutionContext>(WEB_TEST_CLIENT_TYPE, false),
                    UsesType<ExecutionContext>(AUTO_CONFIGURE_WEB_TEST_CLIENT_TYPE, false),
                    UsesType<ExecutionContext>(RELOCATED_AUTO_CONFIGURE_WEB_TEST_CLIENT_TYPE, false),
                ),
                object : TreeVisitor<Tree, ExecutionContext>() {
                    override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                        acc.webTestClientDetected = true
                        return tree
                    }
                },
            )

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                val source = tree as? SourceFile ?: return tree
                findWebTestClient.visit(source, p, cursor)
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
        if (!context.webTestClientDetected) return TreeVisitor.noop<Tree, ExecutionContext>()
        val delegate = AddGradleDependency(
            configuration = "testImplementation",
            groupId = "org.springframework.boot",
            artifactId = WEB_TEST_CLIENT,
            versionCatalogName = WEB_TEST_CLIENT,
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
        private val TOML_VERSION_CATALOG_PATH = KotlinPath("gradle/libs.versions.toml")
        private const val WEB_TEST_CLIENT = "spring-boot-webtestclient"
        private const val WEB_TEST_CLIENT_TYPE = "org.springframework.test.web.reactive.server.WebTestClient"
        private const val AUTO_CONFIGURE_WEB_TEST_CLIENT_TYPE =
            "org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient"
        private const val RELOCATED_AUTO_CONFIGURE_WEB_TEST_CLIENT_TYPE =
            "org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient"
    }
}
