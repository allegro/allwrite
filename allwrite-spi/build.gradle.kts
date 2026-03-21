plugins {
    `java-library`
    id("conventions.kotlin")
    id("conventions.koin")
}

dependencies {
    api(platform(libs.rewrite.bom))
    api(libs.rewrite.core)
}
