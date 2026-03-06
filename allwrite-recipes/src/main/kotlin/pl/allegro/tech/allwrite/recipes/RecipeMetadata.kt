package pl.allegro.tech.allwrite.recipes

internal class RecipeMetadata(
    displayName: String?,
    description: String?,
    val visibility: RecipeVisibility,
    val group: String?,
    val recipe: String?
) {
    val displayName: String = displayName ?: javaClass.simpleName
    val description: String = description ?: "${this.displayName}."

    val tags: Set<String> = buildSet {
        add("visibility:$visibility")
        if (visibility == RecipeVisibility.PUBLIC) {
            group?.let { add("group:$it") } ?: error("For public recipes, you must specify a 'group' tag!")
            recipe?.let { add("recipe:$it") } ?: error("For public recipes, you must specify a 'recipe' tag!")
        }
    }
}
