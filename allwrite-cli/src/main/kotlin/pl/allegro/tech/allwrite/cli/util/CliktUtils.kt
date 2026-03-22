package pl.allegro.tech.allwrite.cli.util

import com.github.ajalt.clikt.completion.CompletionCandidates
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.convert
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

internal fun <T> RawOption.convertLazy(converter: (String) -> T) = this.convert {
    lazy { converter(it) }
}

internal fun <T> PropertyDelegateProvider<CliktCommand, ReadOnlyProperty<CliktCommand, Lazy<T>>>.lazy(): PropertyDelegateProvider<CliktCommand, ReadOnlyProperty<CliktCommand, T>> {
    val source = this
    return PropertyDelegateProvider { thisRef, property ->
        val delegate = source.provideDelegate(thisRef, property)
        ReadOnlyProperty { ref, prop -> delegate.getValue(ref, prop).value }
    }
}

internal fun customCompletion(fn: String): CompletionCandidates.Custom = CompletionCandidates.Custom { shell ->
    when (shell) {
        CompletionCandidates.Custom.ShellType.BASH -> fn
        CompletionCandidates.Custom.ShellType.FISH -> "COMPREPLY=()"
    }
}
