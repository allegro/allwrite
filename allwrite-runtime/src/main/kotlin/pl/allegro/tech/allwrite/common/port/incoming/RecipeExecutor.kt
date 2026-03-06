package pl.allegro.tech.allwrite.common.port.incoming

import org.openrewrite.Recipe
import java.nio.file.Path

public interface RecipeExecutor {
    public fun execute(recipe: Recipe, inputFiles: List<Path>, failOnError: Boolean)
}
