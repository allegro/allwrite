package pl.allegro.tech.allwrite.recipes.spring

import pl.allegro.tech.allwrite.recipes.gradle.UpdateGradleDependency

public fun upgradeGroovyToV5(): List<UpdateGradleDependency> = listOf(UPGRADE_GROOVY, UPGRADE_SPOCK_BOM_RECIPE)

private val UPGRADE_GROOVY: UpdateGradleDependency =
    UpdateGradleDependency(
        groupId = "org.apache.groovy",
        artifactId = "*",
        targetVersion = "5.0.7",
    )

private val UPGRADE_SPOCK_BOM_RECIPE: UpdateGradleDependency =
    UpdateGradleDependency(
        groupId = "org.spockframework",
        artifactId = "*",
        targetVersion = "2.4-groovy-5.0",
        sourceVersionPattern = "\\d+\\.\\d+.*",
    )
