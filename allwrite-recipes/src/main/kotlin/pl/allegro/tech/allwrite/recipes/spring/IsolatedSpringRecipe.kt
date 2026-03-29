package pl.allegro.tech.allwrite.recipes.spring

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.PUBLIC
import pl.allegro.tech.allwrite.runtime.port.incoming.RecipeSource
import kotlin.getValue

/**
 * A recipe that performs migration e.g. from Spring Boot 3.5 to Spring Boot 4.0 (without previous migrations like 3.4->3.5, 3.3->3.4, etc.)
 */
public abstract class IsolatedSpringRecipe(from: String, to: String) : AllwriteRecipe(
    displayName = "Migrate from Spring $from to Spring $to",
    visibility = PUBLIC,
    group = "springBoot",
    action = "upgrade",
    from = from,
    to = to,
), KoinComponent {

    private val recipeSource: RecipeSource by inject()

    private val majorVersion = metadata.to?.split(".")?.first()
    private val versionWithUnderscore = metadata.to?.replace(".", "_")

    override fun getRecipeList(): List<Recipe> {
        return recipeSource.get("org.openrewrite.java.spring.boot$majorVersion.UpgradeSpringBoot_$versionWithUnderscore")
            .recipeList
            .filterNot { it.name.contains("UpgradeSpringBoot") }
    }
}
