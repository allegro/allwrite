package pl.allegro.tech.allwrite.kapt.generators

import com.squareup.kotlinpoet.PropertySpec
import org.openrewrite.config.RecipeDescriptor

public interface CompletionGenerator {
    public fun generate(recipeDescriptors: List<RecipeDescriptor>): PropertySpec
}
