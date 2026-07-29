package pl.allegro.tech.allwrite.recipes.java

import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.TreeVisitor
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JContainer
import org.openrewrite.java.tree.JRightPadded
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space
import org.openrewrite.marker.Markers
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.INTERNAL
import java.util.UUID

public class ReplaceFactoryWithConstructor(
    @Option
    private val fullyQualifiedTypeName: String,
    @Option
    private val factoryClassNamePattern: String,
) : AllwriteRecipe(visibility = INTERNAL) {

    override fun getDisplayName(): String = "Replace factory with constructor"
    override fun getDescription(): String = "Replace factory with constructor."

    private val factoryClassNameRegex = factoryClassNamePattern.toRegex()

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        object : JavaVisitor<ExecutionContext>() {

            init {
                doAfterVisit(ReplaceFactoryMethodWithConstructor())
            }

            override fun visitNewClass(newClass: J.NewClass, p: ExecutionContext): J {
                cursor.root.putMessage("constructorIdentifierPrefix", newClass.clazz!!.prefix)
                return super.visitNewClass(newClass, p)
            }
        }

    private inner class ReplaceFactoryMethodWithConstructor : JavaVisitor<ExecutionContext>() {

        override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J {
            val m = super.visitMethodInvocation(method, p) as J.MethodInvocation

            if (factoryClassNameRegex.matches(method.methodType?.declaringType?.className ?: "") &&
                method.methodType?.declaringType?.owningClass?.fullyQualifiedName == fullyQualifiedTypeName
            ) {
                val select = m.select

                if (select is J.NewClass) {
                    when (val constructorSymbol = select.clazz) {
                        is J.Identifier -> {
                            val constructorType = select.constructorType
                            val owningClass = constructorType?.declaringType?.owningClass
                            if (owningClass != null) {
                                maybeRemoveImport(constructorType.declaringType)
                                maybeAddImport(owningClass)

                                return select
                                    .withClazz(
                                        constructorSymbol
                                            .withSimpleName(owningClass.className)
                                            .withType(owningClass),
                                    )
                                    .withConstructorType(
                                        constructorType
                                            .withDeclaringType(owningClass)
                                            .withReturnType(owningClass),
                                    )
                                    .withArguments(m.arguments)
                                    .withPrefix(m.prefix)
                            }
                        }

                        is J.FieldAccess ->
                            return select
                                .withClazz(constructorSymbol.target.withPrefix(constructorSymbol.prefix))
                                .withArguments(m.arguments)
                                .withPrefix(m.prefix)
                    }
                } else if (select is J.Identifier) {
                    return J.NewClass(
                        UUID.randomUUID(),
                        m.prefix,
                        m.markers,
                        null,
                        Space.EMPTY,
                        J.Identifier(
                            UUID.randomUUID(),
                            cursor.root.getMessage("constructorIdentifierPrefix") ?: Space.build(" ", emptyList()),
                            Markers.EMPTY,
                            emptyList(),
                            fullyQualifiedTypeName.substringAfterLast("."),
                            m.methodType?.declaringType?.owningClass,
                            null,
                        ),
                        JContainer.build(m.arguments.map { JRightPadded.build(it) }),
                        null,
                        JavaType.Method(
                            null,
                            1,
                            m.methodType?.declaringType?.owningClass,
                            "<constructor>",
                            m.methodType?.declaringType?.owningClass,
                            null as List<String>?,
                            null,
                            null,
                            null,
                            null,
                            null,
                        ),
                    )
                }
            }
            return m
        }
    }
}
