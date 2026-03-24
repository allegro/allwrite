package conventions

import pl.allegro.tech.allwrite.buildlogic.PublishableLibraryExtension

plugins {
    `java-library`
    `maven-publish`
    signing
}

val extension = extensions.create<PublishableLibraryExtension>("publishableLibrary")

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            pom {
                name = extension.name
                description = extension.description
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

