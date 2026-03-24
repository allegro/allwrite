plugins {
    `java-test-fixtures`
    alias(libs.plugins.openrewrite.recipe.library.base)
    id("conventions.kotlin")
    id("conventions.koin")
    id("conventions.publishable-library")
}

publishableLibrary {
    name = "allwrite-recipes"
    description = "A collection of recipes, filling the gaps in vanilla OpenRewrite"
}

recipeDependencies {
    parserClasspath("org.springframework.boot:spring-boot-test:3.3.+")

    parserClasspath("org.springframework:spring-context:6.+")
    parserClasspath("org.springframework:spring-core:6.+")
    parserClasspath("org.springframework:spring-beans:6.+")
    parserClasspath("org.springframework:spring-web:6.+")
    parserClasspath("org.springframework:spring-test:6.+")

    parserClasspath("jakarta.annotation:jakarta.annotation-api:2.+")
    parserClasspath("jakarta.inject:jakarta.inject-api:2.+")
}

dependencies {
    api(projects.allwriteSpi)
    implementation(projects.allwriteRuntime)

    implementation(platform(libs.rewrite.bom))
    implementation(libs.rewrite.java)
    implementation(libs.rewrite.java17)
    implementation(libs.rewrite.java21)
    implementation(libs.rewrite.java25)
    implementation(libs.rewrite.kotlin)
    implementation(libs.rewrite.groovy)
    implementation(libs.rewrite.yaml)
    implementation(libs.rewrite.toml)
    implementation(libs.rewrite.properties)
    implementation(libs.rewrite.spring)
    implementation(libs.rewrite.gradle) { exclude(module = "rewrite-groovy") }
    implementation(libs.rewrite.gradle.model)
    implementation(libs.rewrite.static.analysis)
    implementation(libs.snakeyaml)
    implementation(libs.logback.classic)
    implementation(libs.jackson.module.kotlin)

    testImplementation(testFixtures(projects.allwriteRuntime))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.assertj.core)
    testImplementation(libs.rewrite.test)
}

tasks {
    test {
        jvmArgs("-Xmx4g")
    }
}
