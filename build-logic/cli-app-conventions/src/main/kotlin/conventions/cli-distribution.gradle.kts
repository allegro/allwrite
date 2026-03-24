@file:Suppress("UnstableApiUsage")

package conventions

import pl.allegro.tech.allwrite.buildlogic.ExtractJdkTask
import libs
import org.gradle.internal.jvm.Jvm
import org.jreleaser.gradle.plugin.tasks.AbstractJReleaserTask
import org.jreleaser.model.Active.ALWAYS

/**
 * This convention configures the build for the CLI distributions as a self-contained platform-specific
 * archives. To achieve this, it has to download JDKs for each of the target platforms and then build
 * a custom JRE with jlink.
 *
 * The distribution files will be uploaded as GitHub Release assets and referenced in Homebrew formula.
 */
plugins {
    application
    alias(libs.plugins.jreleaser)
}

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
            version = libs.versions.jvm.get()
        }
    }
    assemble {
        enabled = true
        jlink {
            create("allwrite") {
                enabled = true
                active = ALWAYS
                jdk {
                    path = Jvm.current().javaHome
                }
                javaArchive {
                    path = tasks.distZip.flatMap { it.archiveFile }.map { it.asFile.absolutePath }
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
                rootProject.tasks.withType<ExtractJdkTask>().all {
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
                active = ALWAYS
                formulaName = "allwrite"
                multiPlatform = true

                templateDirectory = file("src/jreleaser/templates")
                skipTemplate("README.md.tpl")

                repository.repoOwner = "allegro"
                repository.name = "homebrew-tap"
                repository.commitMessage = "Release {{distributionName}} {{tagName}}"
                repository.tagName = "{{distributionName}}-{{tagName}}"
                commitAuthor {
                    name = "allegro-homebrew[bot]"
                    email = "269041864+allegro-homebrew[bot]@users.noreply.github.com"
                }
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
    jreleaserAssemble {
        dependsOn(distZip, rootProject.tasks["provisionJdks"])
    }

    withType<AbstractJReleaserTask> {
        notCompatibleWithConfigurationCache("Uses Task.project at runtime")
    }
}
