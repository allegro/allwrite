plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
    id("conventions.kotlin")
    id("conventions.koin")
    id("conventions.recipe-classpaths")
}

recipeClasspaths {
    register("spring-framework") {
        classpath("org.springframework:spring-context:6.2.17")
        classpath("org.springframework:spring-core:6.2.17")
        classpath("org.springframework:spring-beans:6.2.17")
        classpath("org.springframework:spring-web:6.2.17")
        classpath("jakarta.annotation:jakarta.annotation-api:2.1.1")
        classpath("jakarta.inject:jakarta.inject-api:2.0.1")
    }

    register("pl.allegro.tech.allwrite.recipes.spring.DeleteSpringPropertyFromSpringAnnotations") {
        classpath("org.springframework.boot:spring-boot-test:3.3.0")
        classpath("org.springframework:spring-test:6.2.17")
        classpath("org.springframework:spring-core:6.2.17")
        classpath("org.springframework:spring-context:6.2.17")
    }

    register("pl.allegro.tech.allwrite.recipes.spring.QualifyVariable") {
        classpath("org.springframework:spring-beans:6.2.17")
    }
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

kotlin {
    compilerOptions {
        javaParameters = true
    }
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
}

tasks {
    test {
        jvmArgs("-Xmx4g")
    }
}
