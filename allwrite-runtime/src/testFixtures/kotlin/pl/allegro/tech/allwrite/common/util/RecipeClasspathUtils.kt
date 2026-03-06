package pl.allegro.tech.allwrite.common.util

import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.test.RecipeSpec
import pl.allegro.tech.allwrite.common.RecipeClasspathResolver
import java.nio.file.Path

private val resolver = RecipeClasspathResolver()

fun RecipeSpec.withRecipeClasspath(): RecipeSpec {
    val recipeClasspath = recipe?.let(::classpathFor)
        ?: error(".withRecipeClasspath() must be called after .recipe()")

    return withClasspath(recipeClasspath)
}

fun RecipeSpec.withClasspathFor(recipeName: String): RecipeSpec {
    val recipeClasspath = classpathFor(recipeName)
    return withClasspath(recipeClasspath)
}

private fun RecipeSpec.withClasspath(classpath: List<Path>): RecipeSpec {
    parser(JavaParser.fromJavaVersion().classpath(classpath))
    parser(KotlinParser.builder().classpath(classpath))
    parser(GroovyParser.builder().classpath(classpath))
    return this
}

fun classpathFor(recipe: Recipe) =
    resolver.resolveClasspathTransitive(recipe)

fun classpathFor(recipeName: String) =
    resolver.resolveClasspathNonTransitive(recipeName)

inline fun <reified T> classpathFor(): List<Path> =
    classpathFor(T::class.qualifiedName ?: error("Can't provide classpath for unnamed class $${T::class}"))
