package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.FindSourceFiles
import org.openrewrite.Preconditions
import org.openrewrite.TreeVisitor
import java.nio.file.Path

private val profileRegex = Regex("application-([^.]*)\\..*")

internal fun profile(filePath: Path): String? = profileRegex.matchEntire(filePath.fileName.toString())?.groupValues?.drop(1)?.firstOrNull()

internal val propertyFilePathExpressions = listOf(
    "**/application*.yaml",
    "**/application*.yml",
    "**/application*.properties",
)

internal fun findSpringPropertyFiles(): TreeVisitor<*, ExecutionContext> =
    propertyFilePathExpressions
        .map { FindSourceFiles(it).visitor }
        .let { Preconditions.or(*it.toTypedArray()) }
