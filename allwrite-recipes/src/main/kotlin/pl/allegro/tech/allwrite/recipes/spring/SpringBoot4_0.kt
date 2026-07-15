package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.Recipe

public class SpringBoot4_0 : IsolatedSpringRecipe(from = "3.5", to = "4.0") {

    override fun getRecipeList(): List<Recipe> =
        super.getRecipeList() +
            AddNonNullableTypeBoundsToSpringRepositories() +
            ReplaceStatusCodeValue() +
            ChangeSpringBoot4WebServerTypes() +
            upgradeGroovyToV5()
}
