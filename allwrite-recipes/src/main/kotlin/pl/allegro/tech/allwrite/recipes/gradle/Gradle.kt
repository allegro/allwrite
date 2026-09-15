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

internal fun Cursor.isPluginsBlock(block: J.Block): Boolean = isGradleBlock(block, "plugins")

internal fun Cursor.isDependenciesBlock(block: J.Block): Boolean = isGradleBlock(block, "dependencies")

private fun Cursor.isGradleBlock(block: J.Block, blockName: String): Boolean {
    val method = firstEnclosing(J.MethodInvocation::class.java)
    val lambda = firstEnclosing(J.Lambda::class.java)
    return method?.simpleName == blockName &&
        lambda?.body == block &&
        method.arguments.any { it == lambda }
}

internal fun J.matchesGradleDependencyReference(aliases: Set<String>, coordinates: Set<Pair<String, String>>): Boolean =
    when (this) {
        is J.FieldAccess -> aliases.any { toString() == "libs.${it.toVersionCatalogReference()}" }
        is J.Literal -> (value as? String)?.let { coordinatesString ->
            val parts = coordinatesString.split(":")
            parts.size >= 2 && (parts[0] to parts[1]) in coordinates
        } == true
        is J.MethodInvocation ->
            if (simpleName == "platform" || simpleName == "enforcedPlatform") {
                arguments.firstOrNull()?.matchesGradleDependencyReference(aliases, coordinates) == true
            } else {
                select?.matchesGradleDependencyReference(aliases, coordinates) == true && arguments.isEmpty()
            }
        else -> false
    }

internal fun Tree?.isTomlVersionCatalogFile(): Boolean = this is Toml.Document && sourcePath == TOML_VERSION_CATALOG_PATH
