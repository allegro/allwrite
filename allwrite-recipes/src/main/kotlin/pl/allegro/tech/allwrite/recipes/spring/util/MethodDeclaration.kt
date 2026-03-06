package pl.allegro.tech.allwrite.recipes.spring.util

import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.TypeUtils

internal fun J.MethodDeclaration.hasAutowiredAnnotation() = leadingAnnotations
    .any { TypeUtils.isAssignableTo(ANNOTATION_AUTOWIRED, it.type) }

/**
 * Finds arguments matching a **qualified** name (respecting @Qualifier and similar annotations) when it is present
 * or a simple name otherwise
 */
internal fun J.MethodDeclaration.findArguments(qualifiedName: String): List<Variable> = parameters.filterIsInstance<J.VariableDeclarations>()
    .mapNotNull {
        when (it.qualifiedName()) {
            qualifiedName -> it.variables().firstOrNull()
            null -> it.findVariableBy(qualifiedName)
            else -> null
        }
    }
