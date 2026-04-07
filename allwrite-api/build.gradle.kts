plugins {
    id("conventions.kotlin")
    id("conventions.publishable-library")
}

publishableLibrary {
    name = "allwrite-api"
    description = "Public API for allwrite runtime - incoming port interfaces"
}

dependencies {
    api(platform(libs.rewrite.bom))
    api(libs.rewrite.core)

    api(libs.semver)
}
