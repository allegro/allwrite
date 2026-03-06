package pl.allegro.tech.allwrite.recipes.spring.util

import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Statement
import org.openrewrite.kotlin.tree.K

/**
 * Abstraction over Java and Kotlin constructors to access underlying [J.MethodDeclaration]
 */
internal sealed class Constructor(val method: J.MethodDeclaration) {

    abstract fun tree(): Statement

    internal class JavaConstructor(method: J.MethodDeclaration) : Constructor(method) {

        override fun tree(): Statement = method
    }

    internal class KotlinConstructor(val constructor: K.Constructor) : Constructor(constructor.methodDeclaration) {

        override fun tree(): Statement = constructor
    }
}

/**
 * Return a constructor, which is used by Spring for Autowiring:
 * - in case there is a single constructor, return it
 * - if there is at least one with @Autowired, return the fist such
 * - if there is a default, return it
 * - return null
 */
internal fun J.ClassDeclaration.getAutowiringConstructor(): Constructor? {
    val constructors: List<Constructor> = body.statements.mapNotNull { m -> m.asConstructor() }
    if (constructors.size == 1) return constructors[0]

    val autowiredConstructor = constructors.firstOrNull { it.method.hasAutowiredAnnotation() }
    val defaultConstructor = constructors.firstOrNull { it.isDefault() }

    return autowiredConstructor ?: defaultConstructor
}

private fun Constructor.isDefault() = method.parameters.isNullOrEmpty() || (method.parameters.size == 1 && method.parameters[0] is J.Empty)

private fun Statement.asConstructor(): Constructor? = when {
    this is J.MethodDeclaration && this.methodType?.name in setOf("<constructor>") -> Constructor.JavaConstructor(this)
    this is K.Constructor -> Constructor.KotlinConstructor(this)
    else -> null
}
