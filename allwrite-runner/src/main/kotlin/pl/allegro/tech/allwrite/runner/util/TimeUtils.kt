package pl.allegro.tech.allwrite.runner.util

import pl.allegro.tech.allwrite.runner.application.CommandExecutionResult.ExecutionResult
import pl.allegro.tech.allwrite.runner.application.CommandExecutionResult.TimeMeasuredExecutionResult
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
        throwable = throwable
    )
}
