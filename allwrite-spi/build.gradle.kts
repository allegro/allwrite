plugins {
    id("conventions.kotlin")
    id("conventions.koin")
    id("conventions.publishable-library")
}

publishableLibrary {
    name = "allwrite-spi"
    description = "A service provider interface for implementing recipes aware of allwrite features (e.g. declaring friendly names)"
}

dependencies {
    api(platform(libs.rewrite.bom))
    api(libs.rewrite.core)
}
