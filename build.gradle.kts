plugins {
    id("conventions.versioning")
    id("conventions.maven-central")
    id("conventions.jdk-provisioning")
}

buildscript {
    dependencies {
        // unfortunately, JReleaser requires very old version of JGit (newer one is brought by axion-release-plugin)
        classpath("org.eclipse.jgit:org.eclipse.jgit") { version { strictly("5.13.0.202109080827-r") } }
    }
}

