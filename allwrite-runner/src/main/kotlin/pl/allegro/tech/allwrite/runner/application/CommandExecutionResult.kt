package pl.allegro.tech.allwrite.runner.application

import kotlin.time.Duration

internal sealed interface CommandExecutionResult {
    val executedRecipes: List<String>

    data class ExecutionResult(override val executedRecipes: List<String>) : CommandExecutionResult

    data class TimeMeasuredExecutionResult(
        override val executedRecipes: List<String>,
        val executionTime: Duration,
        val throwable: Throwable? = null
    ) : CommandExecutionResult
}
