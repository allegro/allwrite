plugins {
    `java-library`
    `java-test-fixtures`
    id("conventions.kotlin")
    id("conventions.koin")
}

dependencies {
    api(projects.allwriteSpi)

    // OpenRewrite
    api(platform(libs.rewrite.bom))
    api(libs.rewrite.core)
    implementation(libs.rewrite.java)
    implementation(libs.rewrite.java11)
    implementation(libs.rewrite.java17)
    implementation(libs.rewrite.kotlin)
    implementation(libs.rewrite.properties)
    implementation(libs.rewrite.spring)
    implementation(libs.rewrite.gradle)
    implementation(libs.rewrite.toml)

    // Semver
    api(libs.semver)

    // Logging
    implementation(libs.kotlin.logging)

    // Tests
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions)

    testFixturesApi(platform(libs.koin.bom))
    testFixturesApi(libs.koin.annotations)
    testFixturesApi(libs.koin.test)
    testFixturesApi(libs.kotest.extensions.koin)
    testFixturesApi(libs.mockk)
    testFixturesApi(libs.rewrite.test)
    testFixturesImplementation(libs.rewrite.java)
    testFixturesImplementation(libs.rewrite.kotlin)
    testFixturesImplementation(libs.rewrite.groovy)
}
