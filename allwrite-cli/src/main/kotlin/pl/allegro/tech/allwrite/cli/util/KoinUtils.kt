package pl.allegro.tech.allwrite.cli.util

import org.koin.core.component.KoinComponent

internal inline fun <reified T : Any> KoinComponent.injectAll(): Lazy<List<T>> = lazy { getKoin().getAll<T>() }
