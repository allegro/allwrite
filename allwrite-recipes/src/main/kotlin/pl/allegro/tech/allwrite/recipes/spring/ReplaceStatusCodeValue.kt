package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.TreeVisitor
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTemplate
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.MethodMatcher
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.TypeUtils
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.RecipeVisibility
import pl.allegro.tech.allwrite.recipes.kotlin.KotlinPropertyMatcher
import pl.allegro.tech.allwrite.recipes.util.DelegatingJVisitor

public class ReplaceStatusCodeValue :
    AllwriteRecipe(
        displayName = "Replace ResponseEntity.getStatusCodeValue() with getStatusCode().value()",
        description = "Replaces deprecated `ResponseEntity.getStatusCodeValue()` / `.statusCodeValue` " +
            "with `getStatusCode().value()` / `.statusCode.value()` as required by Spring Framework 7 / Spring Boot 4.",
        visibility = RecipeVisibility.INTERNAL,
    ),
    ClasspathAwareRecipe {

    override fun requireOnClasspath(): List<String> = listOf("spring-web-6", "spring-core-6", "spring-beans-6")

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        val javaVisitor = object : JavaVisitor<ExecutionContext>() {
            private val methodMatcher = MethodMatcher("org.springframework.http.ResponseEntity getStatusCodeValue()")
            private val propertyMatcher = KotlinPropertyMatcher("org.springframework.http.ResponseEntity getStatusCodeValue()")

            override fun visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): J {
                val m = super.visitMethodInvocation(method, ctx) as J.MethodInvocation
                if (!matchesGetStatusCodeValue(m)) return m
                val select = m.select ?: return m
                return JavaTemplate.builder("#{any()}.getStatusCode().value()")
                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "spring-web-6", "spring-core-6"))
                    .build()
                    .apply(updateCursor(m), m.coordinates.replace(), select)
            }

            private fun matchesGetStatusCodeValue(m: J.MethodInvocation): Boolean {
                if (methodMatcher.matches(m)) return true
                if (m.simpleName != "getStatusCodeValue" || m.arguments.isNotEmpty()) return false
                val selectType = m.select?.type ?: return false
                return TypeUtils.isAssignableTo("org.springframework.http.ResponseEntity", selectType)
            }

            override fun visitFieldAccess(fieldAccess: J.FieldAccess, ctx: ExecutionContext): J {
                val fa = super.visitFieldAccess(fieldAccess, ctx) as J.FieldAccess
                if (!matchesStatusCodeValueProperty(fa)) return fa

                return JavaTemplate.builder("#{any()}.getStatusCode().value()")
                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "spring-web-6", "spring-core-6"))
                    .build()
                    .apply(updateCursor(fa), fa.coordinates.replace(), fa.target)
            }

            private fun matchesStatusCodeValueProperty(fa: J.FieldAccess): Boolean {
                if (fa.simpleName != "statusCodeValue") return false
                if (propertyMatcher.matches(fa)) return true
                return TypeUtils.isAssignableTo("org.springframework.http.ResponseEntity", fa.target.type)
            }
        }
        return DelegatingJVisitor(javaVisitor)
    }
}
