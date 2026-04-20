package pl.allegro.tech.allwrite.recipes.spring.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.tree.J

class IdentifierTest : ParsingTest() {

    @Test
    fun `should convert class name to an implicit bean name`() {
        val j = parse(
            """
            class LongClassNameFactoryBeanFactory {}
            """.trimIndent(),
        ).classes[0]

        // then
        assertEquals("longClassNameFactoryBeanFactory", j.name.toSpringBeanName())
    }

    @Test
    fun `should convert kotlin class name to an implicit bean name`() {
        val k = parseKotlin(
            """
            class LongClassNameFactoryBeanFactory
            """.trimIndent(),
        ).classes[0]

        // then
        assertEquals("longClassNameFactoryBeanFactory", k.name.toSpringBeanName())
    }

    @Test
    fun `should convert method name to an implicit bean name`() {
        val j = parse(
            """
            class Class {
              public void longMethodName() {}
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.MethodDeclaration

        // then
        assertEquals("longMethodName", j.name.toSpringBeanName())
    }

    @Test
    fun `should convert kotlin method name to an implicit bean name`() {
        val k = parseKotlin(
            """
            class LongClassNameFactoryBeanFactory {
               fun longMethodName() {}
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.MethodDeclaration

        // then
        assertEquals("longMethodName", k.name.toSpringBeanName())
    }

    @Test
    fun `should convert method argument name to an implicit bean name`() {
        val j = parse(
            """
            class Class {
              public void test(int longArgumentName) {}
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.MethodDeclaration
        val variable = (j.parameters[0] as J.VariableDeclarations).variables[0]

        // then
        assertEquals("longArgumentName", variable.name.toSpringBeanName())
    }

    @Test
    fun `should convert kotlin method argument name to an implicit bean name`() {
        val k = parseKotlin(
            """
            class LongClassNameFactoryBeanFactory {
               fun test(longArgumentName: Int) {}
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.MethodDeclaration
        val variable = (k.parameters[0] as J.VariableDeclarations).variables[0]

        // then
        assertEquals("longArgumentName", variable.name.toSpringBeanName())
    }
}
