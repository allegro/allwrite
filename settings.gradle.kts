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
include("allwrite-completions")
include("allwrite-runtime")
include("allwrite-recipes")
include("allwrite-cli")
include("allwrite-spi")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

includeBuild("build-logic")
