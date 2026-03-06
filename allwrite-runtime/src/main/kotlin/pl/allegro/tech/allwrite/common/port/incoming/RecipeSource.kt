package pl.allegro.tech.allwrite.common.port.incoming

import org.openrewrite.Recipe
import org.openrewrite.config.RecipeDescriptor

public interface RecipeSource {
    public fun findAll(): List<RecipeDescriptor>
    public fun activate(recipe: String): Recipe
}
