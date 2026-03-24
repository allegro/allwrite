plugins {
    `java-library`
    `maven-publish`
    `java-test-fixtures`
    signing
    alias(libs.plugins.openrewrite.recipe.library.base)
    id("conventions.kotlin")
    id("conventions.koin")
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

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            pom {
                name = "allwrite-recipes"
                description = "A collection of recipes, filling the gaps in vanilla OpenRewrite"
                url = "https://github.com/allegro/allwrite"
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                scm {
                    url = "https://github.com/allegro/allwrite"
                    connection = "scm:git@github.com:allegro/allwrite.git"
                    developerConnection = "scm:git@github.com:allegro/allwrite.git"
                }
                developers {
                    developer {
                        id = "radoslaw-panuszewski"
                        name = "Radosław Panuszewski"
                    }
                    developer {
                        id = "aleksandrserbin"
                        name = "Aleksandr Serbin"
                    }
                }
            }
        }
    }
}

signing {
    val gpgKeyId = System.getenv("GPG_KEY_ID")
    val gpgPrivateKey = System.getenv("GPG_PRIVATE_KEY")
    val gpgPrivateKeyPassword = System.getenv("GPG_PRIVATE_KEY_PASSWORD")

    if (gpgKeyId != null && gpgPrivateKey != null && gpgPrivateKeyPassword != null) {
        useInMemoryPgpKeys(gpgKeyId, gpgPrivateKey, gpgPrivateKeyPassword)
        sign(publishing.publications["library"])
    }
}
