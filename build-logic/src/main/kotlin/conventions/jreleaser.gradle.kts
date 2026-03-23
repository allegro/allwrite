@file:Suppress("UnstableApiUsage")

package conventions

import ExtractJdkTask
import libs
import org.gradle.internal.jvm.Jvm
import org.jreleaser.model.Active
import java.net.URI

/**
 * This convention configures the build for the CLI distributions as a self-contained platform-specific
 * archives. To achieve this, it has to download JDKs for each of the target platforms and then build
 * a custom JRE with jlink.
 *
 * The distribution files will be uploaded as GitHub Release assets and referenced in Homebrew formula.
 */
plugins {
    application
    `maven-publish`
    alias(libs.plugins.jreleaser)
}

/**
 * Configure JReleaser to build JLink images and Homebrew formulae
 */
jreleaser {
    gitRootSearch = true
    project {
        description = "Automated code migrations runner"
        copyright = "Allegro"
        website = "https://github.com/allegro/allwrite"
        docsUrl = "https://github.com/allegro/allwrite"
        license = "Apache-2.0"
        authors = listOf("Allegro")
        java {
            mainClass = application.mainClass
            groupId = group.toString()
            artifactId = project.name
            version = "21"
        }
    }
    assemble {
        enabled = true
        jlink {
            create("allwrite") {
                enabled = true
                active = Active.ALWAYS
                jdk {
                    path = Jvm.current().javaHome
                }
                mainJar {
                    path = tasks.jar.flatMap { it.archiveFile }
                }
                jars {
                    pattern =
                        tasks.installDist
                            .map { it.destinationDir }
                            .map { it.resolve("lib").absolutePath + "/*" }
                }
                moduleNames.addAll(
                    "java.base",
                    "java.instrument",
                    "java.naming",
                    "java.logging",
                    "java.xml", // logback
                    "java.compiler", // openrewrite-java
                    "jdk.compiler", // openrewrite-java
                    "java.desktop", // snake-yaml
                    "jdk.unsupported",
                    "java.management",
                    "jdk.crypto.ec", // EC for TLS
                    "jdk.crypto.cryptoki"
                )

                // For each downloaded JDK, configure it as a JLink target in JReleaser
                tasks.withType<ExtractJdkTask>().all {
                    val task = this
                    targetJdk {
                        path = task.targetDir.flatMap { it.file(task.target.map { it.jdkPath }) }
                        platform = task.target.map { it.jreleaserName }
                    }
                }
            }
        }
        packagers {
            brew {
                active = Active.ALWAYS
                formulaName = "allwrite"
                multiPlatform = true
                repository.repoOwner = "allegro"
                repository.name = "homebrew-tap"
                templateDirectory = file("src/jreleaser/templates")
            }
        }
    }
    release {
        github {
            releaseName = "v${project.version}"
            overwrite = false
            update {
                enabled = true
                section("ASSETS")
            }
            skipTag = true
        }
    }
}

tasks {
    jreleaserAssemble.configure {
        dependsOn(jar, installDist, tasks["provisionJdks"])
        mkdir(layout.buildDirectory.dir("jreleaser"))
        notCompatibleWithConfigurationCache("Uses Task.project at runtime")
    }

    jreleaserPackage.configure {
        notCompatibleWithConfigurationCache("Uses Task.project at runtime")
    }

    build {
        dependsOn(jreleaserAssemble, jreleaserPackage)
    }
}
