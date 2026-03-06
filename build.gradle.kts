plugins {
    alias(libs.plugins.axion.release)
    alias(libs.plugins.nexus.publish)
}

allprojects {
    group = "pl.allegro.tech.allwrite"
    version = rootProject.scmVersion.version
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
            snapshotRepositoryUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")
            username = System.getenv("SONATYPE_USERNAME")
            password = System.getenv("SONATYPE_PASSWORD")
        }
    }
}
