package pl.allegro.tech.allwrite.cli.infrastructure.os

import org.koin.core.annotation.Single

internal interface SystemCommandExecutor {
    fun exec(command: String): String
}

@Single
internal class ProcessBuilderSystemCommandExecutor : SystemCommandExecutor {

    override fun exec(command: String): String {
        val processBuilder = ProcessBuilder(command.split(" "))
            .redirectErrorStream(true)

        val process = processBuilder.start()

        val output = process.inputReader().use { it.readText() }
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            error("Command '$command' failed with exit code $exitCode and output: $output")
        }
        return output.trim()
    }
}
