package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.recipes.gradle.UpdateGradleDependency

public class SpringBoot4_0 : IsolatedSpringRecipe(from = "3.5", to = "4.0") {

    override fun getRecipeList(): List<Recipe> =
        super.getRecipeList() +
            AddNonNullableTypeBoundsToSpringRepositories() +
            ReplaceStatusCodeValue() +
            UpdateGradleDependency(
                groupId = "org.spockframework",
                artifactId = "spock-bom",
                targetVersion = "2.4-groovy-5.0",
                sourceVersionPattern = "\\d+\\.\\d+.*",
            )
}
