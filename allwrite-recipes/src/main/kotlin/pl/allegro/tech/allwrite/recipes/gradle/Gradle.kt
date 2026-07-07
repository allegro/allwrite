package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.SourceFile
import org.openrewrite.Tree

internal fun Tree?.isBuildGradleFile(): Boolean =
    (this as? SourceFile)?.sourcePath?.toString()?.let { sourcePath ->
        sourcePath.endsWith("build.gradle") || sourcePath.endsWith("build.gradle.kts")
    } == true
