plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.apache.commons.compress)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.pgpainless)
}
