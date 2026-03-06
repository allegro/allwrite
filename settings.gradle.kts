pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "allwrite"

include("allwrite-completions")
include("allwrite-runtime")
include("allwrite-recipes")
include("allwrite-runner")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
