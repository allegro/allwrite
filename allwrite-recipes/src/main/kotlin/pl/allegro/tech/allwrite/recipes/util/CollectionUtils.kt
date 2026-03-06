package pl.allegro.tech.allwrite.recipes.util

internal fun <T> Iterable<T>.replace(predicate: (T) -> Boolean, replacement: (T) -> T): List<T> =
    map {
        if (predicate(it)) {
            replacement(it)
        } else {
            it
        }
    }

internal fun <T> Iterable<T>.replace(element: T & Any, replacement: T & Any): List<T> =
    map {
        if (element == it) {
            replacement
        } else {
            it
        }
    }

internal fun <T> List<T>.filterNotOrThis(predicate: (T) -> Boolean): List<T> =
    if (any { predicate.invoke(it) }) {
        filterNot(predicate)
    } else {
        this
    }

internal fun <T> List<T>.mapFirst(mapper: (T) -> T): List<T> {
    if (isEmpty()) return this

    val mut = this.toMutableList()
    mut[0] = mapper(this[0])
    return mut
}

internal fun <T> List<T>.mapLast(mapper: (T) -> T): List<T> {
    if (isEmpty()) return this

    val mut = this.toMutableList()
    mut[size - 1] = mapper(this[size - 1])
    return mut
}
