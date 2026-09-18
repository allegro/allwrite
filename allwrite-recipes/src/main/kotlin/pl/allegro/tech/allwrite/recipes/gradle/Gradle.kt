package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.Cursor
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.java.tree.J
import org.openrewrite.toml.tree.Toml
import java.nio.file.Path

private val TOML_VERSION_CATALOG_PATH: Path = Path.of("gradle/libs.versions.toml")

internal fun Tree?.isBuildGradleFile(): Boolean =
    (this as? SourceFile)?.sourcePath?.toString()?.let { sourcePath ->
        sourcePath.endsWith("build.gradle") || sourcePath.endsWith("build.gradle.kts")
    } == true

internal fun Cursor.isPluginsBlock(block: J.Block): Boolean {
    val pluginsMethod = firstEnclosing(J.MethodInvocation::class.java)
    val pluginsLambda = firstEnclosing(J.Lambda::class.java)
    return pluginsMethod?.simpleName == "plugins" &&
        pluginsLambda?.body == block &&
        pluginsMethod.arguments.any { it == pluginsLambda }
}

internal fun Tree?.isTomlVersionCatalogFile(): Boolean = this is Toml.Document && sourcePath == TOML_VERSION_CATALOG_PATH
