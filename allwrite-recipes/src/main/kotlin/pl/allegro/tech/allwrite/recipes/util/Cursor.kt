package pl.allegro.tech.allwrite.recipes.util

import org.openrewrite.Cursor
import org.openrewrite.groovy.tree.G
import org.openrewrite.kotlin.tree.K

internal fun Cursor.isKotlin(): Boolean = firstEnclosing(K::class.java) != null
internal fun Cursor.isGroovy(): Boolean = firstEnclosing(G::class.java) != null
