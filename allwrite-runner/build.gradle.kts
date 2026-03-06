@file:Suppress("UnstableApiUsage")

import org.gradle.kotlin.dsl.dependencies
import java.nio.file.Paths

plugins {
    id("conventions.kotlin")
    id("conventions.koin")
    id("conventions.jdk-provisioning")
    id("conventions.jreleaser")
    alias(libs.plugins.kotlinx.serialization)
}

application {
    applicationName = "allwrite-runner"
    mainClass = "pl.allegro.tech.allwrite.runner.MainKt"
}

dependencies {
    implementation(projects.allwriteRuntime)

    implementation(projects.allwriteRecipes)
    implementation(recipeClasspaths(projects.allwriteRecipes))

    // OpenRewrite
    implementation(libs.rewrite.java)

    // Ktor
    implementation(libs.bundles.ktor)

    // Serialization
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization)

    // Command line interface
    implementation(libs.clikt)
    implementation(libs.clikt.markdown)
    implementation(libs.markout)
    implementation(libs.markout.markdown)

    // github
    implementation(libs.github.client)

    // Reflection
    implementation(libs.kotlin.reflect)

    // Logging
    implementation(libs.slf4j)
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    // Tests
    testImplementation(testFixtures(projects.allwriteRuntime))
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.framework.datatest)
    testImplementation(libs.mockk)

    // kapt
    kapt(projects.allwriteCompletions)
    implementation(projects.allwriteCompletions)
}

testing.suites.register<JvmTestSuite>("e2e") {
    dependencies {
        implementation(libs.kotest.runner.junit5)
        implementation(libs.kotest.assertions)
        implementation(libs.apache.commons.lang3)
    }

    targets.all {
        testTask {
            dependsOn(tasks.jreleaserAssemble)
        }
    }
}

/**
 * Example command for local testing:
 * ./gradlew :allwrite-runner:run --args "run spring-boot/upgrade 3 4" -Pworkdir=/Users/username/IdeaProjects/some-service
 */
tasks.run {
    workingDir = project.properties["workdir"]?.toString()?.let(::File) ?: Paths.get("").toFile()
}

tasks {
    distZip {
        archiveFileName = "allwrite.zip"
    }

    distTar {
        archiveFileName = "allwrite.tar"
    }

    installDist {
        into(layout.buildDirectory.dir("installation"))
    }

    check {
        dependsOn("e2e")
    }
}
