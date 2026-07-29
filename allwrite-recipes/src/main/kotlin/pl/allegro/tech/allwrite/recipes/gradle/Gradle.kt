package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.toml.tree.Toml
import java.nio.file.Path

private val TOML_VERSION_CATALOG_PATH: Path = Path.of("gradle/libs.versions.toml")

internal fun Tree?.isBuildGradleFile(): Boolean =
    (this as? SourceFile)?.sourcePath?.toString()?.let { sourcePath ->
        sourcePath.endsWith("build.gradle") || sourcePath.endsWith("build.gradle.kts")
    } == true

internal fun Tree?.isTomlVersionCatalogFile(): Boolean =
    this is Toml.Document && sourcePath == TOML_VERSION_CATALOG_PATH
