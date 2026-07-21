package pl.allegro.tech.allwrite.recipes.spring

import pl.allegro.tech.allwrite.recipes.gradle.ChangeGradleDependency

public fun upgradeGroovyToV5(): List<ChangeGradleDependency> = listOf(UPGRADE_GROOVY, UPGRADE_SPOCK_BOM_RECIPE)

private val UPGRADE_GROOVY: ChangeGradleDependency =
    ChangeGradleDependency(
        oldGroupId = "org.apache.groovy",
        oldArtifactId = "*",
        newGroupId = "org.apache.groovy",
        newArtifactId = "",
        newVersion = "5.0.7",
    )

private val UPGRADE_SPOCK_BOM_RECIPE: ChangeGradleDependency =
    ChangeGradleDependency(
        oldGroupId = "org.spockframework",
        oldArtifactId = "*",
        newGroupId = "org.spockframework",
        newArtifactId = "",
        newVersion = "2.4-groovy-5.0",
    )
