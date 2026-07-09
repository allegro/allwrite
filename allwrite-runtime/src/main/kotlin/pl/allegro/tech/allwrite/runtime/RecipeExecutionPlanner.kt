package pl.allegro.tech.allwrite.runtime

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe

@Single
internal class RecipeExecutionPlanner {

    fun plan(recipe: Recipe): RecipeExecutionPlan =
        RecipeExecutionPlan(splitIntoPhases(recipe).map(::RecipeExecutionPhase))

    private fun splitIntoPhases(recipe: Recipe): List<List<Recipe>> {
        val subRecipes = recipe.recipeList
        if (subRecipes.none { it is ClasspathAwareRecipe || it.needsExpansion() }) return listOf(listOf(recipe))

        val phases = mutableListOf<List<Recipe>>()
        var currentGroup = mutableListOf<Recipe>()
        for (subRecipe in subRecipes) {
            when {
                subRecipe is ClasspathAwareRecipe -> {
                    if (currentGroup.isNotEmpty()) {
                        phases.add(currentGroup)
                        currentGroup = mutableListOf()
                    }

                    phases.add(listOf(subRecipe))
                }

                subRecipe.needsExpansion() -> {
                    if (currentGroup.isNotEmpty()) {
                        phases.add(currentGroup)
                        currentGroup = mutableListOf()
                    }

                    val nestedPhases = splitIntoPhases(subRecipe)
                    logger.info { "Expanding nested recipe ${subRecipe.javaClass.simpleName} (${nestedPhases.size} sub-phases)" }
                    phases.addAll(nestedPhases)
                }

                else -> currentGroup.add(subRecipe)
            }
        }

        if (currentGroup.isNotEmpty()) {
            phases.add(currentGroup)
        }

        return phases
    }

    private fun Recipe.needsExpansion(): Boolean = recipeList.any { it is ClasspathAwareRecipe || it.needsExpansion() }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
