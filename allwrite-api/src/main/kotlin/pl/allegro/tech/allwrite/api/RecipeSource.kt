package pl.allegro.tech.allwrite.api

import org.openrewrite.Recipe
import org.openrewrite.config.RecipeDescriptor

public interface RecipeSource {
    public fun findAll(): List<RecipeDescriptor>
    public fun get(recipe: String): Recipe
}
