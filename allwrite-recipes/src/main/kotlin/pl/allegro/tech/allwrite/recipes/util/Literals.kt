package pl.allegro.tech.allwrite.recipes.util

import org.openrewrite.Tree
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JContainer
import org.openrewrite.java.tree.JRightPadded
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space
import org.openrewrite.kotlin.tree.K
import org.openrewrite.marker.Markers
import pl.allegro.tech.allwrite.recipes.java.spacesInBetween

internal fun JavaLiteral(prefix: Space = Space.EMPTY, markers: Markers = Markers.EMPTY, type: JavaType.Primitive, value: Any, valueSource: String) =
    J.Literal(Tree.randomId(), prefix, markers, value, valueSource, emptyList(), type)

internal fun JavaStringLiteral(value: String) = JavaLiteral(type = JavaType.Primitive.String, value = value, valueSource = "\"$value\"")

internal fun KotlinListLiteral(prefix: Space = Space.EMPTY, markers: Markers = Markers.EMPTY, elements: List<Expression>, type: JavaType?): K.ListLiteral =
    K.ListLiteral(Tree.randomId(), prefix, markers, JContainer.build(elements.map { JRightPadded.build(it) }), type)

internal fun KotlinStringListLiteral(prefix: Space = Space.EMPTY, markers: Markers = Markers.EMPTY, values: List<String>) =
    KotlinListLiteral(prefix, markers, values.map { JavaStringLiteral(it) }.spacesInBetween(), JavaType.Array(null, JavaType.Primitive.String, null))

internal fun GroovyListLiteral(prefix: Space = Space.EMPTY, markers: Markers = Markers.EMPTY, elements: List<Expression>, type: JavaType?): G.ListLiteral =
    G.ListLiteral(Tree.randomId(), prefix, markers, JContainer.build(elements.map { JRightPadded.build(it) }), type)

internal fun GroovyStringListLiteral(prefix: Space = Space.EMPTY, markers: Markers = Markers.EMPTY, values: List<String>) =
    GroovyListLiteral(prefix, markers, values.map { JavaStringLiteral(it) }.spacesInBetween(), JavaType.Array(null, JavaType.Primitive.String, null))
