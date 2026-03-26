package pl.allegro.tech.allwrite.recipes.java

import org.openrewrite.ExecutionContext
import org.openrewrite.TreeVisitor
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import pl.allegro.tech.allwrite.spi.AllwriteRecipe
import pl.allegro.tech.allwrite.spi.RecipeVisibility.INTERNAL

public class ChangeRecordField(
    private val declaringTypeFqn: String,
    private val oldFieldName: String,
    private val newFieldName: String,
) : AllwriteRecipe(visibility = INTERNAL) {

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> = object : JavaIsoVisitor<ExecutionContext>() {

        override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
            return super.visitClassDeclaration(classDecl, p)
        }

        override fun visitFieldAccess(fa: J.FieldAccess, p: ExecutionContext): J.FieldAccess {
            val fieldAccess = super.visitFieldAccess(fa, p)
            val fieldOwner = fieldAccess.name.fieldType?.owner

            if (fieldOwner is JavaType.Class && fieldOwner.fullyQualifiedName == declaringTypeFqn && fieldAccess.name.simpleName == oldFieldName) {
                val newName = fieldAccess.name
                    .withSimpleName(newFieldName)
                    .withFieldType(fieldAccess.name.fieldType?.withName(newFieldName))

                return fieldAccess.withName(newName)
            }
            return fieldAccess
        }

        override fun visitMethodInvocation(mi: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
            val methodInvocation = super.visitMethodInvocation(mi, p)
            val declaringType = methodInvocation.methodType?.declaringType

            if (declaringType?.fullyQualifiedName == declaringTypeFqn && methodInvocation.simpleName == oldFieldName) {
                val methodType = methodInvocation.methodType?.withName(newFieldName)
                val methodName = methodInvocation.name.withSimpleName(newFieldName).withType(methodType)

                return methodInvocation.withName(methodName).withMethodType(methodType)
            }

            return methodInvocation
        }
    }
}
