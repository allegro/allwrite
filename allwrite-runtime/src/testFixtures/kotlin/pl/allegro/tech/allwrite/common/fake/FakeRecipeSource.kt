package pl.allegro.tech.allwrite.common.fake

import org.koin.core.annotation.Single
import org.openrewrite.Recipe
import org.openrewrite.RecipeException
import org.openrewrite.config.RecipeDescriptor
import pl.allegro.tech.allwrite.common.port.incoming.RecipeSource
import pl.allegro.tech.allwrite.common.port.incoming.tagPropertyOrNull
import pl.allegro.tech.allwrite.recipes.RecipeVisibility.PUBLIC

@Single
class FakeRecipeSource(val recipes: List<Recipe> = TEST_RECIPES) : RecipeSource {

    constructor(vararg recipes: Recipe) : this(listOf(*recipes))

    override fun findAll(includeInternal: Boolean): List<RecipeDescriptor> =
        recipes.map(Recipe::getDescriptor)
            .filter { includeInternal || PUBLIC.name.equals(it.tagPropertyOrNull("visibility"), ignoreCase = true) }

    override fun activate(recipe: String): Recipe =
        recipes
            .firstOrNull { it.name == recipe }
            ?: throw RecipeException("Recipes not found: $recipe")

    companion object {

        val SPRING_BOOT_3_TEST_RECIPE = FakeRecipe(
            id = "pl.allegro.tech.allwrite.recipes.spring-boot-3",
            displayName = "This recipe upgrades Spring Boot\n2 to 3",
            description = "Longer first description\nline break",
            tags = setOf("visibility:PUBLIC", "from:2", "to:3", "group:spring-boot", "recipe:upgrade")
        )
        val SPRING_BOOT_4_TEST_RECIPE = FakeRecipe(
            id = "pl.allegro.tech.allwrite.recipes.spring-boot-4",
            displayName = "Upgrade Spring Boot from 3 to 4",
            description = "This recipe upgrades Spring Boot\n3 to 4",
            tags = setOf("visibility:PUBLIC", "from:3", "to:4", "group:spring-boot", "recipe:upgrade")
        )
        val JACKSON_TEST_RECIPE = FakeRecipe(
            id = "pl.allegro.tech.allwrite.recipes.jackson",
            displayName = "Move from Jackson to kotlinx-serialization",
            description = "Move from Jackson to kotlinx-serialization.",
            tags = setOf("visibility:PUBLIC", "from:2", "to:3", "group:jackson", "recipe:upgrade")
        )
        val SETUP_CI_TEST_RECIPE = FakeRecipe(
            id = "pl.allegro.tech.allwrite.recipes.setup-ci",
            displayName = "Setup CI",
            description = "Introduce CI setup action",
            tags = setOf("visibility:PUBLIC", "group:workflows", "recipe:introduceSetupCi")
        )
        val EXPAND_MAPPINGS_TEST_RECIPE = FakeRecipe(
            id = "pl.allegro.tech.allwrite.recipes.yaml.ExpandMappings",
            displayName = "Expand YAML Mappings",
            description = "Expands YAML mappings",
            tags = setOf("visibility:INTERNAL")
        )
        val OPENREWRITE_TEST_RECIPE = FakeRecipe(
            id = "org.openrewrite.java.format.AutoFormat",
            displayName = "Auto Format",
            description = "Auto-formats Java code",
            tags = emptySet()
        )
        val PICNIC_TEST_RECIPE = FakeRecipe(
            id = "tech.picnic.errorprone.SomeRule",
            displayName = "Some Rule",
            description = "Some Picnic rule",
            tags = emptySet()
        )
        val TEST_RECIPES = listOf(
            SPRING_BOOT_3_TEST_RECIPE,
            SPRING_BOOT_4_TEST_RECIPE,
            JACKSON_TEST_RECIPE,
            SETUP_CI_TEST_RECIPE,
            EXPAND_MAPPINGS_TEST_RECIPE,
            OPENREWRITE_TEST_RECIPE,
            PICNIC_TEST_RECIPE
        )
    }
}
