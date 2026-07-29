package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.Preconditions
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.gradle.search.FindDependency
import org.openrewrite.toml.tree.Toml
import pl.allegro.tech.allwrite.AllwriteScanningRecipe
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

internal class AddRestAssuredSpringWebTestClientEntry :
    AllwriteScanningRecipe<AddRestAssuredSpringWebTestClientEntry.Context>(
        displayName = "Add Rest Assured Spring Web Test Client dependency",
        description = "Adds io.rest-assured:spring-web-test-client when io.rest-assured:rest-assured is present.",
        visibility = INTERNAL,
    ) {

    internal data class Context(
        val versionCatalog: VersionCatalog = VersionCatalog(),
        var versionCatalogType: VersionCatalogType? = null,
        var moduleRoots: MutableSet<Path> = HashSet(),
        var restAssuredDetected: Boolean = false,
    )

    override fun getInitialValue(ctx: ExecutionContext): Context = Context()

    override fun getScanner(acc: Context): TreeVisitor<*, ExecutionContext> =
        object : TreeVisitor<Tree, ExecutionContext>() {
            private val findRestAssured = Preconditions.check(
                FindDependency(
                    REST_ASSURED_GROUP,
                    REST_ASSURED_ARTIFACT,
                    null,
                    null,
                    null,
                ).visitor,
                object : TreeVisitor<Tree, ExecutionContext>() {
                    override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                        acc.restAssuredDetected = true
                        return tree
                    }
                },
            )

            override fun visit(tree: Tree?, p: ExecutionContext): Tree? {
                val source = tree as? SourceFile ?: return tree
                if (source is Toml.Document && source.sourcePath == TOML_VERSION_CATALOG_PATH) {
                    acc.versionCatalogType = LibsToml
                    ParseTomlVersionCatalog(acc.versionCatalog).visit(source, p, cursor)
                }
                if (source.isBuildGradleFile()) {
                    acc.moduleRoots.add(source.sourcePath.parent ?: Paths.get(""))
                    findRestAssured.visit(source, p, cursor)
                }
                return tree
            }
        }

    override fun getVisitor(context: Context): TreeVisitor<*, ExecutionContext> {
        if (!context.restAssuredDetected) return TreeVisitor.noop<Tree, ExecutionContext>()
        val delegate = AddGradleDependency(
            configuration = "testImplementation",
            groupId = REST_ASSURED_GROUP,
            artifactId = SPRING_WEB_TEST_CLIENT_ARTIFACT,
            versionCatalogName = SPRING_WEB_TEST_CLIENT_ARTIFACT,
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
        val TOML_VERSION_CATALOG_PATH = KotlinPath("gradle/libs.versions.toml")
        const val REST_ASSURED_GROUP = "io.rest-assured"
        const val REST_ASSURED_ARTIFACT = "rest-assured"
        const val SPRING_WEB_TEST_CLIENT_ARTIFACT = "spring-web-test-client"
    }
}
