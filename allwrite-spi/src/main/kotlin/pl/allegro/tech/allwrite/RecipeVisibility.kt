package pl.allegro.tech.allwrite

public enum class RecipeVisibility {
    /**
     * For internal usage, can be executed only via ID:
     * ```
     * allwrite run --recipe pl.allegro.tech.recipes.SomeInternalRecipe
     * ```
     */
    INTERNAL,

    /**
     * For usage by end users. Recipes with a group and action can be executed via a friendly name:
     * ```
     * allwrite run group/friendlyName
     *
     * allwrite run --recipe pl.allegro.tech.recipes.SomePublicRecipe
     * ```
     */
    PUBLIC,
}
