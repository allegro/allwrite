pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "allwrite"

include("allwrite-api")
include("allwrite-cli")
include("allwrite-completions")
include("allwrite-recipes")
include("allwrite-runtime")
include("allwrite-spi")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

includeBuild("build-logic")
