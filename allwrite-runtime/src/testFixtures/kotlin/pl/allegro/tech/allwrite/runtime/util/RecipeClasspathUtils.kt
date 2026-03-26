package pl.allegro.tech.allwrite.runtime.util

import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.java.JavaParser
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.test.RecipeSpec
import pl.allegro.tech.allwrite.spi.ClasspathAwareRecipe

fun RecipeSpec.withRecipeClasspath(): RecipeSpec {
    val recipe = recipe ?: error(".withRecipeClasspath() must be called after .recipe()")
    val classpathAwareRecipe = recipe as? ClasspathAwareRecipe ?: error(".withRecipeClasspath() is only supported for ClasspathAwareRecipe")
    val artifacts = classpathAwareRecipe.requireOnClasspath().toTypedArray()
    val ctx = InMemoryExecutionContext()

    parser(JavaParser.fromJavaVersion().classpathFromResources(ctx, *artifacts))
    parser(KotlinParser.builder().classpathFromResources(ctx, *artifacts))
    parser(GroovyParser.builder().classpathFromResource(ctx, *artifacts))

    return this
}
