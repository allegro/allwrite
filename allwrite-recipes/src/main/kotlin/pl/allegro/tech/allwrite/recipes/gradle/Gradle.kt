package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.Tree
import org.openrewrite.groovy.tree.G
import org.openrewrite.kotlin.tree.K

internal fun Tree?.isBuildGradleFile(): Boolean {
    val isBuildGradle = this is G.CompilationUnit && this.sourcePath.toString().endsWith("build.gradle")
    val isBuildGradleKts = this is K.CompilationUnit && this.sourcePath.toString().endsWith("build.gradle.kts")
    return isBuildGradle || isBuildGradleKts
}
