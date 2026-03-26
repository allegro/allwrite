plugins {
    id("conventions.kotlin")
    id("conventions.publishable-library")
}

publishableLibrary {
    name = "allwrite-api"
    description = "API for interacting with allwrite runtime (e.g. getting a recipe)"
}

dependencies {
    api(platform(libs.rewrite.bom))
    api(libs.rewrite.core)
    api(libs.semver)
}
