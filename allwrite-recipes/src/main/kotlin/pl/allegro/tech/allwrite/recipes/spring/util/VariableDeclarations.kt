package pl.allegro.tech.allwrite.recipes.spring.util

import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.TypeUtils

internal fun J.VariableDeclarations.hasAutowiredAnnotation() = leadingAnnotations
    .any { TypeUtils.isAssignableTo("org.springframework.beans.factory.annotation.Autowired", it.type) }

internal fun J.VariableDeclarations.hasResourceAnnotation() = leadingAnnotations
    .any { TypeUtils.isAssignableTo("jakarta.annotation.Resource", it.type) }

internal fun J.VariableDeclarations.hasInjectAnnotation() = leadingAnnotations
    .any { TypeUtils.isAssignableTo("jakarta.inject.Inject", it.type) }

internal fun J.VariableDeclarations.hasQualifierAnnotation() = leadingAnnotations
    .any { TypeUtils.isAssignableTo(ANNOTATION_QUALIFIER, it.type) }

internal fun J.VariableDeclarations.hasNamedAnnotation() = leadingAnnotations
    .any { TypeUtils.isAssignableTo(ANNOTATION_NAMED, it.type) }

/**
 * Returns `true` if variable is marked with one of @Autowired, @Resource or @Named
 */
internal fun J.VariableDeclarations.isAutowired() = hasAutowiredAnnotation() || hasResourceAnnotation() || hasInjectAnnotation()

/**
 * Returns `true` if variable is marked with one of @Resource, @Qualifier or @Named
 */
internal fun J.VariableDeclarations.hasQualifyingAnnotation() = hasResourceAnnotation() || hasQualifierAnnotation() || hasNamedAnnotation()

/**
 * @see findVariableQualifiedName
 */
internal fun J.VariableDeclarations.qualifiedName(): String? = leadingAnnotations.findVariableQualifiedName()

/**
 * Find variable matching a **simple** name
 */
internal fun J.VariableDeclarations.findVariableBy(simpleName: String): Variable? = variables.find { it.simpleName == simpleName }
    ?.let { Variable(it, this) }

/**
 * Container representing [J.VariableDeclarations.NamedVariable] along with its declaration info.
 * Gives accessor shortcuts to modifiers, annotations and naming data
 */
public data class Variable(
    val variable: J.VariableDeclarations.NamedVariable,
    val declaration: J.VariableDeclarations
) {

    val modifiers: List<J.Modifier> = declaration.modifiers
    val leadingAnnotations: List<J.Annotation> = declaration.leadingAnnotations
    val variableName: String = variable.simpleName
    val qualifiedName: String? = leadingAnnotations.findVariableQualifiedName()
    val name: String = qualifiedName ?: variableName
}

/**
 * Unpack variable declaration into a list of [Variable]s
 */
internal fun J.VariableDeclarations.variables() = variables.map { Variable(it, this) }
