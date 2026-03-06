package pl.allegro.tech.allwrite

import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.apache.commons.lang3.SystemUtils.IS_OS_LINUX
import org.apache.commons.lang3.SystemUtils.IS_OS_MAC_OSX
import org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS
import org.apache.commons.lang3.SystemUtils.OS_ARCH
import org.apache.commons.lang3.SystemUtils.OS_NAME
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString

/**
 * This test requires to have a complete distribution built by JReleaser.
 * As such, it verifies the end-to-end integration of the CLI app.
 */
class CliDistributionIntegrationSpec : FunSpec() {
    init {
        test("should run recipe that requires classpath") {
            // given
            val executablePath = Paths.get("build/jreleaser/assemble/allwrite/jlink")
                .resolve(getPlatformSpecificDirName())
                .resolveFirstChild()
                .resolve("bin/allwrite")
                .toAbsolutePath()
                .pathString

            // when
            val (exitCode, output) = execInTempDir(executablePath, "run", "springBoot/upgrade", "3", "4", "--log-level=DEBUG")

            // then
            exitCode shouldBe 0
            output shouldContain "Resolved recipe classpath"
        }
    }

    private fun getPlatformSpecificDirName(): String =
        when {
            IS_OS_MAC_OSX && OS_ARCH == "aarch64" -> "work-osx-aarch_64"
            IS_OS_MAC_OSX -> "work-osx-x86_64"
            IS_OS_WINDOWS -> "work-windows-x86_64"
            IS_OS_LINUX -> "work-linux-x86_64"
            else -> error("Unsupported platform: os.name=$OS_NAME, os.arch=$OS_ARCH")
        }

    private fun execInTempDir(vararg command: String): Pair<Int, String> {
        val process = ProcessBuilder()
            .directory(tempdir())
            .command(*command)
            .redirectErrorStream(true)
            .start()
        val exitCode = process.waitFor()
        val output = process.inputReader().use { it.readText() }
        println(output)
        return Pair(exitCode, output)
    }
}

private fun Path.resolveFirstChild() =
    Files.list(this).toList().first()
