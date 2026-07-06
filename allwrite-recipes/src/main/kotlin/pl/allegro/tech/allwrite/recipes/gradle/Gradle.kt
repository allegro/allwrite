package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.groovy.tree.G
import org.openrewrite.kotlin.tree.K

internal fun Tree?.isBuildGradleFile(): Boolean {
    val sourcePath = (this as? SourceFile)?.sourcePath?.toString()
    if (sourcePath != null) {
        return sourcePath.endsWith("build.gradle") || sourcePath.endsWith("build.gradle.kts")
    }
    val isBuildGradle = this is G.CompilationUnit && this.sourcePath.toString().endsWith("build.gradle")
    val isBuildGradleKts = this is K.CompilationUnit && this.sourcePath.toString().endsWith("build.gradle.kts")
    return isBuildGradle || isBuildGradleKts
}
