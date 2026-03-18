package pl.allegro.tech.allwrite.recipes

public enum class RecipeVisibility {
    /**
     * For internal usage, can be executed only via ID:
     * ```
     * allwrite run --recipe pl.allegro.tech.recipes.SomeInternalRecipe
     * ```
     */
    INTERNAL,

    /**
     * For usage by the end users, can be executed via friendly name and via ID:
     * ```
     * allwrite run group/friendlyName
     *
     * allwrite run --recipe pl.allegro.tech.recipes.SomePublicRecipe
     * ```
     */
    PUBLIC
}
