package pl.allegro.tech.allwrite.recipes.java

import org.openrewrite.Tree
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JContainer
import org.openrewrite.java.tree.JRightPadded
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space
import org.openrewrite.marker.Markers
import pl.allegro.tech.allwrite.recipes.util.JavaStringLiteral

internal fun annotation(prefix: Space = Space.EMPTY, type: JavaType.FullyQualified, params: Map<String, Expression>): J.Annotation = J.Annotation(
    Tree.randomId(),
    prefix,
    Markers.EMPTY,
    J.Identifier(
        Tree.randomId(),
        Space.EMPTY,
        Markers.EMPTY,
        mutableListOf<J.Annotation>(),
        type.fullyQualifiedName.substringAfterLast("."),
        type,
        null
    ),
    JContainer.build(
        prepareParams(params)
    )
)

/**
 * Creates an annotation of type `type` with a single parameter, assigned to the default attribute
 */
internal fun annotation(type: JavaType.FullyQualified, valueParam: String): J.Annotation =
    annotation(type = type, params = mapOf(DEFAULT_ANNOTATION_ARGUMENT_NAME to JavaStringLiteral(valueParam)))

/**
 * Creates an annotation of type `type` without parameters
 */
internal fun annotation(type: JavaType.FullyQualified): J.Annotation = annotation(type = type, params = emptyMap())

private fun prepareParams(params: Map<String, Expression>): List<JRightPadded<Expression>> {
    if (params.isEmpty()) return emptyList()
    if (params.size == 1 && params.keys.first() == DEFAULT_ANNOTATION_ARGUMENT_NAME) {
        val valueParameter = params.getValue(DEFAULT_ANNOTATION_ARGUMENT_NAME)
        return mutableListOf( // arguments
            JRightPadded.build(valueParameter)
        )
    }
    return params.map { (name, j) ->
        JRightPadded.build(
            assignment(name, j)
        )
    }
}

