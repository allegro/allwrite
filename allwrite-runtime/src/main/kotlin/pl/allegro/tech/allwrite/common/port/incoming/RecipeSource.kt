package pl.allegro.tech.allwrite.common.port.incoming

import org.openrewrite.Recipe
import org.openrewrite.config.RecipeDescriptor

public interface RecipeSource {
    public fun findAll(includeInternal: Boolean = false): List<RecipeDescriptor>
    public fun get(recipe: String): Recipe
}
