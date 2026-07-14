package pl.allegro.tech.allwrite.recipes.spring

import pl.allegro.tech.allwrite.recipes.gradle.ChangeGradleDependency

public val UPGRADE_SPOCK_BOM_RECIPE: ChangeGradleDependency =
    ChangeGradleDependency(
        oldGroupId = "org.spockframework",
        oldArtifactId = "*",
        newGroupId = "org.spockframework",
        newArtifactId = "",
        newVersion = "2.4-groovy-5.0",
    )
