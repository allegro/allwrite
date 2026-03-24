package conventions

import libs

plugins {
    alias(libs.plugins.axion.release)
}

scmVersion {
    // sadly, we must disable this feature, because it requires newer JGit (we enforce the older version for JReleaser to work correctly)
    unshallowRepoOnCI = false
}

allprojects {
    group = "pl.allegro.tech.allwrite"
    version = rootProject.scmVersion.version
}
