package pl.allegro.tech.allwrite.recipes.spring.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.openrewrite.java.tree.J

class MethodDeclarationTest : ParsingTest() {

    @BeforeEach
    fun setup() {
        javaParser.reset()
    }

    @Test
    fun `hasAutowiredAnnotation() should return true when method has such annotation`() {
        val j = parse(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            
            class Example {
              @Autowired
              public void test() {}
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.MethodDeclaration

        // then
        assertTrue(j.hasAutowiredAnnotation())
    }

    @Test
    fun `hasAutowiredAnnotation() should return false when method has no such annotation`() {
        val j = parse(
            """
            import jakarta.inject.Inject;
            
            class Example {
              @Inject
              public void test() {}
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.MethodDeclaration

        // then
        assertFalse(j.hasAutowiredAnnotation())
    }

    @Test
    fun `findArguments() should match by a qualified name when it is present`() {
        val j = parse(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.beans.factory.annotation.Qualifier;
            
            class Example {
              @Autowired
              public void test(@Qualifier("target") String str) {}
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.MethodDeclaration

        // then
        val found = j.findArguments("target")
        assertNotNull(found)
        assertThat(found).hasSize(1)
        assertEquals(j.parameters[0], found[0].declaration)

        val notFound = j.findArguments("str")
        assertNotNull(notFound)
        assertThat(notFound).isEmpty()
    }

    @Test
    fun `findArguments() should find all arguments with a given qualified name`() {
        val j = parse(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.beans.factory.annotation.Qualifier;
            import jakarta.inject.Named;
            
            class Example {
              @Autowired
              public void test(String str1, @Qualifier("target") String str2, @Named("target") String str2, String target) {}
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.MethodDeclaration

        // then
        val found = j.findArguments("target")
        assertNotNull(found)
        assertThat(found).hasSize(3)
        assertEquals(j.parameters[1], found[0].declaration)
        assertEquals(j.parameters[2], found[1].declaration)
        assertEquals(j.parameters[3], found[2].declaration)
    }

    @Test
    fun `findArguments() should match by a simple name when variable has no qualified name`() {
        val j = parse(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.beans.factory.annotation.Qualifier;
            
            class Example {
              @Autowired
              public void test(String target) {}
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.MethodDeclaration

        // then
        val found = j.findArguments("target")
        assertNotNull(found)
        assertThat(found).hasSize(1)
        assertEquals(j.parameters[0], found[0].declaration)
    }
}
