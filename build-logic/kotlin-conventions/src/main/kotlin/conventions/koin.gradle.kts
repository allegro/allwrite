package conventions

import libs

plugins {
    java
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(libs.koin.ksp.compiler)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.annotations)

    testImplementation(libs.koin.test)
    testImplementation(libs.kotest.extensions.koin)
}
