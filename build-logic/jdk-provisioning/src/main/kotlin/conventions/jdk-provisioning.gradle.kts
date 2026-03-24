package conventions

import pl.allegro.tech.allwrite.buildlogic.jdk.ExtractJdkTask
import pl.allegro.tech.allwrite.buildlogic.jdk.FetchJdkTask
import pl.allegro.tech.allwrite.buildlogic.jdk.VerifyJdkTask
import pl.allegro.tech.allwrite.buildlogic.jdk.JdkDistribution
import java.nio.file.Path

val provisionJdksTask = tasks.register("provisionJdks")

JdkDistribution.entries.forEach { dist ->
    val extractedJdkDir = layout.buildDirectory / "jdk" / "extracted" / dist.name
    val fetchedJdkDir = layout.buildDirectory / "jdk" / "fetched" / dist.name
    val distributionPath = fetchedJdkDir / "jdk.${dist.archiveFormat}"
    val signaturePath = fetchedJdkDir / "jdk.sig"
    val certPath = fetchedJdkDir / "cert"

    val fetchTask = tasks.register<FetchJdkTask>("fetchJdk${dist.taskSuffix}") {
        this.target = dist
        this.distributionPath = distributionPath.asPath
        this.signaturePath = signaturePath.asPath
        this.certPath = certPath.asPath
    }
    val verifyTask = tasks.register<VerifyJdkTask>("verifyJdk${dist.taskSuffix}") {
        this.distributionPath = distributionPath.asPath
        this.signaturePath = signaturePath.asPath
        this.certPath = certPath.asPath
        dependsOn(fetchTask)
    }
    val extractTask = tasks.register<ExtractJdkTask>("extractJdk${dist.taskSuffix}") {
        this.target = dist
        this.distributionPath = distributionPath.asPath
        this.targetDir = extractedJdkDir
        dependsOn(verifyTask)
    }

    provisionJdksTask.configure {
        dependsOn(extractTask)
    }
}

operator fun Provider<Directory>.div(path: String): Provider<Directory> =
    map { it.dir(path) }

val Provider<Directory>.asPath: Provider<Path>
    get() = map { it.asFile.toPath() }
