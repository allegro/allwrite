package pl.allegro.tech.allwrite.recipes.spring.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.openrewrite.java.tree.J

class VariableDeclarationsTest : ParsingTest() {

    @BeforeEach
    fun setup() {
        javaParser.reset()
    }

    @Test
    fun `hasAnnotation() should return true when variable declaration is marked with this annotation`() {
        var j = parse(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            import jakarta.annotation.Resource;
            import jakarta.inject.Named;
            
            class Example1 {
              @Named @Qualifier @Resource int x;
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.VariableDeclarations

        assertTrue(j.hasNamedAnnotation())
        assertTrue(j.hasQualifierAnnotation())
        assertTrue(j.hasResourceAnnotation())

        assertFalse(j.hasInjectAnnotation())
        assertFalse(j.hasAutowiredAnnotation())

        assertTrue(j.isAutowired())
        assertTrue(j.hasQualifyingAnnotation())

        j = parse(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            import jakarta.inject.Inject;
            
            class Example2 {
              @Autowired @Inject int x;
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.VariableDeclarations

        assertTrue(j.hasInjectAnnotation())
        assertTrue(j.hasAutowiredAnnotation())

        assertFalse(j.hasNamedAnnotation())
        assertFalse(j.hasQualifierAnnotation())
        assertFalse(j.hasResourceAnnotation())

        assertTrue(j.isAutowired())
        assertFalse(j.hasQualifyingAnnotation())

        j = parse(
            """
            
            class Example3 {
              int x;
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.VariableDeclarations

        assertFalse(j.hasInjectAnnotation())
        assertFalse(j.hasAutowiredAnnotation())
        assertFalse(j.hasNamedAnnotation())
        assertFalse(j.hasQualifierAnnotation())
        assertFalse(j.hasResourceAnnotation())
        assertFalse(j.isAutowired())
        assertFalse(j.hasQualifyingAnnotation())
    }

    @Test
    fun `qualifiedName() should return a name inferred from qualifier annotations with expected priority`() {
        var j = parse(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            import jakarta.annotation.Resource;
            import jakarta.inject.Named;
            
            class Example1 {
              @Named("n") @Qualifier("q") @Resource(name = "r") int x;
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.VariableDeclarations

        assertThat(j.qualifiedName()).isEqualTo("r")

        j = parse(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            import jakarta.inject.Named;
            
            class Example2 {
              @Named("n") @Qualifier("q") int x;
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.VariableDeclarations

        assertThat(j.qualifiedName()).isEqualTo("q")

        j = parse(
            """
            import jakarta.inject.Named;
            
            class Example3 {
              @Named("n") int x;
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.VariableDeclarations

        assertThat(j.qualifiedName()).isEqualTo("n")

        j = parse("class Example4 { int x; }").classes[0].body.statements[0] as J.VariableDeclarations

        assertThat(j.qualifiedName()).isNull()
    }

    @Test
    fun `should find variable by a simple name`() {
        val j = parse(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            import jakarta.annotation.Resource;
            import jakarta.inject.Named;
            
            class Example1 {
              @Named("n") @Qualifier("q") @Resource(name = "r") int x, y;
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.VariableDeclarations

        val foundBySimpleName = j.findVariableBy("x")
        val foundByQualifiedName = j.findVariableBy("r")

        assertNotNull(foundBySimpleName)
        assertEquals(j.variables[0], foundBySimpleName.variable)

        assertNull(foundByQualifiedName)
    }

    @Test
    fun `should unpack declaration into separate variables`() {
        val j = parse(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            import jakarta.annotation.Resource;
            import jakarta.inject.Named;
            
            class Example1 {
              @Named("n") @Qualifier("q") @Resource(name = "r") int x, y;
            }
            """.trimIndent(),
        ).classes[0].body.statements[0] as J.VariableDeclarations

        val result = j.variables()

        assertThat(result).allMatch { it.declaration == j }
        assertThat(result.map { it.variable }).containsAll(j.variables)
    }
}
