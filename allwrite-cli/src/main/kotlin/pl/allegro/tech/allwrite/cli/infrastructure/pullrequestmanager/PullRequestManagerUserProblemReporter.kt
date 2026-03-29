package pl.allegro.tech.allwrite.cli.infrastructure.pullrequestmanager

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import pl.allegro.tech.allwrite.runtime.port.outgoing.Problem
import pl.allegro.tech.allwrite.runtime.port.outgoing.UserProblemReporter
import pl.allegro.tech.allwrite.cli.application.port.outgoing.ShutdownListener
import pl.allegro.tech.allwrite.cli.infrastructure.os.port.incoming.SystemEnvironment
import java.io.File
import java.io.PrintStream

@Single
internal class PullRequestManagerUserProblemReporter(
    private val systemEnvironment: SystemEnvironment,
) : UserProblemReporter, ShutdownListener {

    private val issues = mutableListOf<Problem>()

    override fun reportProblem(problem: Problem) {
        issues.add(problem)
    }

    override fun onAppShutdown() {
        if (issues.isNotEmpty()) {
            val commentOutput = systemEnvironment["PR_MANAGER_SUMMARY_COMMENT_FILE"]
                ?.let(::File)
                ?.printWriter()
                ?: fallbackOutput()

            commentOutput.use { output ->
                output.appendLine("Failed to automatically apply some parts of the migration:")

                for (issue in issues) {
                    output.appendLine("* ${issue.message}")
                }
            }
        }
    }

    private fun fallbackOutput(): PrintStream {
        logger.debug { "Using System.out as UserProblem output" }
        return System.out
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
