package pl.allegro.tech.allwrite.runner.application

import com.github.ajalt.clikt.core.PrintMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import org.openrewrite.RecipeException
import pl.allegro.tech.allwrite.common.port.incoming.RecipeSource
import pl.allegro.tech.allwrite.runner.util.JSON
import java.io.File
import java.io.FileNotFoundException

@Single
internal class RecipeInstantiator(
    private val recipeSource: RecipeSource
) {

    fun instantiate(recipeName: String): Recipe =
        try {
            logger.info { "Activating recipe $recipeName" }
            recipeSource.get(recipeName)
        } catch (_: RecipeException) {
            throw PrintMessage("Recipe '$recipeName' not found. ${Messages.LIST_RECIPES_HINT}", statusCode = 1)
        }

    fun instantiateAll(recipeNames: List<String>): List<Recipe> {
        try {
            logger.info { "Activating recipes: $recipeNames" }
            return recipeNames.map(recipeSource::get)
        } catch (e: RecipeException) {
            throw PrintMessage("Recipe not found. ${Messages.LIST_RECIPES_HINT}", statusCode = 1)
        }
    }

    fun instantiateFrom(file: File): List<Recipe> {
        val recipeSet = try {
            JSON.decodeFromString<RecipeSet>(file.readText())
        } catch (_: FileNotFoundException) {
            throw PrintMessage("Specified file not found: ${file.absolutePath}", statusCode = 1)
        } catch (_: IllegalArgumentException) {
            throw PrintMessage("Can't parse file as JSON: ${file.absolutePath}", statusCode = 1)
        }
        return recipeSet.recipes.map(this::instantiate)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

@Serializable
private data class RecipeSet(val recipes: List<String>)
