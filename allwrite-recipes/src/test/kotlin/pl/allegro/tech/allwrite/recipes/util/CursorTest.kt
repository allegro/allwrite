package pl.allegro.tech.allwrite.recipes.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Statement
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.kotlin.tree.K
import kotlin.jvm.optionals.getOrNull

class CursorTest {

    @Test
    fun `isKotlin() should return true in JavaVisitor visiting a kotlin tree`() {
        val j = parseKotlin("class A { val x = 123 }")
        var result: Boolean? = null
        val visitor = object : JavaVisitor<ExecutionContext>() {
            override fun visitStatement(statement: Statement, p: ExecutionContext): J {
                result = cursor.isKotlin()
                return super.visitStatement(statement, p)
            }
        }

        // when
        visitor.visit(j, InMemoryExecutionContext())

        // then
        assertEquals(true, result)
    }

    @Test
    fun `isKotlin() should return false in JavaVisitor visiting a java tree`() {
        val j = parseJava("class A { int x = 123; }")
        var result: Boolean? = null
        val visitor = object : JavaVisitor<ExecutionContext>() {
            override fun visitStatement(statement: Statement, p: ExecutionContext): J {
                result = cursor.isKotlin()
                return super.visitStatement(statement, p)
            }
        }

        // when
        visitor.visit(j, InMemoryExecutionContext())

        // then
        assertEquals(false, result)
    }

    fun parseJava(java: String): J.CompilationUnit = javaParser.parse(java).findFirst().getOrNull() as J.CompilationUnit
    fun parseKotlin(java: String): K.CompilationUnit = kotlinParser.parse(java).findFirst().getOrNull() as K.CompilationUnit

    companion object {
        val kotlinParser: KotlinParser = KotlinParser.builder().build()
        val javaParser: JavaParser = JavaParser.fromJavaVersion().build()
    }
}
