package pl.allegro.tech.allwrite.recipes.spring

import pl.allegro.tech.allwrite.recipes.gradle.UpdateGradleDependency

public val UPGRADE_SPOCK_BOM_RECIPE: UpdateGradleDependency =
    UpdateGradleDependency(
        groupId = "org.spockframework",
        artifactId = "spock-bom",
        targetVersion = "2.4-groovy-5.0",
        sourceVersionPattern = "\\d+\\.\\d+.*",
    )
