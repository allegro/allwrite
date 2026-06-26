package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.TreeVisitor
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTemplate
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.MethodMatcher
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.TypeUtils
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.RecipeVisibility
import pl.allegro.tech.allwrite.recipes.kotlin.KotlinPropertyMatcher
import pl.allegro.tech.allwrite.recipes.util.DelegatingJVisitor
import pl.allegro.tech.allwrite.recipes.util.isKotlin

public class ReplaceStatusCodeValue :
    AllwriteRecipe(
        displayName = "Replace ResponseEntity.getStatusCodeValue() with getStatusCode().value()",
        description = "Replaces deprecated `ResponseEntity.getStatusCodeValue()` / `.statusCodeValue` " +
            "with `getStatusCode().value()` / `.statusCode.value()` as required by Spring Framework 7 / Spring Boot 4.",
        visibility = RecipeVisibility.INTERNAL,
    ),
    ClasspathAwareRecipe {

    override fun requireOnClasspath(): List<String> = listOf("spring-web-6", "spring-core-6")

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        val javaVisitor = object : JavaVisitor<ExecutionContext>() {
            private val methodMatcher = MethodMatcher("org.springframework.http.ResponseEntity getStatusCodeValue()")
            private val propertyMatcher = KotlinPropertyMatcher("org.springframework.http.ResponseEntity getStatusCodeValue()")

            override fun visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): J {
                val m = super.visitMethodInvocation(method, ctx) as J.MethodInvocation
                if (!matchesGetStatusCodeValue(m)) return m
                val select = m.select
                return if (select != null) {
                    JavaTemplate.builder("#{any()}.getStatusCode().value()")
                        .javaParser(springWebParser(ctx))
                        .build()
                        .apply(updateCursor(m), m.coordinates.replace(), select)
                } else if (!cursor.isKotlin()) {
                    JavaTemplate.builder("getStatusCode().value()")
                        .contextSensitive()
                        .javaParser(springWebParser(ctx))
                        .build()
                        .apply(updateCursor(m), m.coordinates.replace())
                } else {
                    m
                }
            }

            private fun matchesGetStatusCodeValue(m: J.MethodInvocation): Boolean {
                if (methodMatcher.matches(m)) return true
                if (m.simpleName != "getStatusCodeValue" || hasRealArguments(m)) return false
                val selectType = m.select?.type ?: return true
                return isResponseEntityOrUnresolved(selectType)
            }

            private fun hasRealArguments(m: J.MethodInvocation): Boolean = m.arguments.any { it !is J.Empty }

            private fun isResponseEntityOrUnresolved(type: JavaType): Boolean {
                if (TypeUtils.isAssignableTo("org.springframework.http.ResponseEntity", type)) return true
                return TypeUtils.isOfClassType(type, "java.lang.Object")
            }

            private fun springWebParser(ctx: ExecutionContext) =
                JavaParser.fromJavaVersion().classpathFromResources(ctx, "spring-web-6", "spring-core-6")

            override fun visitFieldAccess(fieldAccess: J.FieldAccess, ctx: ExecutionContext): J {
                val fa = super.visitFieldAccess(fieldAccess, ctx) as J.FieldAccess
                if (!matchesStatusCodeValueProperty(fa)) return fa

                val statusCodeAccess = fa.withName(
                    fa.name
                        .withSimpleName("statusCode")
                        .withFieldType(fa.name.fieldType?.withName("statusCode")),
                )
                return JavaTemplate.builder("#{any(org.springframework.http.HttpStatusCode)}.value()")
                    .javaParser(springWebParser(ctx))
                    .build()
                    .apply(updateCursor(fa), fa.coordinates.replace(), statusCodeAccess)
            }

            private fun matchesStatusCodeValueProperty(fa: J.FieldAccess): Boolean {
                if (fa.simpleName != "statusCodeValue") return false
                if (propertyMatcher.matches(fa)) return true
                val targetType = fa.target.type ?: return true
                return isResponseEntityOrUnresolved(targetType)
            }

            override fun visitIdentifier(identifier: J.Identifier, ctx: ExecutionContext): J {
                val id = super.visitIdentifier(identifier, ctx) as J.Identifier
                if (id.simpleName != "statusCodeValue") return id
                if (cursor.firstEnclosing(J.FieldAccess::class.java) != null) return id
                if (cursor.isKotlin()) return id
                if (!matchesStatusCodeValueIdentifier(id)) return id
                return JavaTemplate.builder("getStatusCode().value()")
                    .contextSensitive()
                    .javaParser(springWebParser(ctx))
                    .build()
                    .apply(updateCursor(id), id.coordinates.replace())
            }

            private fun matchesStatusCodeValueIdentifier(id: J.Identifier): Boolean {
                val fieldType = id.fieldType
                if (fieldType != null) {
                    val ownerType = fieldType.owner as? JavaType.FullyQualified ?: return false
                    return TypeUtils.isAssignableTo("org.springframework.http.ResponseEntity", ownerType)
                }
                val enclosingClass = cursor.firstEnclosing(J.ClassDeclaration::class.java) ?: return false
                val classType = enclosingClass.type ?: return false
                return TypeUtils.isAssignableTo("org.springframework.http.ResponseEntity", classType)
            }
        }
        return DelegatingJVisitor(javaVisitor)
    }
}
