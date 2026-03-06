plugins {
    id("conventions.kotlin")
    id("conventions.koin")
}

dependencies {
    implementation(projects.allwriteRecipes)
    implementation(projects.allwriteRuntime)
    implementation(libs.kotlinx.serialization)
    implementation(libs.mustache)
    implementation(libs.semver)
    implementation(libs.kotlinpoet)
    implementation(libs.autoservice)

    kapt(libs.autoservice)
}
