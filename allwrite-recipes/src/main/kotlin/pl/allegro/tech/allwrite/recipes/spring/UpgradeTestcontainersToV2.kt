package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.recipes.gradle.ChangeGradleDependency

public fun upgradeTestcontainersToV2(): List<Recipe> =
    listOf(
        ChangeGradleDependency(
            oldGroupId = "org.testcontainers",
            oldArtifactId = "mongodb",
            newArtifactId = "testcontainers-mongodb",
        ),
        ChangeGradleDependency(
            oldGroupId = "org.testcontainers",
            oldArtifactId = "junit-jupiter",
            newArtifactId = "testcontainers-junit-jupiter",
        ),
    )
