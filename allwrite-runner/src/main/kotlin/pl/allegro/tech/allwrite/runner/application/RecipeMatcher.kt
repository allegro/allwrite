package pl.allegro.tech.allwrite.runner.application

import com.github.zafarkhaja.semver.Version
import org.koin.core.annotation.Single
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.common.port.incoming.RecipeCoordinates
import pl.allegro.tech.allwrite.common.port.incoming.RecipeSource
import pl.allegro.tech.allwrite.common.port.incoming.getFromVersion
import pl.allegro.tech.allwrite.common.port.incoming.getToVersion
import pl.allegro.tech.allwrite.common.port.incoming.tagPropertyOrNull

@Single
internal class RecipeMatcher(
    private val recipeSource: RecipeSource
) {

    fun findMatching(coordinates: RecipeCoordinates): List<RecipeDescriptor> {
        val (group, recipe, from, to) = coordinates

        val descriptors = recipeSource.findAll()
        val groupRecipes = descriptors
            .filter { group == it.tagPropertyOrNull("group") }
            .filter { recipe == it.tagPropertyOrNull("recipe") }

        return findExactMatch(from, to, groupRecipes)?.let(::listOf)
            ?: findAllMatching(from, to, groupRecipes)
    }

    private fun findExactMatch(from: Version?, to: Version?, recipes: List<RecipeDescriptor>): RecipeDescriptor? =
        recipes
            .filter { from != null && from.isSameMinorVersionAs(it.getFromVersion()) }
            .firstOrNull { to != null && to.isSameMinorVersionAs(it.getToVersion()) }

    private fun findAllMatching(from: Version?, to: Version?, recipes: List<RecipeDescriptor>): List<RecipeDescriptor> =
        recipes
            .filter {
                val recipeVersionFrom = it.getFromVersion()
                val recipeVersionTo = it.getToVersion()

                atLeastOneNullOr(from, recipeVersionFrom) { v1, v2 -> v1.majorVersion() <= v2.majorVersion() } &&
                        atLeastOneNullOr(to, recipeVersionTo) { v1, v2 -> v1.isHigherThanOrEquivalentTo(v2) } &&
                        atLeastOneNullOr(recipeVersionTo, from) { v1, v2 -> v1.isHigherThan(v2) }
            }
            .sortedWith(nullsLast(compareBy(RecipeDescriptor::getFromVersion, RecipeDescriptor::getToVersion)))

    private inline fun <A, B> atLeastOneNullOr(a: A?, b: B?, block: (A, B) -> Boolean): Boolean =
        (a == null || b == null || block(a, b))
}
