package pl.allegro.tech.allwrite.recipes.java

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.PUBLIC
import pl.allegro.tech.allwrite.runtime.port.incoming.RecipeSource
import kotlin.getValue

/**
 * A recipe that performs migration e.g. from Java 21 to Java 25 (without previous migrations like 17->21, 11->17, etc.)
 */
public abstract class IsolatedJavaRecipe(from: Int, to: Int) : AllwriteRecipe(
    displayName = "Migrate from Java $from to Java $to",
    visibility = PUBLIC,
    group = "java",
    action = "upgrade",
    from = from.toString(),
    to = to.toString(),
), KoinComponent {

    private val recipeSource: RecipeSource by inject()

    override fun getRecipeList(): List<Recipe> =
        recipeSource.get("org.openrewrite.java.migrate.UpgradeToJava${metadata.to}")
            .recipeList
            .filterNot { it.name.startsWith("org.openrewrite.java.migrate.UpgradeToJava") }
}
