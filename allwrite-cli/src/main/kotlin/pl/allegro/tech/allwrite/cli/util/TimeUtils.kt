package pl.allegro.tech.allwrite.cli.util

import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.cli.application.CommandExecutionResult.TimeMeasuredExecutionResult
import kotlin.time.measureTime

internal fun catchingMeasureTime(block: () -> ExecutionResult): TimeMeasuredExecutionResult {
    var throwable: Throwable? = null
    var executionResult = ExecutionResult(emptyList())

    val measuredTime = measureTime {
        try {
            executionResult = block()
        } catch (e: Throwable) {
            throwable = e
        }
    }

    return TimeMeasuredExecutionResult(
        executedRecipes = executionResult.executedRecipes,
        executionTime = measuredTime,
        throwable = throwable,
    )
}
