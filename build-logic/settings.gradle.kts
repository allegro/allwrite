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


// conventions from this subproject will land on every buildscript classpath (because they are applied to root project)
// modifying those conventions may be painful, because it will trigger reconfiguration of every buildscript
include("root-project-conventions")

include("cli-app-conventions")
include("openrewrite-recipe-library-conventions")
include("publishable-library-conventions")
include("kotlin-conventions")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
