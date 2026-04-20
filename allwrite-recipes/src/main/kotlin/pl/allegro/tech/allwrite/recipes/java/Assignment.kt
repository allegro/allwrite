package pl.allegro.tech.allwrite.recipes.java

import org.openrewrite.Tree
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JLeftPadded
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space
import org.openrewrite.marker.Markers

internal fun assignment(name: String, expr: Expression): J.Assignment =
    J.Assignment(
        Tree.randomId(),
        Space.EMPTY,
        Markers.EMPTY,
        J.Identifier(
            Tree.randomId(),
            Space.EMPTY,
            Markers.EMPTY,
            mutableListOf<J.Annotation>(),
            name,
            JavaType.Primitive.String,
            null,
        ),
        JLeftPadded.build(expr).withBefore(Space.SINGLE_SPACE),
        expr.type,
    )

internal fun J.Assignment.name() = (variable as? J.Identifier)?.simpleName
internal fun J.Assignment.valueAsString(): String? = (assignment as? J.Literal)?.value.toString()
