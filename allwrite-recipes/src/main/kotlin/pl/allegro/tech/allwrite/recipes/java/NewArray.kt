package pl.allegro.tech.allwrite.recipes.java

import org.openrewrite.Tree
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JContainer
import org.openrewrite.java.tree.JRightPadded
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space
import org.openrewrite.java.tree.TypeTree
import org.openrewrite.marker.Markers
import pl.allegro.tech.allwrite.recipes.util.JavaStringLiteral

internal fun NewArray(
    prefix: Space = Space.EMPTY,
    markers: Markers = Markers.EMPTY,
    type: JavaType?,
    typeExpression: TypeTree? = null,
    dimensions: List<J.ArrayDimension> = emptyList(),
    initializer: List<Expression>,
): J.NewArray = J.NewArray(Tree.randomId(), prefix, markers, typeExpression, dimensions, JContainer.build(initializer.map { JRightPadded.build(it) }), type)

internal fun stringArray(values: List<String>): J.NewArray =
    NewArray(
        type = JavaType.Array(null, JavaType.Primitive.String, null),
        initializer = values.map { JavaStringLiteral(it) }.spacesInBetween(),
    )
