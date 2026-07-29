package pl.allegro.tech.allwrite

public enum class RecipeVisibility {
    /**
     * Controls CLI recipe discovery and execution; it does not affect Kotlin or Java API visibility.
     *
     * Internal recipes can be executed only via ID:
     * ```
     * allwrite run --recipe pl.allegro.tech.recipes.SomeInternalRecipe
     * ```
     */
    INTERNAL,

    /**
     * Public recipes are shown by default by the CLI and must provide group and action coordinates.
     * They can be executed via a friendly name:
     * ```
     * allwrite run group/friendlyName
     * ```
     *
     * And via an ID:
     * ```
     * allwrite run --recipe pl.allegro.tech.recipes.SomePublicRecipe
     * ```
     */
    PUBLIC,
}
