@file:Suppress("UnstableApiUsage")

import org.gradle.kotlin.dsl.dependencies
import java.nio.file.Paths

plugins {
    id("conventions.kotlin")
    id("conventions.koin")
    id("conventions.cli-distribution")
    alias(libs.plugins.kotlinx.serialization)
}

application {
    applicationName = "allwrite"
    mainClass = "pl.allegro.tech.allwrite.cli.MainKt"
}

dependencies {
    implementation(projects.allwriteRuntime)
    implementation(projects.allwriteRecipes)

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

/**
 * Example command for local testing:
 * ./gradlew :allwrite-cli:run --args "run spring-boot/upgrade 3 4" -Pworkdir=/Users/username/IdeaProjects/some-service
 */
tasks.run {
    workingDir = project.properties["workdir"]?.toString()?.let(::File) ?: Paths.get("").toFile()
}

tasks {
    distZip {
        archiveFileName = "allwrite.zip"
    }

    installDist {
        into(layout.buildDirectory.dir("installation"))
    }
}

