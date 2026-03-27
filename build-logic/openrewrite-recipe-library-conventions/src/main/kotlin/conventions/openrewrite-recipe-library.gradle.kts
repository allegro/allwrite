@file:Suppress("UnstableApiUsage")

package conventions

import libs
import java.util.concurrent.TimeUnit.HOURS

plugins {
    alias(libs.plugins.openrewrite.recipe.library.base)
}

val pinnedModules = setOf(
    libs.lombok.get().module.toString(),
    libs.jsr305.get().module.toString(),
    libs.jetbrains.annotations.get().module.toString(),
    libs.assertj.core.get().module.toString()
)

configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor(24, HOURS)
        force(
            libs.lombok.get().toString(),
            libs.jsr305.get().toString(),
            libs.jetbrains.annotations.get().toString(),
            libs.assertj.core.get().toString()
        )
    }
}

afterEvaluate {
    configurations.forEach { config ->
        config.dependencies.forEach { dep ->
            if (dep.version == "latest.release" && "${dep.group}:${dep.name}" !in pinnedModules) {
                error(
                    "Dependency '${dep.group}:${dep.name}:latest.release' found in configuration '${config.name}'. " +
                        "Dynamic 'latest.release' versions break Gradle configuration cache. " +
                        "Pin it in the 'conventions.openrewrite-recipe-library' convention plugin."
                )
            }
        }
    }
}
