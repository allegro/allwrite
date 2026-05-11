package pl.allegro.tech.allwrite.runtime.util

import org.openrewrite.Recipe

internal fun Recipe.withNestedRecipes(): List<Recipe> = listOf(this) + recipeList.flatMap { it.withNestedRecipes() }
