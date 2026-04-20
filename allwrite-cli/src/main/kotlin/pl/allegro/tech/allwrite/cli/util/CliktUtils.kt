package pl.allegro.tech.allwrite.cli.util

import com.github.ajalt.clikt.completion.CompletionCandidates

internal fun customCompletion(fn: String): CompletionCandidates.Custom =
    CompletionCandidates.Custom { shell ->
        when (shell) {
            CompletionCandidates.Custom.ShellType.BASH -> fn
            CompletionCandidates.Custom.ShellType.FISH -> "COMPREPLY=()"
        }
    }
