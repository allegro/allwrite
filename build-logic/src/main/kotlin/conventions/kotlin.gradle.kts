@file:Suppress("UnstableApiUsage")

package conventions

import libs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.test.logger)
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
    explicitApi()
}

tasks {
    afterEvaluate {
        withType<JavaCompile>().configureEach {
            options.release = libs.versions.jvm.get().toInt()
        }
    }

    withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll("-opt-in=kotlin.io.path.ExperimentalPathApi")
        }
    }
}

testing {
    suites.withType<JvmTestSuite>().configureEach {
        useJUnitJupiter()
    }
}

testlogger {
    showPassed = false
}
