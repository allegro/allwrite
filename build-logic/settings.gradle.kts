@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.panuszewski.typesafe-conventions") version "0.10.0"
}

rootProject.name = "build-logic"
