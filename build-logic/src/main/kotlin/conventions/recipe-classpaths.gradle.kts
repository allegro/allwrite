@file:Suppress("UnstableApiUsage")

package conventions

plugins {
    java
}

/**
 * We add the extension to allow declaring recipe classpath dependencies in the recipeClasspaths {...} block
 */
val recipeClasspaths = objects.domainObjectContainer(RecipeClasspath::class)
extensions.add("recipeClasspaths", recipeClasspaths)

recipeClasspaths.all {
    /**
     * This configuration will be used under the hood to declare dependencies for recipe classpath.
     * When you write this:
     * ```
     * recipeClasspaths {
     *     register("pl.allegro.MyRecipe") {
     *         classpath("com.example:some-library:1.0.0")
     *     }
     * }
     * ```
     * It will declare the 'some-library' dependency for this configuration.
     */
    configurations.dependencyScope(dependencyScopeConfigurationName)

    /**
     * This configuration will be resolved by the recipeClasspathsJar
     */
    configurations.resolvable(resolvableConfigurationName) {
        extendsFrom(configurations[dependencyScopeConfigurationName])
        isTransitive = false
    }
}

/**
 * This task will resolve recipe classpaths nad put them into a jar
 */
val recipeClasspathsJar = tasks.register<Jar>("recipeClasspathsJar") {
    archiveClassifier = "recipe-classpaths"
    recipeClasspaths.all {
        val recipe = this

        val childCopySpec = copySpec {
            from(configurations[recipe.resolvableConfigurationName])
            into(recipe.name)
        }
        with(childCopySpec)
    }
}

/**
 * Outgoing variant that holds the recipeClasspathsJar
 */
configurations.consumable("recipeClasspaths") {
    outgoing.artifact(recipeClasspathsJar)
}

/**
 * Add recipeClasspathsJar to the test classpath, so we can access the classpaths in tests
 */
dependencies {
    testRuntimeOnly(files(recipeClasspathsJar))
}

